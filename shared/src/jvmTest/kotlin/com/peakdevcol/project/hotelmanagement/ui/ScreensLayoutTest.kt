package com.peakdevcol.project.hotelmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.domain.model.BackupSummary
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceFilter
import com.peakdevcol.project.hotelmanagement.domain.model.BalancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceReport
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceSummary
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceTotals
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosing
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingDraft
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingOverview
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingType
import com.peakdevcol.project.hotelmanagement.domain.model.CatalogGroup
import com.peakdevcol.project.hotelmanagement.domain.model.Category
import com.peakdevcol.project.hotelmanagement.domain.model.DaySales
import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeSales
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettingsDetail
import com.peakdevcol.project.hotelmanagement.domain.model.Inventory
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryProduct
import com.peakdevcol.project.hotelmanagement.domain.model.MethodSales
import com.peakdevcol.project.hotelmanagement.domain.model.OrderAction
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDetail
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEvent
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEventType
import com.peakdevcol.project.hotelmanagement.domain.model.OrderListing
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.Payment
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.PendingCollection
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.ProductSales
import com.peakdevcol.project.hotelmanagement.domain.model.ProductSelection
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.model.byStartDay
import com.peakdevcol.project.hotelmanagement.domain.model.navigationFor
import com.peakdevcol.project.hotelmanagement.domain.model.weekDays
import com.peakdevcol.project.hotelmanagement.domain.usecase.ScheduleDay
import com.peakdevcol.project.hotelmanagement.domain.usecase.ScheduledShift
import com.peakdevcol.project.hotelmanagement.fakes.order
import com.peakdevcol.project.hotelmanagement.fakes.shift
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderContent
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderState
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddProductsContent
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddProductsState
import com.peakdevcol.project.hotelmanagement.ui.addorder.ProductPickerState
import com.peakdevcol.project.hotelmanagement.ui.attendance.AttendanceContent
import com.peakdevcol.project.hotelmanagement.ui.attendance.AttendanceState
import com.peakdevcol.project.hotelmanagement.ui.balance.BalanceContent
import com.peakdevcol.project.hotelmanagement.ui.balance.BalanceState
import com.peakdevcol.project.hotelmanagement.ui.balance.CashClosingContent
import com.peakdevcol.project.hotelmanagement.ui.balance.CashClosingState
import com.peakdevcol.project.hotelmanagement.ui.currentorders.ChargeFields
import com.peakdevcol.project.hotelmanagement.ui.currentorders.ChargeForm
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CurrentOrdersContent
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CurrentOrdersState
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailContent
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailState
import com.peakdevcol.project.hotelmanagement.ui.currentorders.TipChoice
import com.peakdevcol.project.hotelmanagement.ui.designsystem.FRAME_TAG
import com.peakdevcol.project.hotelmanagement.ui.designsystem.LayoutFrame
import com.peakdevcol.project.hotelmanagement.ui.designsystem.assertNothingSpills
import com.peakdevcol.project.hotelmanagement.ui.designsystem.saveSnapshotIfAsked
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeeFormContent
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeeFormState
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeesContent
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeesState
import com.peakdevcol.project.hotelmanagement.ui.employees.OpenEmployee
import com.peakdevcol.project.hotelmanagement.ui.inventory.CategoriesContent
import com.peakdevcol.project.hotelmanagement.ui.inventory.CategoriesState
import com.peakdevcol.project.hotelmanagement.ui.inventory.InventoryContent
import com.peakdevcol.project.hotelmanagement.ui.inventory.InventoryState
import com.peakdevcol.project.hotelmanagement.ui.inventory.OpenProduct
import com.peakdevcol.project.hotelmanagement.ui.inventory.ProductFormContent
import com.peakdevcol.project.hotelmanagement.ui.inventory.ProductFormState
import com.peakdevcol.project.hotelmanagement.ui.inventory.TaxesContent
import com.peakdevcol.project.hotelmanagement.ui.inventory.TaxesState
import com.peakdevcol.project.hotelmanagement.ui.login.LoginContent
import com.peakdevcol.project.hotelmanagement.ui.login.LoginMessage
import com.peakdevcol.project.hotelmanagement.ui.login.LoginState
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import com.peakdevcol.project.hotelmanagement.ui.main.LocalWideWindow
import com.peakdevcol.project.hotelmanagement.ui.main.MainState
import com.peakdevcol.project.hotelmanagement.ui.main.MoreScreen
import com.peakdevcol.project.hotelmanagement.ui.main.rememberAppMessages
import com.peakdevcol.project.hotelmanagement.ui.schedules.SchedulesContent
import com.peakdevcol.project.hotelmanagement.ui.schedules.SchedulesState
import com.peakdevcol.project.hotelmanagement.ui.settings.SettingsContent
import com.peakdevcol.project.hotelmanagement.ui.settings.SettingsState
import kotlin.test.Test
import kotlin.time.Instant

