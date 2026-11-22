package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.AppSection
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.model.navigationFor
import com.peakdevcol.project.hotelmanagement.fakes.employee
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** RF-AUTH-05 and RF-EMP-05, with the sections built so far (PRD, section 13, 2026-11-20, and decisions 2A of M10 and M11). */
class AppNavigationTest {
    private val waiter = employee("waiter", "Ana Gómez")
    private val admin = employee("admin", "Laura Méndez", type = UserType.ADMIN).copy(area = UserArea.MANAGEMENT)
    private val everyEmployeeSection = listOf(AppSection.ORDERS, AppSection.ROOMS, AppSection.ATTENDANCE)
    private val receptionSections = listOf(AppSection.ORDERS, AppSection.ROOMS, AppSection.RESERVATIONS, AppSection.ATTENDANCE)

    @Test
    fun aWaiterOpensOnTheOrdersAndHasTheRoomsInMore() {
        val navigation = navigationFor(waiter)

        assertEquals(everyEmployeeSection, navigation.sections)
        assertEquals(listOf(AppSection.ORDERS, AppSection.ATTENDANCE), navigation.mainSections)
        assertEquals(AppSection.ORDERS, navigation.startSection)
        assertEquals(listOf(AppSection.ROOMS), navigation.moreSections)
    }

    @Test
    fun receptionOpensOnTheRoomsWithTheReservationsBesideThem() {
        val navigation = navigationFor(waiter.copy(type = UserType.RECEPTION, area = UserArea.RECEPTION))

        assertEquals(listOf(AppSection.ROOMS, AppSection.RESERVATIONS, AppSection.ORDERS, AppSection.ATTENDANCE), navigation.mainSections)
        assertEquals(AppSection.ROOMS, navigation.startSection)
        assertTrue(navigation.moreSections.isEmpty())
    }

    @Test
    fun housekeepingAndMaintenanceOpenOnTheRoomsWithoutReservations() {
        listOf(UserArea.HOUSEKEEPING, UserArea.MAINTENANCE).forEach { area ->
            val navigation = navigationFor(waiter.copy(area = area))

            assertEquals(listOf(AppSection.ROOMS, AppSection.ORDERS, AppSection.ATTENDANCE), navigation.mainSections, "$area")
            assertEquals(AppSection.ROOMS, navigation.startSection, "$area")
            assertTrue(navigation.moreSections.isEmpty(), "$area")
        }
    }

    @Test
    fun aBasicEmployeeOfReceptionDoesNotSeeTheGuests() {
        val navigation = navigationFor(waiter.copy(area = UserArea.RECEPTION))

        assertEquals(everyEmployeeSection, navigation.sections)
        assertEquals(listOf(AppSection.ROOMS, AppSection.ORDERS, AppSection.ATTENDANCE), navigation.mainSections)
    }

    @Test
    fun theReceptionRoleHasTheReservationsInAnyArea() {
        UserArea.entries.forEach { area ->
            val navigation = navigationFor(waiter.copy(type = UserType.RECEPTION, area = area))
            assertEquals(receptionSections, navigation.sections, "$area")
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
                AppSection.RESERVATIONS,
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
