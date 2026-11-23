package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.domain.model.GuestError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.code
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal const val GUEST_FULL_NAME_FIELD = "fullName"
internal const val GUEST_PHONE_FIELD = "phone"
internal const val GUEST_DOCUMENT_TYPE_FIELD = "documentType"
internal const val GUEST_DOCUMENT_NUMBER_FIELD = "documentNumber"
internal const val GUEST_NATIONALITY_FIELD = "nationality"
internal const val GUEST_EMAIL_FIELD = "email"
internal const val GUEST_SEARCH_KEYS_FIELD = "searchKeys"
internal const val GUEST_UPDATED_BY_FIELD = "updatedBy"
internal const val GUEST_UPDATED_AT_FIELD = "updatedAt"
internal const val RESERVATION_STATE_FIELD = "state"
internal const val RESERVATION_CHECK_IN_FIELD = "checkInDate"
internal const val RESERVATION_CHECK_OUT_FIELD = "checkOutDate"
internal const val ROOM_NIGHT_DATE_FIELD = "date"

/** How long a reservation waits for the server: it needs connection (RNF-04), so it never stays waiting. */
internal val RESERVATION_SERVER_WAIT: Duration = 15.seconds

internal fun Exception.toReservationError(): ReservationError = when ((this as? FirebaseFirestoreException)?.code) {
    FirestoreExceptionCode.UNAVAILABLE, FirestoreExceptionCode.DEADLINE_EXCEEDED -> ReservationError.NoConnection
    FirestoreExceptionCode.PERMISSION_DENIED -> ReservationError.NotAllowed
    else -> ReservationError.Unknown
}

internal fun Exception.toGuestError(): GuestError = when ((this as? FirebaseFirestoreException)?.code) {
    FirestoreExceptionCode.UNAVAILABLE, FirestoreExceptionCode.DEADLINE_EXCEEDED -> GuestError.NoConnection
    FirestoreExceptionCode.PERMISSION_DENIED -> GuestError.NotAllowed
    else -> GuestError.Unknown
}