/**
 * RNF-18 on whole screens: with long names and large amounts, at the narrowest phone (320 dp), a
 * common phone (360 dp), a tablet (720 dp) and a desktop window (1024 dp, where lists show their
 * detail beside them), with the text of the device at 100 % and 130 %.
 */
@OptIn(ExperimentalTestApi::class)
class ScreensLayoutTest {
    private val frames = listOf(320.dp, 360.dp, 720.dp, 1024.dp).flatMap { width -> listOf(1f, 1.3f).map { scale -> LayoutFrame(width, scale) } }

    private fun checkEveryFrame(name: String, content: @Composable () -> Unit) = runDesktopComposeUiTest(width = 1100, height = 900) {
        var frame by mutableStateOf(frames.first())
        setContent { ScreenFrame(frame, content) }
        frames.forEach { next ->
            frame = next
            waitForIdle()
            assertNothingSpills("$name a $next")
        }
        listOf(LayoutFrame(360.dp, 1f), LayoutFrame(360.dp, 1f, darkTheme = true), LayoutFrame(1024.dp, 1f)).forEach { snapshot ->
            frame = snapshot
            waitForIdle()
            saveSnapshotIfAsked(name, snapshot)
        }
    }

    // ---- Fake data, long on purpose

    private val now = Instant.parse("2026-11-20T01:00:00Z")
    private val beer = Product(id = "beer", name = "Cerveza Club Colombia dorada", unitPrice = 7_000, quantity = 2, lineId = "beer-line")
    private val burger = Product(id = "burger", name = "Hamburguesa de la casa con papas a la francesa", unitPrice = 1_284_000, quantity = 1, lineId = "burger-line", taxRate = 800)
    private val water = Product(id = "water", name = "Agua", unitPrice = 5_000)
    private val roomOrder = order("room", destination = OrderDestination.Room(1204), products = listOf(beer, burger), createdAt = "2026-11-19T22:45:00Z")
    private val employeeName = "Ana María Rojas Restrepo de la Cruz"
    private val admin = User("admin", "Laura Méndez Restrepo de la Cruz", "laura.mendez@hoteljuanmajestic.com", UserType.ADMIN, UserArea.MANAGEMENT, active = true)

    private val detailState = OrderDetailState.Loaded(
        detail = OrderDetail(
            order = roomOrder,
            employeeName = employeeName,
            history = listOf(
                OrderEvent(OrderEventType.CREATED, "ana", employeeName, at = Instant.parse("2026-11-19T22:45:00Z")),
                OrderEvent(OrderEventType.PRODUCTS_ADDED, "ana", employeeName, at = Instant.parse("2026-11-19T23:10:00Z"), products = listOf(burger)),
                OrderEvent(OrderEventType.REOPENED, "admin", admin.fullName, at = null),
            ),
            availableActions = listOf(OrderAction.CLOSE, OrderAction.PAY, OrderAction.CANCEL),
        ),
    )

    private val catalog = listOf(
        CatalogGroup("Bebidas frías y calientes", listOf(beer.copy(quantity = 1, lineId = null), water, Product("lemonade", "Limonada de coco con hierbabuena y jengibre", 9_500))),
        CatalogGroup("Cocina", listOf(burger.copy(quantity = 1, lineId = null))),
        CatalogGroup(null, listOf(Product("wine", "Botella de vino reserva especial", 1_250_000))),
    )
    private val picker = ProductPickerState(
        catalog = catalog,
        catalogLoaded = true,
        selection = ProductSelection().add(water).increase(water.id).add(beer),
    )

    private val categories = listOf(Category("drinks", "Bebidas frías y calientes de la carta", 0), Category("kitchen", "Cocina", 1))
    private val taxes = listOf(Tax("consumption", "Impuesto nacional al consumo de restaurantes", 800), Tax("vat", "IVA", 1_900, active = false))

    private val totals = BalanceTotals(paymentCount = 1_284, salesAmount = 18_430_500, taxAmount = 2_124_500, tipAmount = 1_092_000)
    private val closing = CashClosing(
        number = 41,
        from = Instant.parse("2026-11-18T11:00:00Z"),
        to = Instant.parse("2026-11-19T11:00:00Z"),
        type = CashClosingType.DAILY,
        expectedCash = 1_284_000,
        countedCash = 1_280_000,
        difference = -4_000,
        note = "Faltó el cambio de la mesa 4, que se devolvió al huésped de la habitación 1204 al día siguiente.",
        closedBy = "admin",
        createdAt = Instant.parse("2026-11-19T11:05:00Z"),
    )
    private val payment = Payment(
        id = "payment",
        amount = 18_430_500,
        tipAmount = 1_092_000,
        taxAmount = 0,
        method = PaymentMethod.CASH,
        sourceId = "room",
        receivedBy = "admin",
        receivedAt = Instant.parse("2026-11-19T23:00:00Z"),
        destination = OrderDestination.Room(1204),
        lines = emptyList(),
    )

