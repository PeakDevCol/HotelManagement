package com.peakdevcol.project.hotelmanagement.domain.model

/** Whether the server already has an order that was saved (RF-ORD-09). */
enum class OrderDelivery {
    SENT,

    /** Saved on the device. It is sent by itself when the connection comes back. */
    PENDING,
}
