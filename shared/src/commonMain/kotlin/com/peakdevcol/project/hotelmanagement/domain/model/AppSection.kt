package com.peakdevcol.project.hotelmanagement.domain.model

/** How the side navigation groups the sections. */
enum class SectionGroup {
    OPERATION,
    ADMINISTRATION,
}

/** The sections of the app, in the order of the side navigation. */
enum class AppSection(val group: SectionGroup, val onlyAdministrators: Boolean) {
    ORDERS(SectionGroup.OPERATION, onlyAdministrators = false),

    /** RF-HAB-01 to RF-HAB-04: the board of every employee, where an administrator also sets the rooms up (decision 1A). */
    ROOMS(SectionGroup.OPERATION, onlyAdministrators = false),

    /** RF-ASI-02 to RF-ASI-05: the shifts of the employee and, from the second PR of M8, marking them. */
    ATTENDANCE(SectionGroup.OPERATION, onlyAdministrators = false),
    BALANCE(SectionGroup.ADMINISTRATION, onlyAdministrators = true),

    /** RF-ASI-01: the shifts of the whole team. */
    SCHEDULES(SectionGroup.ADMINISTRATION, onlyAdministrators = true),

    /** RF-ASI-05 to RF-ASI-08: what waits for the administrator, the corrections and the report of hours (P37). */
    ATTENDANCE_REVIEW(SectionGroup.ADMINISTRATION, onlyAdministrators = true),
    INVENTORY(SectionGroup.ADMINISTRATION, onlyAdministrators = true),
    EMPLOYEES(SectionGroup.ADMINISTRATION, onlyAdministrators = true),
    SETTINGS(SectionGroup.ADMINISTRATION, onlyAdministrators = true),
}

/**
 * RF-AUTH-05 and RF-EMP-05: what an employee reaches and where the app opens, without an
 * intermediate menu. [sections] has every section the role allows. [mainSections] are the ones of
 * the area that go in the bottom bar of the phone; the first one is where the app opens, and the
 * rest of [sections] goes in "Más".
 */
data class AppNavigation(
    val sections: List<AppSection>,
    val mainSections: List<AppSection>,
) {
    val startSection: AppSection get() = mainSections.first()
    val moreSections: List<AppSection> get() = sections - mainSections.toSet()
}

/** The Firestore rules apply the same permissions (PRD, section 3). */
fun navigationFor(user: User): AppNavigation {
    val allowed = AppSection.entries.filter { section -> !section.onlyAdministrators || user.type == UserType.ADMIN }
    val main = user.area.mainSections().filter { it in allowed }.ifEmpty { allowed.take(1) }
    return AppNavigation(sections = allowed, mainSections = main)
}

// Only the sections that exist today: tasks (phase 4) join the bar of their areas when they are
// built (PRD, section 13, 2026-11-20). With decision 2A of M10, reception, housekeeping and
// maintenance open on the rooms; service and administration have them in "Más", like
// administration has attendance.
private fun UserArea.mainSections(): List<AppSection> = when (this) {
    UserArea.MANAGEMENT -> listOf(AppSection.ORDERS, AppSection.BALANCE)
    UserArea.SERVICE -> listOf(AppSection.ORDERS, AppSection.ATTENDANCE)
    UserArea.RECEPTION, UserArea.HOUSEKEEPING, UserArea.MAINTENANCE ->
        listOf(AppSection.ROOMS, AppSection.ORDERS, AppSection.ATTENDANCE)
}