    // ---- Screens

    @Test
    fun login() = checkEveryFrame("inicio de sesión") {
        LoginContent(
            state = LoginState(email = "andres.gomez.rodriguez@hoteljuanmajestic.com", password = "contraseña", message = LoginMessage.PasswordEmailRequested),
            onIntent = {},
        )
    }

    @Test
    fun orders() = checkEveryFrame("lista de pedidos") {
        CurrentOrdersContent(
            state = CurrentOrdersState(
                isLoading = false,
                orders = listOf(
                    OrderListing(roomOrder, employeeName, isMine = true),
                    OrderListing(order("closed", state = OrderState.CLOSED, destination = OrderDestination.Table(4)), "Andrés", isMine = false),
                    OrderListing(order("pending", destination = OrderDestination.Pool(3), isPendingToSend = true), "Laura", isMine = false),
                ),
                selectedOrderId = "room",
                now = now,
            ),
            onIntent = {},
            orderDetail = { OrderDetailContent(state = detailState, onIntent = {}, onBack = null, onAddProducts = {}) },
        )
    }

    @Test
    fun orderDetail() = checkEveryFrame("detalle del pedido") {
        OrderDetailContent(state = detailState, onIntent = {}, onBack = {}, onAddProducts = {})
    }

    @Test
    fun charge() = checkEveryFrame("cobro") {
        val form = ChargeForm(method = PaymentMethod.TRANSFER, tip = TipChoice.OTHER, otherTipDigits = "1250000")
        Column(
            modifier = Modifier.fillMaxWidth().background(MajesticTheme.colors.surfaceRaised).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ChargeFields(state = detailState.copy(charge = form), form = form, onIntent = {})
        }
    }

    @Test
    fun newOrder() = checkEveryFrame("nuevo pedido") {
        AddOrderContent(state = AddOrderState(picker = picker, destination = OrderDestination.Room(1204)), onIntent = {}, onClose = {})
    }

    @Test
    fun addProducts() = checkEveryFrame("agregar productos") {
        AddProductsContent(
            state = AddProductsState(picker = picker, order = order("closed", state = OrderState.CLOSED, destination = OrderDestination.Room(1204))),
            onIntent = {},
            onClose = {},
        )
    }

    @Test
    fun balance() = checkEveryFrame("balance") {
        val days = listOf(CalendarDate(2026, 11, 12), CalendarDate(2026, 11, 14))
        BalanceContent(
            state = BalanceState(
                isLoading = false,
                report = BalanceReport(BalancePeriod(CalendarDate(2026, 11, 1), CalendarDate(2026, 11, 30)), dayStartMinutes = 360, payments = emptyList()),
                filter = BalanceFilter(method = PaymentMethod.CASH, employeeId = "admin"),
                summary = BalanceSummary(
                    totals = totals,
                    byDay = days.map { DaySales(it, totals) },
                    byMethod = PaymentMethod.entries.map { MethodSales(it, totals) },
                    byEmployee = listOf(EmployeeSales("admin", admin.fullName, totals)),
                    byProduct = listOf(ProductSales("burger", burger.name, quantity = 1_284, amount = 18_430_500, taxAmount = 2_124_500)),
                ),
                pending = PendingCollection(inProgressCount = 3, inProgressAmount = 300_000, closedCount = 6, closedAmount = 112_000),
                daysWithoutDailyClosing = days.toSet(),
            ),
            onIntent = {},
            onCashClosingClick = {},
        )
    }

    @Test
    fun cashClosing() = checkEveryFrame("cierre de caja") {
        CashClosingContent(
            state = CashClosingState(
                isLoading = false,
                overview = CashClosingOverview(draft = CashClosingDraft(lastClosing = closing, payments = listOf(payment)), recentClosings = listOf(closing)),
                employeeNames = mapOf("admin" to admin.fullName),
                type = CashClosingType.MANUAL,
                countedText = "19400000",
                note = "Faltan billetes de la caja menor.",
            ),
            onIntent = {},
            onBack = {},
        )
    }

    @Test
    fun inventory() = checkEveryFrame("inventario") {
        val inventory = Inventory(
            categories = categories,
            products = listOf(
                InventoryProduct("beer", beer.name, 7_000, "drinks", taxId = "consumption"),
                InventoryProduct("lemonade", "Limonada de coco con hierbabuena y jengibre", 9_500, "drinks", taxId = "vat", active = false),
                InventoryProduct("wine", "Botella de vino reserva especial", 1_250_000, "gone"),
            ),
            taxes = taxes,
        )
        InventoryContent(
            state = InventoryState(isLoading = false, inventory = inventory, openProduct = OpenProduct("beer")),
            onIntent = {},
            productForm = { _, _ -> ProductFormContent(state = productFormState(), onIntent = {}, onBack = {}) },
            // Opened from "Más" on the phone, with the arrow back.
            onBack = {},
        )
    }

