package com.peakdevcol.project.hotelmanagement.data.firebase

/** Collection names. They must match section 8 of the PRD, which the Firestore rules use. */
internal object FirestorePaths {
    const val USERS = "Users"
    const val ORDERS = "Orders"
    const val ORDER_EVENTS = "events"
    const val PAYMENTS = "Payments"
    const val CASH_CLOSINGS = "CashClosings"
    const val PRODUCTS = "Products"
    const val CATEGORIES = "Categories"
    const val TAXES = "Taxes"
    const val SETTINGS = "Settings"
    const val SHIFTS = "Shifts"
    const val ATTENDANCE = "Attendance"
    const val DEVICES = "Devices"
}
