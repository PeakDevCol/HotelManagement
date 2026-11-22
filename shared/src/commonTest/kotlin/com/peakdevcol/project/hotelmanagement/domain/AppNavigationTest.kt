package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.AppSection
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.model.navigationFor
import com.peakdevcol.project.hotelmanagement.fakes.employee
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** RF-AUTH-05 and RF-EMP-05, with the sections built so far (PRD, section 13, 2026-11-20, and decision 2A of M10). */
class AppNavigationTest {
    private val waiter = employee("waiter", "Ana Gómez")
    private val admin = employee("admin", "Laura Méndez", type = UserType.ADMIN).copy(area = UserArea.MANAGEMENT)
    private val everyEmployeeSection = listOf(AppSection.ORDERS, AppSection.ROOMS, AppSection.ATTENDANCE)

    @Test
    fun aWaiterOpensOnTheOrdersAndHasTheRoomsInMore() {
        val navigation = navigationFor(waiter)

        assertEquals(everyEmployeeSection, navigation.sections)
        assertEquals(listOf(AppSection.ORDERS, AppSection.ATTENDANCE), navigation.mainSections)
        assertEquals(AppSection.ORDERS, navigation.startSection)
        assertEquals(listOf(AppSection.ROOMS), navigation.moreSections)
    }

    @Test
    fun receptionHousekeepingAndMaintenanceOpenOnTheRooms() {
        listOf(UserArea.RECEPTION, UserArea.HOUSEKEEPING, UserArea.MAINTENANCE).forEach { area ->
            val navigation = navigationFor(waiter.copy(type = UserType.RECEPTION, area = area))

            assertEquals(listOf(AppSection.ROOMS, AppSection.ORDERS, AppSection.ATTENDANCE), navigation.mainSections, "$area")
            assertEquals(AppSection.ROOMS, navigation.startSection, "$area")
            assertTrue(navigation.moreSections.isEmpty(), "$area")
        }
    }

    @Test
    fun everyAreaWithoutAdministrationHasTheSameSections() {
        UserArea.entries.forEach { area ->
            val navigation = navigationFor(waiter.copy(type = UserType.RECEPTION, area = area))
            assertEquals(everyEmployeeSection, navigation.sections, "$area")
        }
    }

    @Test
    fun anAdministratorOfManagementHasTheCashInTheBarAndTheRestInMore() {
        val navigation = navigationFor(admin)

        assertEquals(AppSection.entries, navigation.sections)
        assertEquals(listOf(AppSection.ORDERS, AppSection.BALANCE), navigation.mainSections)
        assertEquals(
            listOf(
                AppSection.ROOMS,
                AppSection.ATTENDANCE,
                AppSection.SCHEDULES,
                AppSection.ATTENDANCE_REVIEW,
                AppSection.INVENTORY,
                AppSection.EMPLOYEES,
                AppSection.SETTINGS,
            ),
            navigation.moreSections,
        )
    }

    @Test
    fun anAdministratorOfAnotherAreaKeepsTheBarOfTheArea() {
        val navigation = navigationFor(admin.copy(area = UserArea.HOUSEKEEPING))

        assertEquals(listOf(AppSection.ROOMS, AppSection.ORDERS, AppSection.ATTENDANCE), navigation.mainSections)
        assertEquals(AppSection.entries - everyEmployeeSection.toSet(), navigation.moreSections)
    }

    @Test
    fun theAreaNeverGivesPermissionsTheRoleDoesNotHave() {
        val navigation = navigationFor(waiter.copy(area = UserArea.MANAGEMENT))

        assertEquals(everyEmployeeSection, navigation.sections)
        assertEquals(listOf(AppSection.ORDERS), navigation.mainSections)
    }
}
