package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.BalanceFilter
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceOption
import com.peakdevcol.project.hotelmanagement.domain.model.BalancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceReport
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceTotals
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.DestinationKind
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.Payment
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.ProductSales
import com.peakdevcol.project.hotelmanagement.fakes.beer
import com.peakdevcol.project.hotelmanagement.fakes.burger
import com.peakdevcol.project.hotelmanagement.fakes.payment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BalanceTest {
    private val names = mapOf("uid-1" to "Ana", "uid-2" to "Beto")

    private fun reportOf(vararg payments: Payment) = BalanceReport(
        period = BalancePeriod(CalendarDate(2026, 11, 1), CalendarDate(2026, 11, 30)),
        dayStartMinutes = 6 * 60,
        payments = payments.toList(),
    )

    private val beerInCash = payment("order_a", "2026-11-16T20:00:00Z", lines = listOf(beer), tipAmount = 500)
    private val burgerByTransfer = payment(
        "order_b",
        "2026-11-17T09:00:00Z",
        lines = listOf(burger, beer.copy(lineId = "line-2", quantity = 2)),
        method = PaymentMethod.TRANSFER,
        receivedBy = "uid-2",
        destination = OrderDestination.Room(204),
    )

    @Test
    fun separatesTipsAndTaxesFromTheSales() {
        val totals = reportOf(beerInCash, burgerByTransfer).summary(BalanceFilter(), names).totals

        assertEquals(BalanceTotals(paymentCount = 2, salesAmount = 22_400, taxAmount = 1_900, tipAmount = 500), totals)
        assertEquals(20_500, totals.salesWithoutTax)
        assertEquals(22_900, totals.totalReceived)
    }

    @Test
    fun groupsByOperationalDaySoTheNightShiftIsNotSplit() {
        // 3:00 p. m. of 16 nov and 4:00 a. m. of 17 nov in Colombia: the same operational day.
        val byDay = reportOf(beerInCash, burgerByTransfer).summary(BalanceFilter(), names).byDay

        assertEquals(listOf(CalendarDate(2026, 11, 16)), byDay.map { it.date })
        assertEquals(2, byDay.single().totals.paymentCount)
    }

    @Test
    fun showsEveryMethodAlsoWithoutPayments() {
        val byMethod = reportOf(beerInCash).summary(BalanceFilter(), names).byMethod

        assertEquals(listOf(PaymentMethod.CASH to 4_000L, PaymentMethod.TRANSFER to 0L), byMethod.map { it.method to it.totals.totalReceived })
    }

    @Test
    fun putsWhoReceivedTheMostFirst() {
        val byEmployee = reportOf(beerInCash, burgerByTransfer).summary(BalanceFilter(), names).byEmployee

        assertEquals(listOf("Beto", "Ana"), byEmployee.map { it.employeeName })
    }

    @Test
    fun addsUpTheLinesOfEachProduct() {
        val byProduct = reportOf(beerInCash, burgerByTransfer).summary(BalanceFilter(), names).byProduct

        assertEquals(
            listOf(
                ProductSales("burger", "Hamburguesa", quantity = 1, amount = 11_900, taxAmount = 1_900),
                ProductSales("beer", "Cerveza Aguila", quantity = 3, amount = 10_500, taxAmount = 0),
            ),
            byProduct,
        )
    }

    @Test
    fun filtersByMethodEmployeeAndDestinationKind() {
        val report = reportOf(beerInCash, burgerByTransfer)

        assertEquals(1, report.summary(BalanceFilter(method = PaymentMethod.TRANSFER), names).totals.paymentCount)
        assertEquals(1, report.summary(BalanceFilter(employeeId = "uid-1"), names).totals.paymentCount)
        assertEquals(1, report.summary(BalanceFilter(destination = DestinationKind.ROOM), names).totals.paymentCount)
        assertEquals(0, report.summary(BalanceFilter(destination = DestinationKind.BAR), names).totals.paymentCount)
    }

    @Test
    fun aProductFilterShowsWhatThatProductSoldWithoutTips() {
        val summary = reportOf(beerInCash, burgerByTransfer).summary(BalanceFilter(productId = "beer"), names)

        assertEquals(BalanceTotals(paymentCount = 2, salesAmount = 10_500, taxAmount = 0, tipAmount = 0), summary.totals)
        assertEquals(listOf(3), summary.byProduct.map { it.quantity })
    }

    @Test
    fun paymentsWithoutDestinationOnlyLeaveWhenADestinationIsChosen() {
        val beforeM5 = payment("order_c", "2026-11-18T15:00:00Z", lines = emptyList(), destination = null).copy(amount = 8_000)
        val report = reportOf(beforeM5)

        assertEquals(8_000, report.summary(BalanceFilter(), names).totals.salesAmount)
        assertEquals(0, report.summary(BalanceFilter(destination = DestinationKind.TABLE), names).totals.paymentCount)
        assertTrue(report.summary(BalanceFilter(), names).byProduct.isEmpty())
    }

    @Test
    fun offersTheEmployeesAndProductsOfThePeriodByName() {
        val renamedBeer = payment("order_d", "2026-11-20T15:00:00Z", lines = listOf(beer.copy(name = "Cerveza Águila")))
        val report = reportOf(beerInCash, burgerByTransfer, renamedBeer)

        assertEquals(listOf(BalanceOption("uid-1", "Ana"), BalanceOption("uid-2", "Beto")), report.employeeOptions(names))
        assertEquals(
            listOf(BalanceOption("beer", "Cerveza Águila"), BalanceOption("burger", "Hamburguesa")),
            report.productOptions(),
        )
    }
}
