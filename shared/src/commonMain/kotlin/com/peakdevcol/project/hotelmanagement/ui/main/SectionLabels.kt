package com.peakdevcol.project.hotelmanagement.ui.main

import com.peakdevcol.project.hotelmanagement.domain.model.AppSection
import com.peakdevcol.project.hotelmanagement.domain.model.SectionGroup
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.menu_employees
import com.peakdevcol.project.hotelmanagement.resources.menu_inventory
import com.peakdevcol.project.hotelmanagement.resources.menu_settings
import com.peakdevcol.project.hotelmanagement.resources.nav_cash
import com.peakdevcol.project.hotelmanagement.resources.nav_attendance
import com.peakdevcol.project.hotelmanagement.resources.nav_attendance_review
import com.peakdevcol.project.hotelmanagement.resources.nav_cash_and_balance
import com.peakdevcol.project.hotelmanagement.resources.nav_group_administration
import com.peakdevcol.project.hotelmanagement.resources.nav_group_operation
import com.peakdevcol.project.hotelmanagement.resources.nav_orders
import com.peakdevcol.project.hotelmanagement.resources.nav_schedules
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/** The name of the section in the side navigation and on top of its screen. */
fun AppSection.title(): StringResource = when (this) {
    AppSection.ORDERS -> Res.string.nav_orders
    AppSection.ATTENDANCE -> Res.string.nav_attendance
    AppSection.BALANCE -> Res.string.nav_cash_and_balance
    AppSection.SCHEDULES -> Res.string.nav_schedules
    AppSection.ATTENDANCE_REVIEW -> Res.string.nav_attendance_review
    AppSection.INVENTORY -> Res.string.menu_inventory
    AppSection.EMPLOYEES -> Res.string.menu_employees
    AppSection.SETTINGS -> Res.string.menu_settings
}

/** The name in the bottom bar of the phone, where there is room for one short word. */
fun AppSection.shortTitle(): StringResource = when (this) {
    AppSection.BALANCE -> Res.string.nav_cash
    else -> title()
}

fun AppSection.icon(): DrawableResource = when (this) {
    AppSection.ORDERS -> MajesticIcons.receipt
    AppSection.ATTENDANCE -> MajesticIcons.attendance
    AppSection.BALANCE -> MajesticIcons.payments
    AppSection.SCHEDULES -> MajesticIcons.calendar
    AppSection.ATTENDANCE_REVIEW -> MajesticIcons.editNote
    AppSection.INVENTORY -> MajesticIcons.inventory
    AppSection.EMPLOYEES -> MajesticIcons.employees
    AppSection.SETTINGS -> MajesticIcons.settings
}

fun AppSection.selectedIcon(): DrawableResource = when (this) {
    AppSection.ORDERS -> MajesticIcons.receiptFilled
    AppSection.ATTENDANCE -> MajesticIcons.attendanceFilled
    AppSection.BALANCE -> MajesticIcons.paymentsFilled
    AppSection.SCHEDULES -> MajesticIcons.calendar
    AppSection.ATTENDANCE_REVIEW -> MajesticIcons.editNote
    AppSection.INVENTORY -> MajesticIcons.inventoryFilled
    AppSection.EMPLOYEES -> MajesticIcons.employeesFilled
    AppSection.SETTINGS -> MajesticIcons.settingsFilled
}

fun SectionGroup.title(): StringResource = when (this) {
    SectionGroup.OPERATION -> Res.string.nav_group_operation
    SectionGroup.ADMINISTRATION -> Res.string.nav_group_administration
}
