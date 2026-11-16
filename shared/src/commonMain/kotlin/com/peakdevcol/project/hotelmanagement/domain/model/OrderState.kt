package com.peakdevcol.project.hotelmanagement.domain.model

/** Life cycle of an order (PRD, section 6.3). */
enum class OrderState {
    IN_PROGRESS,
    CLOSED,
    PAID,
    CANCELLED,
}
