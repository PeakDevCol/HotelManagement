package com.peakdevcol.project.hotelmanagement.domain.model

import com.peakdevcol.project.hotelmanagement.domain.validation.UserInputValidator
import kotlin.time.Instant

/** RF-RES-01 with decision 5A: the documents a guest shows at the front desk. */
enum class DocumentType {
    /** Cédula de ciudadanía. */
    CC,

    /** Cédula de extranjería. */
    CE,
    PASSPORT,

    /** Tarjeta de identidad, of a minor. */
    TI,

    /** Permiso por protección temporal. */
    PPT,
    OTHER,
}

/**
 * RF-RES-01: a guest of the hotel. Their data is personal (RNF-14): only reception and the
 * administrators read it. With decision 5A the name and the phone are enough to reserve; the
 * document comes later, and the check-in asks for it (M12).
 */
data class Guest(
    val id: String,
    val fullName: String,
    val phone: String,
    val documentType: DocumentType? = null,
    val documentNumber: String? = null,
    val nationality: String? = null,
    val email: String? = null,
    /** When the guest authorized the treatment of their data. The check-in records it (M12). */
    val dataConsentAt: Instant? = null,
) {
    // S9: personal data never reaches a log through the text of the object.
    override fun toString(): String = "Guest(id=$id)"
}

/** What the reception types about a guest. Blank texts mean the data was not given. */
data class GuestFields(
    val fullName: String,
    val phone: String,
    val documentType: DocumentType? = null,
    val documentNumber: String = "",
    val nationality: String = DEFAULT_NATIONALITY,
    val email: String = "",
) {
    // S9: personal data never reaches a log through the text of the object.
    override fun toString(): String = "GuestFields"
}

const val DEFAULT_NATIONALITY = "Colombia"
const val MAX_GUEST_NAME_LENGTH = 120
const val MAX_GUEST_TEXT_LENGTH = 60
private const val MIN_PHONE_DIGITS = 7

// The longest beginning of a word that a search keeps: past it, a longer word still finds the guest.
private const val MAX_SEARCH_KEY_LENGTH = 20
private const val MIN_SEARCH_KEY_LENGTH = 2

/** Why a guest was not saved or found. */
sealed class GuestError(message: String) : Exception(message) {
    data object BlankName : GuestError("The name is required")
    data object InvalidPhone : GuestError("The phone needs at least $MIN_PHONE_DIGITS digits")

    /** The type and the number of the document go together, or neither goes. */
    data object IncompleteDocument : GuestError("The document needs its type and its number")
    data object DuplicateDocument : GuestError("Another guest has that document")
    data object InvalidEmail : GuestError("The email is not valid")
    data object NoSession : GuestError("There is no session")
    data object NotAllowed : GuestError("Only reception and the administrators see the guests")
    data object NoConnection : GuestError("There is no connection")
    data object Unknown : GuestError("Unknown error")
}

/** A document number as it is stored and compared: letters and digits only, in capitals. "1.020.304" is "1020304". */
fun normalizeDocumentNumber(text: String): String = text.filter(Char::isLetterOrDigit).uppercase()

/** Checks what was typed and returns the guest to store, with [id] and what [existing] already had. */
fun GuestFields.toGuest(id: String, existing: Guest?): Result<Guest> {
    val name = fullName.trim().replace(Regex("\\s+"), " ")
    if (name.isEmpty()) return Result.failure(GuestError.BlankName)
    val cleanPhone = phone.trim()
    if (cleanPhone.count(Char::isDigit) < MIN_PHONE_DIGITS) return Result.failure(GuestError.InvalidPhone)
    val number = normalizeDocumentNumber(documentNumber).takeIf { it.isNotEmpty() }
    if ((documentType == null) != (number == null)) return Result.failure(GuestError.IncompleteDocument)
    val cleanEmail = email.trim().takeIf { it.isNotEmpty() }
    if (cleanEmail != null && !UserInputValidator.isValidEmail(cleanEmail)) return Result.failure(GuestError.InvalidEmail)
    return Result.success(
        Guest(
            id = id,
            fullName = name.take(MAX_GUEST_NAME_LENGTH),
            phone = cleanPhone.take(MAX_GUEST_TEXT_LENGTH),
            documentType = documentType,
            documentNumber = number?.take(MAX_GUEST_TEXT_LENGTH),
            nationality = nationality.trim().takeIf { it.isNotEmpty() }?.take(MAX_GUEST_TEXT_LENGTH),
            email = cleanEmail?.take(MAX_GUEST_NAME_LENGTH),
            dataConsentAt = existing?.dataConsentAt,
        ),
    )
}

private fun wordsOf(text: String): List<String> =
    text.searchable().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }

/**
 * RF-RES-01: what a search by name or document finds the guest by. Firestore has no text search,
 * so each word of the name is stored with each of its beginnings, from two letters, and the
 * document whole: "Ana Rojas" is found by "an", "ana", "ro", "roj"…
 */
val Guest.searchKeys: List<String>
    get() {
        val nameKeys = wordsOf(fullName).flatMap { word ->
            val longest = word.take(MAX_SEARCH_KEY_LENGTH)
            (MIN_SEARCH_KEY_LENGTH..longest.length).map { length -> longest.take(length) }
        }
        val documentKey = documentNumber?.searchable()
        return (nameKeys + listOfNotNull(documentKey)).distinct()
    }

/**
 * The one key a search asks Firestore for. Names have no digits, so a query with digits is a
 * document, compared whole even if it was typed with dots; otherwise, the longest word typed,
 * which narrows the most. Null when nothing typed is long enough to search.
 */
fun guestSearchKeyFor(query: String): String? {
    val key = if (query.any(Char::isDigit)) {
        normalizeDocumentNumber(query).searchable()
    } else {
        wordsOf(query).maxByOrNull { it.length }?.take(MAX_SEARCH_KEY_LENGTH)
    }
    return key?.takeIf { it.length >= MIN_SEARCH_KEY_LENGTH }
}

/** Whether every word typed starts a word of the name, or the whole query is the document. */
fun Guest.matches(query: String): Boolean {
    val typed = wordsOf(query)
    if (typed.isEmpty()) return false
    val document = documentNumber?.searchable()
    if (document != null && normalizeDocumentNumber(query).searchable() == document) return true
    val nameWords = wordsOf(fullName)
    return typed.all { word -> nameWords.any { it.startsWith(word) } }
}
