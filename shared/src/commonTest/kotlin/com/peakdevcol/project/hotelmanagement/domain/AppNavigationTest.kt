package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.AppSection
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.model.navigationFor
import com.peakdevcol.project.hotelmanagement.fakes.employee
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** RF-AUTH-05 and RF-EMP-05, with the sections of phase 1 (PRD, section 13, 2026-11-20). */
class AppNavigationTest {
    private val waiter = employee("waiter", "Ana Gómez")
    private val admin = employee("admin", "Laura Méndez", type = UserType.ADMIN).copy(area = UserArea.MANAGEMENT)

    @Test
    fun anEmployeeOpensOnTheOrdersAndHasAttendanceInTheBar() {
        val navigation = navigationFor(waiter)

        assertEquals(listOf(AppSection.ORDERS, AppSection.ATTENDANCE), navigation.sections)
        assertEquals(listOf(AppSection.ORDERS, AppSection.ATTENDANCE), navigation.mainSections)
        assertEquals(AppSection.ORDERS, navigation.startSection)
        assertTrue(navigation.moreSections.isEmpty())
    }

    @Test
    fun everyAreaWithoutAdministrationOpensOnTheOrders() {
        UserArea.entries.forEach { area ->
            val navigation = navigationFor(waiter.copy(type = UserType.RECEPTION, area = area))
            assertEquals(listOf(AppSection.ORDERS, AppSection.ATTENDANCE), navigation.sections, "$area")
        }
    }

    @Test
    fun anAdministratorOfManagementHasTheCashInTheBarAndTheRestInMore() {
        val navigation = navigationFor(admin)

        assertEquals(AppSection.entries, navigation.sections)
        assertEquals(listOf(AppSection.ORDERS, AppSection.BALANCE), navigation.mainSections)
        assertEquals(
            listOf(AppSection.ATTENDANCE, AppSection.SCHEDULES, AppSection.ATTENDANCE_REVIEW, AppSection.INVENTORY, AppSection.EMPLOYEES, AppSection.SETTINGS),
            navigation.moreSections,
        )
    }

    @Test
    fun anAdministratorOfAnotherAreaKeepsTheBarOfTheArea() {
        val navigation = navigationFor(admin.copy(area = UserArea.SERVICE))

        assertEquals(listOf(AppSection.ORDERS, AppSection.ATTENDANCE), navigation.mainSections)
        assertEquals(AppSection.entries - AppSection.ORDERS - AppSection.ATTENDANCE, navigation.moreSections)
    }

    @Test
    fun theAreaNeverGivesPermissionsTheRoleDoesNotHave() {
        val navigation = navigationFor(waiter.copy(area = UserArea.MANAGEMENT))

        assertEquals(listOf(AppSection.ORDERS, AppSection.ATTENDANCE), navigation.sections)
        assertEquals(listOf(AppSection.ORDERS), navigation.mainSections)
    }
}
