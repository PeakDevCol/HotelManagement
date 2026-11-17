package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.usecase.AddProductsToOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReconcilePendingAdditionsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RemoveProductFromOrderUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakePendingAdditionRepository
import com.peakdevcol.project.hotelmanagement.fakes.order
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OrderProductsUseCasesTest {
    private val auth = FakeAuthRepository(userId = "uid-1")
    private val orders = FakeOrderRepository()
    private val pendingAdditions = FakePendingAdditionRepository()
    private val addProducts = AddProductsToOrderUseCase(auth, orders, pendingAdditions)

    private val beer = Product(id = "1", name = "Cerveza Aguila", unitPrice = 3_500, quantity = 2, lineId = "beer")
    private val water = Product(id = "5", name = "Agua", unitPrice = 2_000, quantity = 1, lineId = "water")
    private val soda = Product(id = "3", name = "Coca-Cola", unitPrice = 3_000, quantity = 1)
    private val table = order("table", products = listOf(beer, water), destination = OrderDestination.Table(3))

    init {
        orders.openOrders.value = listOf(table)
    }

    private fun stored() = orders.openOrders.value.single()

    @Test
    fun everyAddedProductIsANewLineWithItsOwnId() = runTest {
        addProducts(table, listOf(soda))
        addProducts(table, listOf(soda))

        val added = stored().products.drop(2)
        assertEquals(listOf("Coca-Cola", "Coca-Cola"), added.map { it.name })
        assertNotNull(added[0].lineId)
        assertNotEquals(added[0].lineId, added[1].lineId)
    }

    @Test
    fun addingTheSameProductThatIsAlreadyInTheOrderKeepsBothLines() = runTest {
        addProducts(table, listOf(beer.copy(lineId = null)))

        assertEquals(listOf("Cerveza Aguila", "Agua", "Cerveza Aguila"), stored().products.map { it.name })
        assertEquals(2 * 3_500L + 2_000L + 2 * 3_500L, stored().totalAmount)
    }

    @Test
    fun productsTheServerConfirmedAreNotRemembered() = runTest {
        val result = addProducts(table, listOf(soda))

        assertEquals(OrderDelivery.SENT, result.getOrNull())
        assertTrue(pendingAdditions.additions.value.isEmpty())
    }

    @Test
    fun productsAddedWithoutConnectionAreRememberedWithTheirOrder() = runTest {
        orders.addProductsResult = Result.success(OrderDelivery.PENDING)

        val result = addProducts(table, listOf(soda))

        assertEquals(OrderDelivery.PENDING, result.getOrNull())
        val remembered = pendingAdditions.additions.value.single()
        assertEquals("table" to OrderDestination.Table(3), remembered.orderId to remembered.destination)
        assertEquals(stored().products.last(), remembered.products.single())
        assertFalse(remembered.rejected)
    }

    @Test
    fun aFailedAdditionIsNotRemembered() = runTest {
        orders.addProductsResult = Result.failure(OrderActionError.NotAllowed)

        val result = addProducts(table, listOf(soda))

        assertEquals(OrderActionError.NotAllowed, result.exceptionOrNull())
        assertTrue(pendingAdditions.additions.value.isEmpty())
    }

    @Test
    fun addingNeedsProductsAndASession() = runTest {
        assertEquals(OrderActionError.NoProducts, addProducts(table, emptyList()).exceptionOrNull())
        val withoutSession = AddProductsToOrderUseCase(FakeAuthRepository(userId = null), orders, pendingAdditions)
        assertEquals(OrderActionError.NoSession, withoutSession(table, listOf(soda)).exceptionOrNull())
    }

    @Test
    fun removingTakesOneLineOutAndTheTotalFollows() = runTest {
        val result = RemoveProductFromOrderUseCase(auth, orders)("table", "water")

        assertTrue(result.isSuccess)
        assertEquals(listOf(beer), stored().products)
        assertEquals(7_000, stored().totalAmount)
    }

    @Test
    fun theLastProductCannotBeRemoved() = runTest {
        val remove = RemoveProductFromOrderUseCase(auth, orders)
        remove("table", "water")

        val result = remove("table", "beer")

        assertEquals(OrderActionError.LastProduct, result.exceptionOrNull())
        assertEquals(listOf(beer), stored().products)
    }

    @Test
    fun productsAreNotRemovedFromAClosedOrder() = runTest {
        orders.openOrders.value = listOf(table.copy(state = OrderState.CLOSED))

        val result = RemoveProductFromOrderUseCase(auth, orders)("table", "water")

        assertEquals(OrderActionError.StateChanged(OrderState.CLOSED), result.exceptionOrNull())
    }

    // ---- RF-CUR-13 (V6): what the server did with the products added without connection

    private val sodaLine = soda.copy(lineId = "soda")
    private val waiting = PendingAddition("addition-1", "table", OrderDestination.Table(3), listOf(sodaLine))

    private fun TestScope.reconcile() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ReconcilePendingAdditionsUseCase(orders, pendingAdditions)()
        }
    }

    @Test
    fun whileTheDeviceIsStillSendingNothingIsDecided() = runTest {
        pendingAdditions.additions.value = listOf(waiting)
        orders.openOrders.value = listOf(table.copy(products = table.products + sodaLine, isPendingToSend = true))

        reconcile()

        assertEquals(listOf(waiting), pendingAdditions.additions.value)
    }

    @Test
    fun withoutNewsFromTheServerNothingIsDecided() = runTest {
        pendingAdditions.additions.value = listOf(waiting)
        orders.openOrders.value = listOf(table.copy(state = OrderState.CLOSED, isUpToDate = false))

        reconcile()

        assertEquals(listOf(waiting), pendingAdditions.additions.value)
    }

    @Test
    fun productsThatReachedTheServerAreForgotten() = runTest {
        pendingAdditions.additions.value = listOf(waiting)
        orders.openOrders.value = listOf(table.copy(products = table.products + sodaLine, isPendingToSend = true))
        reconcile()

        orders.openOrders.value = listOf(table.copy(products = table.products + sodaLine))

        assertTrue(pendingAdditions.additions.value.isEmpty())
    }

    @Test
    fun productsTheServerRejectedBecauseTheOrderWasClosedAreKeptAsRejected() = runTest {
        pendingAdditions.additions.value = listOf(waiting)
        orders.openOrders.value = listOf(table.copy(products = table.products + sodaLine, isPendingToSend = true))
        reconcile()

        orders.openOrders.value = listOf(table.copy(state = OrderState.CLOSED))

        val rejected = pendingAdditions.additions.value.single()
        assertTrue(rejected.rejected)
        assertEquals(listOf(sodaLine), rejected.products)
    }

    @Test
    fun onlyTheMissingProductsOfAnAdditionAreReportedAsRejected() = runTest {
        val juice = Product(id = "9", name = "Jugo", unitPrice = 4_000, lineId = "juice")
        pendingAdditions.additions.value = listOf(waiting.copy(products = listOf(sodaLine, juice)))
        orders.openOrders.value = listOf(table.copy(products = table.products + juice))

        reconcile()

        assertEquals(listOf(sodaLine), pendingAdditions.additions.value.single().products)
    }

    @Test
    fun everyPendingAdditionIsResolvedOneAfterAnother() = runTest {
        val bar = order("bar", products = listOf(beer), state = OrderState.CLOSED)
        val second = PendingAddition("addition-2", "bar", OrderDestination.Bar(), listOf(water.copy(lineId = "late")))
        pendingAdditions.additions.value = listOf(waiting, second)
        orders.openOrders.value = listOf(table.copy(products = table.products + sodaLine), bar)

        reconcile()

        assertEquals(listOf(second.copy(rejected = true)), pendingAdditions.additions.value)
    }
}
