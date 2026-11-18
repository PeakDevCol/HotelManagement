package com.peakdevcol.project.hotelmanagement.domain.model

/** RF-BAL-02: the destinations group by kind, not by room or table number. */
enum class DestinationKind {
    ROOM,
    TABLE,
    POOL,
    BAR,
    OTHER,
}

fun OrderDestination.kind(): DestinationKind = when (this) {
    is OrderDestination.Room -> DestinationKind.ROOM
    is OrderDestination.Table -> DestinationKind.TABLE
    is OrderDestination.Pool -> DestinationKind.POOL
    is OrderDestination.Bar -> DestinationKind.BAR
    OrderDestination.Other -> DestinationKind.OTHER
}

/** RF-BAL-02: a null value does not filter. */
data class BalanceFilter(
    val method: PaymentMethod? = null,
    val employeeId: String? = null,
    val destination: DestinationKind? = null,
    val productId: String? = null,
)

/**
 * RF-BAL-07: tips and taxes apart from the sales. [salesAmount] is what the orders cost, taxes
 * included, as their prices do.
 */
data class BalanceTotals(
    val paymentCount: Int = 0,
    val salesAmount: Long = 0,
    val taxAmount: Long = 0,
    val tipAmount: Long = 0,
) {
    val salesWithoutTax: Long get() = salesAmount - taxAmount

    /** RF-BAL-01: everything that came in, tips included. */
    val totalReceived: Long get() = salesAmount + tipAmount
}

data class DaySales(val date: CalendarDate, val totals: BalanceTotals)

data class MethodSales(val method: PaymentMethod, val totals: BalanceTotals)

/** [employeeName] is null when the profile of who received the payment cannot be read. */
data class EmployeeSales(val employeeId: String, val employeeName: String?, val totals: BalanceTotals)

data class ProductSales(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val amount: Long,
    val taxAmount: Long,
)

/** RF-BAL-01 and RF-BAL-03: the totals of the period and their breakdowns, after the filters. */
data class BalanceSummary(
    val totals: BalanceTotals,
    /** Only the days with payments, the oldest first. */
    val byDay: List<DaySales>,
    /** Every method, also those without payments. */
    val byMethod: List<MethodSales>,
    /** Who received the most first. */
    val byEmployee: List<EmployeeSales>,
    /** What sold the most first. */
    val byProduct: List<ProductSales>,
)

/** An employee or a product the balance can be filtered by. */
data class BalanceOption(val id: String, val name: String?)

/** The payments of a period, read once: changing a filter does not read them again (RNF-12). */
data class BalanceReport(
    val period: BalancePeriod,
    val dayStartMinutes: Int,
    val payments: List<Payment>,
    /** The closings saved during the period. */
    val closings: List<CashClosing> = emptyList(),
    /** The operational day when the report was read, which has not ended yet. */
    val today: CalendarDate? = null,
) {
    /**
     * RF-BAL-06: the operational days that had payments and ended without a "Cierre del día".
     * A daily closing belongs to the operational day in which it is saved.
     */
    fun daysWithoutDailyClosing(): Set<CalendarDate> {
        val daysWithDailyClosing = closings
            .filter { it.type == CashClosingType.DAILY }
            .mapNotNull { closing -> closing.createdAt?.let { operationalDateOf(it, dayStartMinutes) } }
            .toSet()
        return payments.map { operationalDateOf(it.receivedAt, dayStartMinutes) }
            .filter { day -> today == null || day < today }
            .filterNot { it in daysWithDailyClosing }
            .toSet()
    }

    /** Who received payments in the period, by name. */
    fun employeeOptions(employeeNames: Map<String, String>): List<BalanceOption> =
        payments.map { it.receivedBy }.distinct()
            .map { id -> BalanceOption(id, employeeNames[id]) }
            .sortedBy { it.name?.lowercase() ?: "" }

    /** What was sold in the period, by name. A product renamed meanwhile shows its latest name. */
    fun productOptions(): List<BalanceOption> =
        payments.sortedBy { it.receivedAt }.flatMap { it.lines }
            .associate { line -> line.id to line.name }
            .map { (id, name) -> BalanceOption(id, name) }
            .sortedBy { it.name?.lowercase() }

    fun summary(filter: BalanceFilter, employeeNames: Map<String, String>): BalanceSummary {
        val sales = payments.filter { it.matches(filter) }.mapNotNull { it.onlyProduct(filter.productId) }
        return BalanceSummary(
            totals = sales.totals(),
            byDay = sales.groupBy { operationalDateOf(it.receivedAt, dayStartMinutes) }
                .map { (date, payments) -> DaySales(date, payments.totals()) }
                .sortedBy { it.date },
            byMethod = PaymentMethod.entries.map { method -> MethodSales(method, sales.filter { it.method == method }.totals()) },
            byEmployee = sales.groupBy { it.receivedBy }
                .map { (employeeId, payments) -> EmployeeSales(employeeId, employeeNames[employeeId], payments.totals()) }
                .sortedByDescending { it.totals.totalReceived },
            byProduct = sales.productSales(),
        )
    }
}

private fun Payment.matches(filter: BalanceFilter): Boolean =
    (filter.method == null || method == filter.method) &&
        (filter.employeeId == null || receivedBy == filter.employeeId) &&
        (filter.destination == null || destination?.kind() == filter.destination)

/**
 * Decision of 2026-11-17: filtering by a product shows what that product sold, not the whole
 * payments that include it. A tip is for the whole payment, so it is left out.
 */
private fun Payment.onlyProduct(productId: String?): Payment? {
    if (productId == null) return this
    val productLines = lines.filter { it.id == productId }
    if (productLines.isEmpty()) return null
    return copy(amount = productLines.totalAmount(), taxAmount = productLines.taxAmount(), tipAmount = 0, lines = productLines)
}

private fun List<Payment>.totals() = BalanceTotals(
    paymentCount = size,
    salesAmount = sumOf { it.amount },
    taxAmount = sumOf { it.taxAmount },
    tipAmount = sumOf { it.tipAmount },
)

private fun List<Payment>.productSales(): List<ProductSales> =
    sortedBy { it.receivedAt }.flatMap { it.lines }
        .groupBy { it.id }
        .map { (productId, lines) ->
            ProductSales(
                productId = productId,
                productName = lines.last().name,
                quantity = lines.sumOf { it.quantity },
                amount = lines.totalAmount(),
                taxAmount = lines.taxAmount(),
            )
        }
        .sortedByDescending { it.amount }
