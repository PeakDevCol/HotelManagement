package com.peakdevcol.project.hotelmanagement.ui.employees

import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeError
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.area_housekeeping
import com.peakdevcol.project.hotelmanagement.resources.area_maintenance
import com.peakdevcol.project.hotelmanagement.resources.area_management
import com.peakdevcol.project.hotelmanagement.resources.area_reception
import com.peakdevcol.project.hotelmanagement.resources.area_service
import com.peakdevcol.project.hotelmanagement.resources.employee_error_email_in_use
import com.peakdevcol.project.hotelmanagement.resources.employee_error_name
import com.peakdevcol.project.hotelmanagement.resources.employee_error_network
import com.peakdevcol.project.hotelmanagement.resources.employee_error_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.employee_error_own_access
import com.peakdevcol.project.hotelmanagement.resources.employee_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.employee_not_found
import com.peakdevcol.project.hotelmanagement.resources.error_email
import com.peakdevcol.project.hotelmanagement.resources.order_error_no_session
import com.peakdevcol.project.hotelmanagement.resources.role_admin
import com.peakdevcol.project.hotelmanagement.resources.role_basic
import com.peakdevcol.project.hotelmanagement.resources.role_reception
import org.jetbrains.compose.resources.StringResource

fun UserType.label(): StringResource = when (this) {
    UserType.BASIC -> Res.string.role_basic
    UserType.RECEPTION -> Res.string.role_reception
    UserType.ADMIN -> Res.string.role_admin
}

fun UserArea.label(): StringResource = when (this) {
    UserArea.SERVICE -> Res.string.area_service
    UserArea.RECEPTION -> Res.string.area_reception
    UserArea.HOUSEKEEPING -> Res.string.area_housekeeping
    UserArea.MAINTENANCE -> Res.string.area_maintenance
    UserArea.MANAGEMENT -> Res.string.area_management
}

fun EmployeeError.message(): StringResource = when (this) {
    EmployeeError.InvalidName -> Res.string.employee_error_name
    EmployeeError.InvalidEmail -> Res.string.error_email
    EmployeeError.EmailAlreadyInUse -> Res.string.employee_error_email_in_use
    EmployeeError.OwnAccess -> Res.string.employee_error_own_access
    EmployeeError.NoSession -> Res.string.order_error_no_session
    EmployeeError.NotAllowed -> Res.string.employee_error_not_allowed
    EmployeeError.NotFound -> Res.string.employee_not_found
    EmployeeError.Network -> Res.string.employee_error_network
    EmployeeError.Unknown -> Res.string.employee_error_unknown
}
