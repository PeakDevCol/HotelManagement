package com.peakdevcol.project.hotelmanagement.domain.model

/** Why a change to an employee was not made. */
sealed class EmployeeError(message: String) : Exception(message) {
    data object InvalidName : EmployeeError("The name is too short")
    data object InvalidEmail : EmployeeError("The email is not valid")
    data object EmailAlreadyInUse : EmployeeError("There is already an account with that email")

    /** RF-EMP-03: nobody takes the administrator role or the access away from themselves, so one always remains. */
    data object OwnAccess : EmployeeError("An administrator cannot change their own role or deactivate themselves")
    data object NoSession : EmployeeError("There is no logged-in employee")
    data object NotAllowed : EmployeeError("Only an administrator manages the employees")
    data object NotFound : EmployeeError("The employee no longer exists")
    data object Network : EmployeeError("The server could not be reached")
    data object Unknown : EmployeeError("Unknown error")
}

/** RF-EMP-04: what happened after creating an employee. */
enum class EmployeeCreation {
    /** The account exists and the employee got the email to choose their password. */
    INVITED,

    /** The account exists, but the email could not be sent. It can be sent again from the employee. */
    CREATED_WITHOUT_EMAIL,
}
