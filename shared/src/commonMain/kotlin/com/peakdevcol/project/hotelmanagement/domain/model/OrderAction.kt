package com.peakdevcol.project.hotelmanagement.domain.model

/** What can be done to an order after it is created (PRD, section 6.3). */
enum class OrderAction {
    CLOSE,
    REOPEN,
    PAY,
    CANCEL,
}

/** The state the order moves to, or null when the action is not allowed from this state. */
fun OrderState.after(action: OrderAction): OrderState? = when (this) {
    OrderState.IN_PROGRESS -> when (action) {
        OrderAction.CLOSE -> OrderState.CLOSED
        OrderAction.CANCEL -> OrderState.CANCELLED
        OrderAction.REOPEN, OrderAction.PAY -> null
    }
    OrderState.CLOSED -> when (action) {
        OrderAction.REOPEN -> OrderState.IN_PROGRESS
        OrderAction.PAY -> OrderState.PAID
        OrderAction.CANCEL -> OrderState.CANCELLED
        OrderAction.CLOSE -> null
    }
    OrderState.PAID, OrderState.CANCELLED -> null
}

/** P20: only an administrator cancels an order. Everything else is open to any employee. */
fun OrderAction.isAllowedFor(userType: UserType): Boolean =
    this != OrderAction.CANCEL || userType == UserType.ADMIN

/** The actions an employee of this role can take on an order in this state, in the order they are offered. */
fun OrderState.actionsFor(userType: UserType): List<OrderAction> =
    OrderAction.entries.filter { action -> after(action) != null && action.isAllowedFor(userType) }

enum class PaymentMethod {
    CASH,
    TRANSFER,
}

/** A change of state with the data it must leave on the order. */
sealed interface OrderStateChange {
    val action: OrderAction
    val employeeId: String

    data class Close(override val employeeId: String) : OrderStateChange {
        override val action = OrderAction.CLOSE
    }

    data class Reopen(override val employeeId: String) : OrderStateChange {
        override val action = OrderAction.REOPEN
    }

    /** P15: the tip is optional and is kept apart from the amount of the order. */
    data class Pay(
        override val employeeId: String,
        val method: PaymentMethod,
        val tipAmount: Long,
    ) : OrderStateChange {
        override val action = OrderAction.PAY
    }

    data class Cancel(override val employeeId: String, val reason: String) : OrderStateChange {
        override val action = OrderAction.CANCEL
    }
}

/** Why an action on an order was not done. */
sealed class OrderActionError(message: String) : Exception(message) {
    data object NoSession : OrderActionError("There is no logged-in employee")
    data object NotAllowed : OrderActionError("The employee cannot do this action")
    data object MissingReason : OrderActionError("Cancelling needs a reason")
    data object InvalidTip : OrderActionError("The tip cannot be negative")
    data object NotFound : OrderActionError("The order does not exist")

    /** RF-CUR-08: another device changed the order first. */
    data class StateChanged(val currentState: OrderState) : OrderActionError("The order is now $currentState")

    /** RF-CUR-12: changes of state need the server. */
    data object NoConnection : OrderActionError("The server could not be reached")
    data object Unknown : OrderActionError("Unknown error")
}
