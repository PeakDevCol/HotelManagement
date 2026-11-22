package com.peakdevcol.project.hotelmanagement.domain.model

/** How the side navigation groups the sections. */
enum class SectionGroup {
    OPERATION,
    ADMINISTRATION,
}

/** Who reaches a section. The Firestore rules apply the same permissions (PRD, section 3). */
enum class SectionAccess {
    EVERY_EMPLOYEE,

    /** RNF-14: the sections with the data of the guests. */
    RECEPTION_AND_ADMINISTRATORS,
    ADMINISTRATORS,
    ;

    fun allows(type: UserType): Boolean = when (this) {
        EVERY_EMPLOYEE -> true
        RECEPTION_AND_ADMINISTRATORS -> type == UserType.RECEPTION || type == UserType.ADMIN
        ADMINISTRATORS -> type == UserType.ADMIN
    }
}

/** The sections of the app, in the order of the side navigation. */
enum class AppSection(val group: SectionGroup, val access: SectionAccess) {
    ORDERS(SectionGroup.OPERATION, SectionAccess.EVERY_EMPLOYEE),

    /** RF-HAB-01 to RF-HAB-04: the board of every employee, where an administrator also sets the rooms up (decision 1A). */
    ROOMS(SectionGroup.OPERATION, SectionAccess.EVERY_EMPLOYEE),

    /** RF-RES-01 to RF-RES-09, with decision 2A of M11: today, the upcoming reservations and the calendar. */
    RESERVATIONS(SectionGroup.OPERATION, SectionAccess.RECEPTION_AND_ADMINISTRATORS),

    /** RF-ASI-02 to RF-ASI-05: the shifts of the employee and, from the second PR of M8, marking them. */
    ATTENDANCE(SectionGroup.OPERATION, SectionAccess.EVERY_EMPLOYEE),
    BALANCE(SectionGroup.ADMINISTRATION, SectionAccess.ADMINISTRATORS),

    /** RF-ASI-01: the shifts of the whole team. */
    SCHEDULES(SectionGroup.ADMINISTRATION, SectionAccess.ADMINISTRATORS),

    /** RF-ASI-05 to RF-ASI-08: what waits for the administrator, the corrections and the report of hours (P37). */
    ATTENDANCE_REVIEW(SectionGroup.ADMINISTRATION, SectionAccess.ADMINISTRATORS),
    INVENTORY(SectionGroup.ADMINISTRATION, SectionAccess.ADMINISTRATORS),
    EMPLOYEES(SectionGroup.ADMINISTRATION, SectionAccess.ADMINISTRATORS),
    SETTINGS(SectionGroup.ADMINISTRATION, SectionAccess.ADMINISTRATORS),
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
    val allowed = AppSection.entries.filter { section -> section.access.allows(user.type) }
    val main = user.area.mainSections().filter { it in allowed }.ifEmpty { allowed.take(1) }
    return AppNavigation(sections = allowed, mainSections = main)
}

// Only the sections that exist today: tasks (phase 4) join the bar of their areas when they are
// built (PRD, section 13, 2026-11-20). With decision 2A of M10, reception, housekeeping and
// maintenance open on the rooms; service and administration have them in "Más", like
// administration has attendance. With decision 2A of M11, the reservations go next to the rooms
// in the bar of reception.
private fun UserArea.mainSections(): List<AppSection> = when (this) {
    UserArea.MANAGEMENT -> listOf(AppSection.ORDERS, AppSection.BALANCE)
    UserArea.SERVICE -> listOf(AppSection.ORDERS, AppSection.ATTENDANCE)
    UserArea.RECEPTION -> listOf(AppSection.ROOMS, AppSection.RESERVATIONS, AppSection.ORDERS, AppSection.ATTENDANCE)
    UserArea.HOUSEKEEPING, UserArea.MAINTENANCE -> listOf(AppSection.ROOMS, AppSection.ORDERS, AppSection.ATTENDANCE)
}
