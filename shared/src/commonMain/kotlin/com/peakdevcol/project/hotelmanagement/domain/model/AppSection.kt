package com.peakdevcol.project.hotelmanagement.domain.model

/** How the side navigation groups the sections. */
enum class SectionGroup {
    OPERATION,
    ADMINISTRATION,
}

/** The sections of the app, in the order of the side navigation. */
enum class AppSection(val group: SectionGroup, val onlyAdministrators: Boolean) {
    ORDERS(SectionGroup.OPERATION, onlyAdministrators = false),
    BALANCE(SectionGroup.ADMINISTRATION, onlyAdministrators = true),
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

// Only the sections that exist today. Attendance (M8), rooms (phase 3) and tasks (phase 4) join the
// bar of their areas when they are built (PRD, section 13, 2026-11-20).
private fun UserArea.mainSections(): List<AppSection> = when (this) {
    UserArea.MANAGEMENT -> listOf(AppSection.ORDERS, AppSection.BALANCE)
    UserArea.SERVICE, UserArea.RECEPTION, UserArea.HOUSEKEEPING, UserArea.MAINTENANCE -> listOf(AppSection.ORDERS)
}
