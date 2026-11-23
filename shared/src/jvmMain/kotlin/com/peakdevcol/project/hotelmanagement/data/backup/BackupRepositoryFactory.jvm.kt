package com.peakdevcol.project.hotelmanagement.data.backup

import com.peakdevcol.project.hotelmanagement.data.firebase.CLOCK_IN_AT_FIELD
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestorePaths
import com.peakdevcol.project.hotelmanagement.data.firebase.RESERVATION_CHECK_OUT_FIELD
import com.peakdevcol.project.hotelmanagement.data.firebase.RESERVATION_EVENTS
import com.peakdevcol.project.hotelmanagement.data.firebase.ROOM_NIGHT_DATE_FIELD
import com.peakdevcol.project.hotelmanagement.domain.model.BackupError
import com.peakdevcol.project.hotelmanagement.domain.model.BackupSummary
import com.peakdevcol.project.hotelmanagement.domain.model.operationalDateOf
import com.peakdevcol.project.hotelmanagement.domain.repository.BackupRepository
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.android
import dev.gitlive.firebase.firestore.code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

internal actual fun backupRepository(firestore: Lazy<FirebaseFirestore>): BackupRepository =
    DesktopBackupRepository(firestore, folder = File(System.getProperty("user.home"), "Documents/HotelManagement/Respaldos"))

/**
 * RF-EMP-07: reads every collection from the server and writes it to a JSON file in the
 * documents of the user. Each document is written as Firestore stores it, with every field,
 * so the backup does not depend on what this version of the app knows how to read.
 */
