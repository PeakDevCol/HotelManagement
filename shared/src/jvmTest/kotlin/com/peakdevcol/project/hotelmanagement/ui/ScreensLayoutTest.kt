package com.peakdevcol.project.hotelmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceData
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.AttendancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.AuthorizedDevice
import com.peakdevcol.project.hotelmanagement.domain.model.BackupSummary
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceFilter
import com.peakdevcol.project.hotelmanagement.domain.model.BalancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceReport
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceSummary
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceTotals
import com.peakdevcol.project.hotelmanagement.domain.model.BoardRoom
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosing
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingDraft
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingOverview
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingType
import com.peakdevcol.project.hotelmanagement.domain.model.CatalogGroup
import com.peakdevcol.project.hotelmanagement.domain.model.Category
import com.peakdevcol.project.hotelmanagement.domain.model.Correction
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionError
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionKind
import com.peakdevcol.project.hotelmanagement.domain.model.DaySales
import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeSales
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettingsDetail
import com.peakdevcol.project.hotelmanagement.domain.model.Housekeeping
import com.peakdevcol.project.hotelmanagement.domain.model.Inventory
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryProduct
import com.peakdevcol.project.hotelmanagement.domain.model.Mark
import com.peakdevcol.project.hotelmanagement.domain.model.MarkDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkOutcome
import com.peakdevcol.project.hotelmanagement.domain.model.MarkSource
import com.peakdevcol.project.hotelmanagement.domain.model.MethodSales
import com.peakdevcol.project.hotelmanagement.domain.model.Occupancy
import com.peakdevcol.project.hotelmanagement.domain.model.OpenEntry
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
import com.peakdevcol.project.hotelmanagement.domain.model.ReportError
import com.peakdevcol.project.hotelmanagement.domain.model.Room
import com.peakdevcol.project.hotelmanagement.domain.model.RoomBoard
import com.peakdevcol.project.hotelmanagement.domain.model.RoomError
import com.peakdevcol.project.hotelmanagement.domain.model.RoomStatus
import com.peakdevcol.project.hotelmanagement.domain.model.RoomType
import com.peakdevcol.project.hotelmanagement.domain.model.RoomTypeCatalog
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.model.byStartDay
import com.peakdevcol.project.hotelmanagement.domain.model.navigationFor
import com.peakdevcol.project.hotelmanagement.domain.model.report
import com.peakdevcol.project.hotelmanagement.domain.model.weekDays
import com.peakdevcol.project.hotelmanagement.domain.usecase.ScheduleDay
import com.peakdevcol.project.hotelmanagement.domain.usecase.ScheduledShift
import com.peakdevcol.project.hotelmanagement.domain.usecase.WorkingEntry
import com.peakdevcol.project.hotelmanagement.fakes.bogotaInstant
import com.peakdevcol.project.hotelmanagement.fakes.bogotaRecord
import com.peakdevcol.project.hotelmanagement.fakes.order
import com.peakdevcol.project.hotelmanagement.fakes.phoneMark
import com.peakdevcol.project.hotelmanagement.fakes.shift
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderContent
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderState
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddProductsContent
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddProductsState
import com.peakdevcol.project.hotelmanagement.ui.addorder.ProductPickerState
import com.peakdevcol.project.hotelmanagement.ui.attendance.AttendanceContent
import com.peakdevcol.project.hotelmanagement.ui.attendance.AttendanceState
import com.peakdevcol.project.hotelmanagement.ui.attendance.MarkResult
import com.peakdevcol.project.hotelmanagement.ui.attendance.PhoneMarking
import com.peakdevcol.project.hotelmanagement.ui.attendance.ReceptionMarkingState
import com.peakdevcol.project.hotelmanagement.ui.attendance.ReceptionResult
import com.peakdevcol.project.hotelmanagement.ui.attendance.WhoIsWorkingState
import com.peakdevcol.project.hotelmanagement.ui.attendancereview.AttendanceReviewContent
import com.peakdevcol.project.hotelmanagement.ui.attendancereview.AttendanceReviewState
import com.peakdevcol.project.hotelmanagement.ui.attendancereview.EditorForm
import com.peakdevcol.project.hotelmanagement.ui.attendancereview.EmployeeAttendanceContent
import com.peakdevcol.project.hotelmanagement.ui.attendancereview.RecordEditor
import com.peakdevcol.project.hotelmanagement.ui.attendancereview.RecordEditorFields
import com.peakdevcol.project.hotelmanagement.ui.attendancereview.ReviewTab
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
import com.peakdevcol.project.hotelmanagement.ui.devices.DevicesContent
import com.peakdevcol.project.hotelmanagement.ui.devices.DevicesState
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeeFormContent
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeeFormState
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeesContent
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeesState
import com.peakdevcol.project.hotelmanagement.ui.employees.OpenEmployee
import com.peakdevcol.project.hotelmanagement.ui.hotelzone.HotelZoneContent
import com.peakdevcol.project.hotelmanagement.ui.hotelzone.HotelZoneState
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
import com.peakdevcol.project.hotelmanagement.ui.main.DaysRangePicker
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import com.peakdevcol.project.hotelmanagement.ui.main.LocalWideWindow
import com.peakdevcol.project.hotelmanagement.ui.main.MainState
import com.peakdevcol.project.hotelmanagement.ui.main.MoreScreen
import com.peakdevcol.project.hotelmanagement.ui.main.rememberAppMessages
import com.peakdevcol.project.hotelmanagement.ui.main.rememberDaysRangePickerState
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomDetailFields
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomForm
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomFormFields
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomTypeForm
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomTypeFormFields
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomTypesContent
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomTypesState
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomsContent
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomsState
import com.peakdevcol.project.hotelmanagement.ui.schedules.SchedulesContent
import com.peakdevcol.project.hotelmanagement.ui.schedules.SchedulesState
import com.peakdevcol.project.hotelmanagement.ui.settings.SettingsContent
import com.peakdevcol.project.hotelmanagement.ui.settings.SettingsState
import com.peakdevcol.project.hotelmanagement.domain.model.AvailableRoom
import com.peakdevcol.project.hotelmanagement.domain.model.DocumentType
import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.GuestError
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSource
import com.peakdevcol.project.hotelmanagement.domain.model.Stay
import com.peakdevcol.project.hotelmanagement.ui.reservations.GuestForm
import com.peakdevcol.project.hotelmanagement.ui.reservations.GuestFormFields
import com.peakdevcol.project.hotelmanagement.ui.reservations.GuestSearch
import com.peakdevcol.project.hotelmanagement.ui.reservations.NewReservationContent
import com.peakdevcol.project.hotelmanagement.ui.reservations.NewReservationState
import com.peakdevcol.project.hotelmanagement.ui.reservations.ReservationDetailFields
import com.peakdevcol.project.hotelmanagement.ui.reservations.ReservationsContent
import com.peakdevcol.project.hotelmanagement.ui.reservations.ReservationsState
import com.peakdevcol.project.hotelmanagement.ui.reservations.RoomSearch
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * RNF-18 on whole screens: with long names and large amounts, at the narrowest phone (320 dp), a
 * common phone (360 dp), a tablet (720 dp) and a desktop window (1024 dp, where lists show their
 * detail beside them), with the text of the device at 100 % and 130 %.
 */
