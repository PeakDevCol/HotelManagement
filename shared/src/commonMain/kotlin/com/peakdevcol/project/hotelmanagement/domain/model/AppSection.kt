package com.peakdevcol.project.hotelmanagement.domain.model

/** The sections of the main menu. */
enum class AppSection {
    ADD_ORDER,
    CURRENT_ORDERS,
    BALANCE,
    INVENTORY,
    EMPLOYEES,
    SETTINGS,
}

private val ADMINISTRATOR_SECTIONS =
    setOf(AppSection.BALANCE, AppSection.INVENTORY, AppSection.EMPLOYEES, AppSection.SETTINGS)

/**
 * RF-AUTH-05: the role decides which sections an employee sees (PRD, section 3), and the area
 * which ones come first (RF-EMP-05). The Firestore rules apply the same permissions.
 */
fun menuFor(user: User): List<AppSection> {
    val ordered = when (user.area) {
        UserArea.MANAGEMENT -> listOf(
            AppSection.CURRENT_ORDERS,
            AppSection.BALANCE,
            AppSection.INVENTORY,
            AppSection.EMPLOYEES,
            AppSection.SETTINGS,
            AppSection.ADD_ORDER,
        )
        UserArea.SERVICE, UserArea.RECEPTION, UserArea.HOUSEKEEPING, UserArea.MAINTENANCE -> listOf(
            AppSection.ADD_ORDER,
            AppSection.CURRENT_ORDERS,
            AppSection.BALANCE,
            AppSection.INVENTORY,
            AppSection.EMPLOYEES,
            AppSection.SETTINGS,
        )
    }
    return ordered.filter { it !in ADMINISTRATOR_SECTIONS || user.type == UserType.ADMIN }
}