internal class DesktopBackupRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val folder: File,
) : BackupRepository {

    override val isAvailable = true

    override suspend fun export(since: Instant?): Result<BackupSummary> = try {
        val collections = readCollections(since)
        val backup = mapOf(
            "exportedAt" to java.time.Instant.now().toString(),
            "since" to since?.toString(),
            "collections" to collections,
        )
        val file = withContext(Dispatchers.IO) { write(JsonWriter.write(backup)) }
        Result.success(BackupSummary(filePath = file.path, documentCount = collections.values.sumOf { it.size }))
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.failure(BackupError.CannotWriteFile)
    } catch (e: Exception) {
        Result.failure(e.toBackupError())
    }

    private suspend fun readCollections(since: Instant?): Map<String, Map<String, Any?>> =
        WHOLE_COLLECTIONS.associateWith { name -> documentsOf(name, dateField = null, since = null) } +
            DATED_COLLECTIONS.mapValues { (name, dateField) -> documentsOf(name, dateField, since) } +
            DAY_COLLECTIONS.mapValues { (name, dayField) -> documentsFromDay(name, dayField, since) } +
            mapOf(FirestorePaths.ORDERS to ordersWithHistory(since), FirestorePaths.RESERVATIONS to reservationsWithHistory(since))

    // Source.SERVER: a backup made from the cache of the device could silently miss documents.
    private suspend fun documentsOf(collection: String, dateField: String?, since: Instant?): Map<String, Any?> =
        snapshotsOf(collection, dateField, since).associate { it.id to it.plainData() }

    private suspend fun documentsFromDay(collection: String, dayField: String, since: Instant?): Map<String, Any?> =
        snapshotsFromDay(collection, dayField, since).associate { it.id to it.plainData() }

    // Days are written as `2026-11-22`, so the ones from the day of [since] on compare as text.
    private suspend fun snapshotsFromDay(collection: String, dayField: String, since: Instant?): List<DocumentSnapshot> {
        val all = firestore.value.collection(collection)
        val query = if (since != null) all.where { dayField greaterThanOrEqualTo operationalDateOf(since, dayStartMinutes = 0).isoText } else all
        return query.get(Source.SERVER).documents
    }

    // Decision 1A of M11: each reservation brings its history, like each order. A reservation goes
    // if it ends on the day of [since] or later.
    private suspend fun reservationsWithHistory(since: Instant?): Map<String, Any?> =
        withSubcollection(snapshotsFromDay(FirestorePaths.RESERVATIONS, RESERVATION_CHECK_OUT_FIELD, since), RESERVATION_EVENTS)

    private suspend fun snapshotsOf(collection: String, dateField: String?, since: Instant?): List<DocumentSnapshot> {
        val all = firestore.value.collection(collection)
        val query = if (dateField != null && since != null) {
            all.where { dateField greaterThanOrEqualTo Timestamp(since.epochSeconds, since.nanosecondsOfSecond) }
        } else {
            all
        }
        return query.get(Source.SERVER).documents
    }

    // The history of an order lives in its own subcollection, so each order brings its events (RF-CUR-11).
    private suspend fun ordersWithHistory(since: Instant?): Map<String, Any?> =
        withSubcollection(snapshotsOf(FirestorePaths.ORDERS, dateField = "dateOrder", since = since), FirestorePaths.ORDER_EVENTS)

    private suspend fun withSubcollection(documents: List<DocumentSnapshot>, subcollection: String): Map<String, Any?> = coroutineScope {
        documents
            .chunked(PARALLEL_READS)
            .flatMap { chunk ->
                chunk.map { document ->
                    async {
                        val children = document.reference.collection(subcollection).get(Source.SERVER).documents
                        document.id to document.plainData() + (subcollection to children.associate { it.id to it.plainData() })
                    }
                }.awaitAll()
            }
            .toMap()
    }

    private fun DocumentSnapshot.plainData(): Map<String, Any?> =
        android.data.orEmpty().mapValues { (_, value) -> value.toPlainValue() }

    private fun Any?.toPlainValue(): Any? = when (this) {
        is com.google.firebase.Timestamp -> java.time.Instant.ofEpochSecond(seconds, nanoseconds.toLong()).toString()
        is Map<*, *> -> entries.associate { (key, value) -> key.toString() to value.toPlainValue() }
        is Iterable<*> -> map { it.toPlainValue() }
        null, is Boolean, is Number, is String -> this
        else -> toString()
    }

    private fun write(json: String): File {
        if (!folder.isDirectory && !folder.mkdirs()) throw IOException("Cannot create $folder")
        val file = File(folder, "respaldo-${LocalDateTime.now().format(FILE_TIME)}.json")
        file.writeText(json)
        return file
    }

    private fun Exception.toBackupError(): BackupError = when ((this as? FirebaseFirestoreException)?.code) {
        FirestoreExceptionCode.PERMISSION_DENIED -> BackupError.NotAllowed
        FirestoreExceptionCode.UNAVAILABLE, FirestoreExceptionCode.DEADLINE_EXCEEDED -> BackupError.Network
        else -> BackupError.Unknown
    }

    private companion object {
        const val PARALLEL_READS = 10
        val FILE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")
    }
}

/** The collections the backup always exports whole. Besides them go [DATED_COLLECTIONS] and the orders. */
internal val WHOLE_COLLECTIONS = listOf(
    FirestorePaths.USERS,
    FirestorePaths.PRODUCTS,
    FirestorePaths.CATEGORIES,
    FirestorePaths.TAXES,
    FirestorePaths.SETTINGS,
    FirestorePaths.DEVICES,
    FirestorePaths.ROOM_TYPES,
    FirestorePaths.ROOMS,
    // Decision 6A of M11: personal data, which is why Settings warns to keep the file safe (Ley 1581).
    FirestorePaths.GUESTS,
)

/**
 * RF-EMP-07: the collections that grow every day, with the field that says when each document
 * started. A backup from a date leaves out what started before it, like the orders by `dateOrder`.
 */
internal val DATED_COLLECTIONS = mapOf(
    FirestorePaths.PAYMENTS to "receivedAt",
    FirestorePaths.CASH_CLOSINGS to "createdAt",
    FirestorePaths.SHIFTS to "startAt",
    FirestorePaths.ATTENDANCE to CLOCK_IN_AT_FIELD,
)

/**
 * Decision 6A of M11: the collections that grow every day and say when with a day of Colombia
 * written as text: a night goes if it is on that day or later. The reservations go apart, with their history.
 */
internal val DAY_COLLECTIONS = mapOf(
    FirestorePaths.ROOM_NIGHTS to ROOM_NIGHT_DATE_FIELD,
)