@OptIn(ExperimentalTestApi::class)
class ScreensLayoutTest {
    private val frames = listOf(320.dp, 360.dp, 720.dp, 1024.dp).flatMap { width -> listOf(1f, 1.3f).map { scale -> LayoutFrame(width, scale) } }

    private val snapshots = listOf(LayoutFrame(360.dp, 1f), LayoutFrame(360.dp, 1f, darkTheme = true), LayoutFrame(1024.dp, 1f))

    // Material fixes its date pickers at 360 dp on every platform, whatever the width of the window.
    private val datePickerFrames = listOf(1f, 1.3f).map { scale -> LayoutFrame(360.dp, scale) }
    private val datePickerSnapshots = listOf(LayoutFrame(360.dp, 1f), LayoutFrame(360.dp, 1f, darkTheme = true), LayoutFrame(360.dp, 1.3f))

    private fun checkEveryFrame(
        name: String,
        frames: List<LayoutFrame> = this.frames,
        snapshots: List<LayoutFrame> = this.snapshots,
        checkSplitWords: Boolean = false,
        content: @Composable () -> Unit,
    ) = runDesktopComposeUiTest(width = 1100, height = 900) {
        var frame by mutableStateOf(frames.first())
        setContent { ScreenFrame(frame, content) }
        frames.forEach { next ->
            frame = next
            waitForIdle()
            assertNothingSpills("$name a $next", checkSplitWords)
        }
        snapshots.forEach { snapshot ->
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
    fun daysRange() = checkEveryFrame("elegir periodo", datePickerFrames, datePickerSnapshots, checkSplitWords = true) {
        DaysRangePicker(state = rememberDaysRangePickerState(CalendarDate(2026, 11, 1), CalendarDate(2026, 11, 30)))
    }

    @Test
    fun daysRangeAcrossYears() = checkEveryFrame("elegir periodo entre dos años", datePickerFrames, datePickerSnapshots, checkSplitWords = true) {
        DaysRangePicker(state = rememberDaysRangePickerState(CalendarDate(2025, 12, 28), CalendarDate(2026, 1, 13)))
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
                detail = HotelSettingsDetail(
                    HotelSettings(operationalDayStartMinutes = 360, hotelLatitude = 4.60971, hotelLongitude = -74.08175, zoneRadiusMeters = 2_000, updatedBy = "admin", updatedAt = now),
                    admin.fullName,
                ),
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

    private val scheduleState = AttendanceState(isLoading = false, today = CalendarDate(2026, 11, 20), days = listOf(night, morning).byStartDay())
    private val noReception = ReceptionMarkingState(isChecking = false)

    // An entry pending and under review, and a mark that failed far from the hotel.
    private val openEntry = AttendanceRecord(
        id = "a-1",
        employeeId = "admin",
        clockIn = Mark(at = null, deviceTime = Instant.parse("2026-11-19T11:02:00Z"), source = MarkSource.PHONE, distanceMeters = 40, accuracyMeters = 18),
    )

    // RF-ASI-09: two employees working, one of them with a mark to confirm.
    private val working = WhoIsWorkingState(
        isVisible = true,
        now = Instant.parse("2026-11-19T22:47:00Z"),
        entries = listOf(
            WorkingEntry(OpenEntry(openEntry.copy(employeeId = "ana"), forgottenAt = Instant.parse("2026-11-20T03:02:00Z")), employeeName),
            WorkingEntry(
                OpenEntry(openEntry.copy(id = "a-2", clockIn = openEntry.clockIn.copy(at = Instant.parse("2026-11-19T13:30:00Z"))), forgottenAt = Instant.parse("2026-11-20T03:02:00Z")),
                admin.fullName,
            ),
        ),
    )

    @Test
    fun attendance() = checkEveryFrame("asistencia") {
        AttendanceContent(
            state = scheduleState.copy(
                marking = PhoneMarking(
                    now = Instant.parse("2026-11-19T22:47:00Z"),
                    isLoading = false,
                    openEntry = OpenEntry(openEntry, forgottenAt = Instant.parse("2026-11-20T03:02:00Z")),
                    result = MarkResult.Failed(MarkKind.EXIT, AttendanceError.OutsideZone(1_850)),
                ),
            ),
            onIntent = {},
            reception = noReception,
            onReceptionIntent = {},
            working = working,
        )
    }

    @Test
    fun attendanceNobodyWorking() = checkEveryFrame("asistencia sin nadie en turno") {
        AttendanceContent(state = scheduleState, onIntent = {}, reception = noReception, onReceptionIntent = {}, working = WhoIsWorkingState(isVisible = true, loadFailed = true))
    }

    @Test
    fun attendanceOffline() = checkEveryFrame("asistencia sin conexión") {
        AttendanceContent(
            state = scheduleState.copy(
                marking = PhoneMarking(
                    now = Instant.parse("2026-11-20T12:00:00Z"),
                    isLoading = false,
                    openEntry = OpenEntry(openEntry, forgottenAt = Instant.parse("2026-11-20T03:02:00Z")),
                    result = MarkResult.Done(MarkOutcome(MarkKind.EXIT, MarkDelivery.PENDING, now, workedTime = 11.hours + 58.minutes)),
                ),
            ),
            onIntent = {},
            reception = noReception,
            onReceptionIntent = {},
            onBack = {},
        )
    }

    @Test
    fun receptionMarking() = checkEveryFrame("marcación en recepción") {
        AttendanceContent(
            state = scheduleState,
            onIntent = {},
            reception = ReceptionMarkingState(
                isAvailable = true,
                isChecking = false,
                emailText = admin.email,
                result = ReceptionResult.Done(MarkOutcome(MarkKind.EXIT, MarkDelivery.SENT, now, workedTime = 8.hours + 3.minutes, employeeName = admin.fullName)),
            ),
            onReceptionIntent = {},
        )
    }

    @Test
    fun attendanceWithoutMarking() = checkEveryFrame("asistencia sin marcación") {
        AttendanceContent(state = scheduleState, onIntent = {}, reception = noReception, onReceptionIntent = {})
    }

    @Test
    fun hotelZone() = checkEveryFrame("zona del hotel") {
        HotelZoneContent(
            state = HotelZoneState(
                isLoading = false,
                latitudeText = "4.609710123456789",
                longitudeText = "-74.081750123456789",
                radiusText = "2000",
                accuracyText = "500",
                showError = true,
                canUseMyLocation = true,
                isLocating = true,
            ),
            onIntent = {},
            onBack = {},
        )
    }

    @Test
    fun devices() = checkEveryFrame("computadores autorizados") {
        DevicesContent(
            state = DevicesState(
                isLoading = false,
                devices = listOf(
                    AuthorizedDevice("pc-1", "Recepción principal del primer piso junto al ascensor", "admin", authorizedAt = now),
                    AuthorizedDevice("pc-2", "Bar de la piscina", "admin", authorizedAt = now),
                ),
                thisDeviceId = "pc-3",
                canAuthorizeThisDevice = true,
                nameText = "Recepción del segundo piso, al lado de la escalera",
            ),
            onIntent = {},
            onBack = {},
        )
    }

    @Test
    fun devicesThisOneAuthorized() = checkEveryFrame("este computador autorizado") {
        DevicesContent(
            state = DevicesState(
                isLoading = false,
                devices = listOf(AuthorizedDevice("pc-1", "Recepción principal del primer piso junto al ascensor", "admin", authorizedAt = now)),
                thisDeviceId = "pc-1",
                canAuthorizeThisDevice = true,
            ),
            onIntent = {},
            onBack = {},
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

    // ---- Marcaciones (RF-ASI-05 to RF-ASI-08), long on purpose

    private fun bogota(text: String) = Instant.parse(bogotaInstant(text))

    private val reviewNow = bogota("2026-11-21 10:00")
    private val luis = User("luis", "Luis Fernando Pérez Gómez de los Santos", "luis.perez@hoteljuanmajestic.com", UserType.BASIC, UserArea.SERVICE, active = true)
    private val ana = User("ana", employeeName, "ana.rojas@hoteljuanmajestic.com", UserType.BASIC, UserArea.HOUSEKEEPING, active = true)
    private val fortnight = AttendancePeriod(CalendarDate(2026, 11, 16), CalendarDate(2026, 11, 30))

    // The entry was moved by hand; the exit, marked at reception, waits to be confirmed.
    private val corrected = AttendanceRecord(
        id = "r-1",
        employeeId = "ana",
        clockIn = phoneMark(at = bogotaInstant("2026-11-20 09:15"), deviceTime = bogotaInstant("2026-11-20 07:02"), distanceMeters = 1_850),
        clockOut = Mark(at = bogota("2026-11-20 16:30"), deviceTime = bogota("2026-11-20 14:05"), source = MarkSource.KIOSK, deviceId = "pc-1"),
        corrections = listOf(
            Correction(
                id = "c-1",
                kind = CorrectionKind.OTHER_TIME,
                mark = MarkKind.ENTRY,
                time = bogota("2026-11-20 06:58"),
                reason = "Llegó con el turno de la mañana y marcó cuando subió del parqueadero, sin señal en el sótano",
                by = "admin",
                at = bogota("2026-11-21 08:30"),
            ),
        ),
    )
    private val forgottenExit = bogotaRecord("r-2", "luis", "2026-11-19 06:00")
    private val reviewData = AttendanceData(
        period = fortnight,
        shifts = listOf(
            shift("ana-fri", "ana", "2026-11-20", "06:00", "14:00"),
            shift("ana-wed", "ana", "2026-11-18", "06:00", "14:00"),
            shift("ana-tue", "ana", "2026-11-17", "06:00", "14:00"),
            shift("ana-night", "ana", "2026-11-21", "22:00", "06:00"),
            shift("luis-thu", "luis", "2026-11-19", "06:00", "14:00"),
        ),
        records = listOf(
            corrected,
            forgottenExit,
            bogotaRecord("r-3", "ana", "2026-11-17 06:47", "2026-11-17 14:00"),
            bogotaRecord("r-4", "ana", "2026-11-19 15:00", "2026-11-19 23:30"),
        ),
        lateToleranceMinutes = 10,
        maxOpenEntryHours = 16,
    )
    private val reviewState = AttendanceReviewState(
        today = CalendarDate(2026, 11, 21),
        now = reviewNow,
        period = fortnight,
        isLoading = false,
        data = reviewData,
        loadedAt = reviewNow,
        report = reviewData.report(reviewNow),
        openEntries = listOf(forgottenExit),
        pendingExits = listOf(forgottenExit),
        employees = listOf(ana, luis, admin),
    )

    @Test
    fun attendanceReview() = checkEveryFrame("marcaciones por revisar") {
        AttendanceReviewContent(state = reviewState, onIntent = {}, onBack = {})
    }

    @Test
    fun attendanceReviewHours() = checkEveryFrame("marcaciones horas") {
        AttendanceReviewContent(state = reviewState.copy(tab = ReviewTab.HOURS, loadError = ReportError.Network), onIntent = {})
    }

    @Test
    fun employeeAttendance() = checkEveryFrame("horas de un empleado") {
        EmployeeAttendanceContent(state = reviewState, employeeId = "ana", onIntent = {}, onBack = {})
    }

    @Test
    fun recordEditor() = checkEveryFrame("marcación por corregir") {
        EditorFrame {
            RecordEditorFields(editor = RecordEditor("ana", corrected, reviewData.shifts.first()), state = reviewState, onIntent = {})
        }
    }

    @Test
    fun recordEditorForm() = checkEveryFrame("otra hora con motivo") {
        EditorFrame {
            RecordEditorFields(
                editor = RecordEditor(
                    employeeId = "luis",
                    record = forgottenExit,
                    shift = reviewData.shifts.last(),
                    form = EditorForm.MarkTime(MarkKind.EXIT, "1400", reasonText = "Se le olvidó marcar la salida porque salió por la puerta del parqueadero de proveedores"),
                    error = CorrectionError.Changed,
                ),
                state = reviewState,
                onIntent = {},
            )
        }
    }

    @Test
    fun registrationForm() = checkEveryFrame("registrar entrada y salida") {
        EditorFrame {
            RecordEditorFields(
                editor = RecordEditor(
                    employeeId = "ana",
                    record = null,
                    shift = reviewData.shifts[1],
                    form = EditorForm.Registration("06:00", "05:30"),
                    error = CorrectionError.TooLong,
                ),
                state = reviewState,
                onIntent = {},
            )
        }
    }

    /** The inside of a sheet, as it is drawn on the phone: it scrolls when it is long. */
    @Composable
    private fun EditorFrame(content: @Composable () -> Unit) {
        Column(
            modifier = Modifier.fillMaxWidth().background(MajesticTheme.colors.surfaceRaised).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }

    // ---- Habitaciones (RF-HAB-01 to RF-HAB-04), long on purpose

    private val doubleType = RoomType("double", "Doble", capacity = 2, baseRate = 250_000)
    private val suiteType = RoomType("suite", "Suite presidencial con jacuzzi y vista al mar", capacity = 12, baseRate = 1_284_000, taxId = "consumption")
    private val oldType = RoomType("old", "Sencilla antigua del primer piso junto a la recepción", capacity = 1, baseRate = 90_000, taxId = "vat", active = false)
    private val boardRooms = listOf(
        BoardRoom(Room("r-101", 101, "double", floor = 1), doubleType),
        BoardRoom(
            Room("r-102", 102, "suite", floor = 1, housekeeping = Housekeeping.DIRTY, housekeepingChangedBy = "ana", housekeepingChangedAt = now),
            suiteType,
            housekeepingChangedByName = employeeName,
        ),
        BoardRoom(Room("r-1204", 1_204, "suite", floor = 12, occupancy = Occupancy.OCCUPIED, expectedCheckOutDate = CalendarDate(2026, 11, 22)), suiteType),
        BoardRoom(Room("r-9999", 9_999, "gone", floor = 99, housekeeping = Housekeeping.CLEANING), type = null),
    )
    private val roomBoard = RoomBoard(
        rooms = boardRooms,
        inactiveRooms = listOf(BoardRoom(Room("r-305", 305, "old", floor = 3, active = false), oldType)),
        types = listOf(doubleType, oldType, suiteType),
        canManage = true,
    )
    private val roomsState = RoomsState(isLoading = false, board = roomBoard)
    private val roomTypeCatalog = RoomTypeCatalog(types = listOf(doubleType, oldType, suiteType), taxes = taxes)

    @Test
    fun rooms() = checkEveryFrame("habitaciones") {
        RoomsContent(state = roomsState, onIntent = {}, onBack = {})
    }

    @Test
    fun roomsOfAnEmployee() = checkEveryFrame("habitaciones de un empleado") {
        RoomsContent(state = RoomsState(isLoading = false, board = roomBoard.copy(inactiveRooms = emptyList(), canManage = false), filter = RoomStatus.OCCUPIED), onIntent = {})
    }

    @Test
    fun roomsWithoutTypes() = checkEveryFrame("habitaciones sin tipos") {
        RoomsContent(state = RoomsState(isLoading = false, board = RoomBoard(canManage = true)), onIntent = {})
    }

    @Test
    fun roomDetail() = checkEveryFrame("habitación por limpiar") {
        EditorFrame { RoomDetailFields(room = boardRooms[1], state = roomsState.copy(markingRoomId = "r-102"), onIntent = {}) }
    }

    @Test
    fun roomDetailOccupied() = checkEveryFrame("habitación ocupada") {
        EditorFrame { RoomDetailFields(room = boardRooms[2], state = roomsState, onIntent = {}) }
    }

    @Test
    fun roomForm() = checkEveryFrame("datos de la habitación") {
        val form = RoomForm(roomId = "r-305", numberText = "9999", floorText = "99", roomTypeId = "old", active = false, error = RoomError.DuplicateNumber)
        EditorFrame { RoomFormFields(form = form, state = roomsState.copy(form = form), onIntent = {}) }
    }

    @Test
    fun roomTypes() = checkEveryFrame("tipos de habitación") {
        RoomTypesContent(state = RoomTypesState(isLoading = false, catalog = roomTypeCatalog), onIntent = {}, onBack = {})
    }

    @Test
    fun roomTypeForm() = checkEveryFrame("tipo de habitación") {
        val form = RoomTypeForm(typeId = "old", name = oldType.name, capacityText = "12", rateDigits = "1284000", taxId = "vat", active = false, error = RoomError.DuplicateTypeName)
        EditorFrame { RoomTypeFormFields(form = form, state = RoomTypesState(isLoading = false, catalog = roomTypeCatalog, form = form), onIntent = {}) }
    }

    // ---- Reservations (M11), with long names, large amounts and the longest dates

    private val longGuest = Guest(
        id = "g-ana",
        fullName = "Ana María Rojas Restrepo de la Cruz",
        phone = "+57 310 555 1234 ext. 108",
        documentType = DocumentType.PASSPORT,
        documentNumber = "AB1234567890",
    )
    private val suite = RoomType("suite", "Suite presidencial con jacuzzi y vista al mar", capacity = 12, baseRate = 1_284_000, taxId = "vat")
    private val freeRooms = listOf(
        AvailableRoom(Room("r-1204", 1204, "suite", floor = 12), suite, nightlyRate = 1_284_000, taxRate = 1_900),
        AvailableRoom(Room("r-9999", 9999, "suite", floor = 99), suite, nightlyRate = 1_284_000, taxRate = 1_900),
    )
    private val longStay = Stay(CalendarDate(2026, 12, 28), CalendarDate(2027, 1, 27))
    private val reservationsList = listOf(
        Reservation(
            id = "r1",
            guestId = "g-ana",
            guestName = longGuest.fullName,
            roomId = "r-1204",
            roomNumber = 1204,
            stay = longStay,
            adults = 12,
            children = 8,
            nightlyRates = List(longStay.nightCount) { 1_284_000L },
            taxAmount = 6_150_252,
            source = ReservationSource.WHATSAPP,
            notes = "Llega en el último vuelo de la noche; pidió cuna, cama extra, almohadas sin plumas y que la habitación quede lejos del ascensor y de la piscina.",
        ),
        Reservation(
            id = "r2",
            guestId = "g-luis",
            guestName = "Luis",
            roomId = "r-9999",
            roomNumber = 9999,
            stay = Stay(CalendarDate(2026, 12, 30), CalendarDate(2026, 12, 31)),
            adults = 1,
            children = 0,
            nightlyRates = listOf(250_000),
            taxAmount = 0,
            source = ReservationSource.DIRECT,
        ),
    )

    @Test
    fun reservations() = checkEveryFrame("reservas") {
        ReservationsContent(state = ReservationsState(isLoading = false, reservations = reservationsList, query = "Ana María Rojas Restrepo de la Cruz"), onIntent = {}, onBack = {})
    }

    @Test
    fun reservationsEmpty() = checkEveryFrame("reservas vacías") {
        ReservationsContent(state = ReservationsState(isLoading = false), onIntent = {})
    }

    @Test
    fun reservationDetail() = checkEveryFrame("detalle de la reserva") {
        EditorFrame { ReservationDetailFields(reservationsList.first()) }
    }

    @Test
    fun newReservationOfAnAdministrator() = checkEveryFrame("nueva reserva") {
        NewReservationContent(
            state = NewReservationState(
                isStarting = false,
                today = CalendarDate(2026, 12, 28),
                canChangeRate = true,
                stay = longStay,
                adults = 12,
                children = 8,
                roomSearch = RoomSearch.Found(freeRooms),
                roomId = "r-1204",
                guest = longGuest,
                source = ReservationSource.WHATSAPP,
                rateText = "1284000",
                notes = reservationsList.first().notes,
                error = ReservationError.NightsTaken(listOf(CalendarDate(2026, 12, 31), CalendarDate(2027, 1, 1))),
            ),
            onIntent = {},
            onBack = {},
        )
    }

    @Test
    fun newReservationSearchingTheGuest() = checkEveryFrame("nueva reserva buscando huésped") {
        NewReservationContent(
            state = NewReservationState(
                isStarting = false,
                today = CalendarDate(2026, 12, 28),
                stay = longStay,
                roomSearch = RoomSearch.Found(emptyList()),
                guestQuery = "Ana María Rojas Restrepo de la Cruz",
                guestSearch = GuestSearch.Found(listOf(longGuest, longGuest.copy(id = "g-2", documentType = null, documentNumber = null))),
            ),
            onIntent = {},
            onBack = {},
        )
    }

    @Test
    fun newReservationWithoutDays() = checkEveryFrame("nueva reserva sin fechas") {
        NewReservationContent(
            state = NewReservationState(isStarting = false, today = CalendarDate(2026, 12, 28), guestSearch = GuestSearch.Failed(GuestError.NoConnection)),
            onIntent = {},
            onBack = {},
        )
    }

    @Test
    fun guestForm() = checkEveryFrame("datos del huésped") {
        val form = GuestForm(
            existing = longGuest,
            fullName = longGuest.fullName,
            phone = longGuest.phone,
            documentType = DocumentType.PASSPORT,
            documentNumber = "AB1234567890",
            nationality = "República Bolivariana de Venezuela",
            email = "ana.maria.rojas.restrepo.de.la.cruz@correo-electronico.com",
            error = GuestError.DuplicateDocument,
        )
        EditorFrame { GuestFormFields(form = form, isSaving = true, onIntent = {}) }
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
