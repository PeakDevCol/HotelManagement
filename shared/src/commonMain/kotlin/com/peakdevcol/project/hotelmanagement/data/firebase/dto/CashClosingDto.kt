package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.CashClosing
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingType
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * A cash closing stored at `CashClosings/{number}` (RF-BAL-06). The id is its number, so two
 * administrators cannot save the same closing: the rules only let the next number be created.
 */
@Serializable
internal data class CashClosingDto(
    val from: Timestamp? = null,
    val to: Timestamp? = null,
    val type: String,
    val expectedCash: Long,
    val countedCash: Long,
    val difference: Long,
    val note: String,
    val closedBy: String,
    val createdAt: BaseTimestamp = Timestamp.ServerTimestamp,
)

internal fun CashClosing.toDto() = CashClosingDto(
    from = from?.toTimestamp(),
    to = to?.toTimestamp(),
    type = type.code(),
    expectedCash = expectedCash,
    countedCash = countedCash,
    difference = difference,
    note = note,
    closedBy = closedBy,
)

/** Returns null when the id is not a number or the type is not one the app knows. */
internal fun CashClosingDto.toCashClosing(id: String): CashClosing? {
    val number = id.toLongOrNull() ?: return null
    val closingType = CashClosingType.entries.firstOrNull { it.code() == type } ?: return null
    return CashClosing(
        number = number,
        from = from?.toInstant(),
        to = to?.toInstant(),
        type = closingType,
        expectedCash = expectedCash,
        countedCash = countedCash,
        difference = difference,
        note = note,
        closedBy = closedBy,
        createdAt = (createdAt as? Timestamp)?.toInstant(),
    )
}

/** The codes of section 8 of the PRD, which the Firestore rules also check. */
internal fun CashClosingType.code(): String = name.lowercase()

internal fun Instant.toTimestamp() = Timestamp(epochSeconds, nanosecondsOfSecond)

internal fun Timestamp.toInstant(): Instant = Instant.fromEpochSeconds(seconds, nanoseconds)