    private fun productFormState() = ProductFormState(
        isLoading = false,
        isNew = false,
        name = "Limonada de coco con hierbabuena y jengibre",
        priceText = "1250000",
        categoryId = "drinks",
        taxId = "consumption",
        categories = categories,
        taxes = taxes,
        fieldError = InventoryError.InvalidPrice,
    )

    @Test
    fun productForm() = checkEveryFrame("producto") {
        ProductFormContent(state = productFormState(), onIntent = {}, onBack = {})
    }

    @Test
    fun categories() = checkEveryFrame("categorías") {
        CategoriesContent(state = CategoriesState(isLoading = false, categories = categories), onIntent = {}, onBack = {})
    }

    @Test
    fun taxes() = checkEveryFrame("impuestos") {
        TaxesContent(state = TaxesState(isLoading = false, taxes = taxes), onIntent = {}, onBack = {})
    }

    private fun employeeFormState() = EmployeeFormState(
        isLoading = false,
        isNew = false,
        fullName = admin.fullName,
        email = admin.email,
        type = UserType.ADMIN,
        area = UserArea.MANAGEMENT,
        isOwnProfile = true,
    )

    @Test
    fun employees() = checkEveryFrame("empleados") {
        EmployeesContent(
            state = EmployeesState(
                isLoading = false,
                employees = listOf(admin, admin.copy(id = "old", fullName = employeeName, type = UserType.RECEPTION, area = UserArea.HOUSEKEEPING, active = false)),
                openEmployee = OpenEmployee("admin"),
            ),
            onIntent = {},
            employeeForm = { _, _ -> EmployeeFormContent(state = employeeFormState(), onIntent = {}, onBack = {}) },
        )
    }

    @Test
    fun employeeForm() = checkEveryFrame("empleado") {
        EmployeeFormContent(state = employeeFormState(), onIntent = {}, onBack = {})
    }

    @Test
    fun settings() = checkEveryFrame("configuración") {
        SettingsContent(
            state = SettingsState(
                isLoading = false,
                detail = HotelSettingsDetail(HotelSettings(operationalDayStartMinutes = 360, updatedBy = "admin", updatedAt = now), admin.fullName),
                hourText = "6",
                minuteText = "00",
                showTimeError = true,
                backupAvailable = true,
                lastBackup = BackupSummary(filePath = "/Users/laura/Documentos/Respaldos del hotel/hotel-2026-11-20.json", documentCount = 1_284),
            ),
            onIntent = {},
            onBack = {},
        )
    }

    private val night = shift("night", "admin", "2026-11-20", "22:00", "06:00", area = UserArea.RECEPTION)
    private val morning = shift("morning", "admin", "2026-11-21", "06:00", "14:30")

    @Test
    fun attendance() = checkEveryFrame("asistencia") {
        AttendanceContent(
            state = AttendanceState(isLoading = false, today = CalendarDate(2026, 11, 20), days = listOf(night, morning).byStartDay()),
            onIntent = {},
        )
    }

    @Test
    fun schedules() = checkEveryFrame("horarios") {
        val monday = CalendarDate(2026, 11, 16)
        SchedulesContent(
            state = SchedulesState(
                monday = monday.plusDays(7),
                thisMonday = monday,
                isLoading = false,
                days = weekDays(monday.plusDays(7)).mapIndexed { index, date ->
                    ScheduleDay(date, if (index == 4) listOf(ScheduledShift(night, overlapsAnother = true), ScheduledShift(morning, overlapsAnother = false)) else emptyList())
                },
                employees = listOf(admin),
            ),
            onIntent = {},
            onBack = {},
        )
    }

    @Test
    fun more() = checkEveryFrame("más") {
        MoreScreen(state = MainState(employee = admin, navigation = navigationFor(admin)), onOpenSection = {}, onIntent = {})
    }
}

/** A window of [frame] width and of the height of a phone, with what the screens get from the app. */
@Composable
private fun ScreenFrame(frame: LayoutFrame, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, frame.fontScale)) {
        MajesticTheme(darkTheme = frame.darkTheme) {
            CompositionLocalProvider(
                LocalAppMessages provides rememberAppMessages(),
                LocalWideWindow provides (frame.width >= MajesticSizes.mediumWidth),
            ) {
                Box(modifier = Modifier.testTag(FRAME_TAG).size(frame.width, SCREEN_HEIGHT).background(MajesticTheme.colors.surface)) {
                    content()
                }
            }
        }
    }
}

private val SCREEN_HEIGHT = 760.dp
