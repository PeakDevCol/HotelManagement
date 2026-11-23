package com.peakdevcol.project.hotelmanagement.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionCard
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SearchTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MenuAction
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticSwitchRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticOverflowMenu
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.HotelBrandHeader
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.FieldLabel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.RoomStatus
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.AmountRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Avatar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonSize
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.AmountText
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticChip
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticFloatingActionButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticNavigationBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTabs
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIconButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MoneyField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.NavigationDestination
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.NavigationSection
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.OrderCard
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.OrderSummaryBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.PendingToSendBadge
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ProductTile
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ProductTileMinWidth
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.QuantityStepper
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.CountStepper
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.RoomStatusBadge
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.RoomTile
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.RoomTileMinWidth
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ScrollingChipRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SearchField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SegmentedControl
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SideNavigation
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.StatEmphasis
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.StatTileData
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.StatTileRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.StatusBadge
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.icon
import kotlin.test.Test

/**
 * RNF-18: every component of the design system, at the narrowest phone (320 dp), a common phone
 * (360 dp) and a tablet (720 dp), with the text of the device at 100 % and 130 %.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalLayoutApi::class)
class DesignSystemLayoutTest {
    private val frames = listOf(320.dp, 360.dp, 720.dp).flatMap { width -> listOf(1f, 1.3f).map { scale -> LayoutFrame(width, scale) } }

    // A tall window, so that a tall component at 130 % is not cut by the bottom of the window.
    private fun checkEveryFrame(name: String, content: @Composable () -> Unit) = runDesktopComposeUiTest(width = 1024, height = 2400) {
        var frame by mutableStateOf(frames.first())
        setContent { LayoutFrameBox(frame, content) }
        frames.forEach { next ->
            frame = next
            waitForIdle()
            assertNothingSpills("$name a $next")
        }
        listOf(false, true).forEach { dark ->
            frame = LayoutFrame(360.dp, 1f, dark)
            waitForIdle()
            saveSnapshotIfAsked(name, frame)
        }
    }

    @Test
    fun buttons() = checkEveryFrame("botones") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MajesticButton(text = "Confirmar pedido", onClick = {})
                MajesticButton(text = "Agregar productos", onClick = {}, style = ButtonStyle.Tonal, icon = MajesticIcons.add)
                MajesticButton(text = "Reabrir", onClick = {}, style = ButtonStyle.Outline)
                MajesticButton(text = "Cancelar", onClick = {}, style = ButtonStyle.Text)
                MajesticButton(text = "Cancelar pedido", onClick = {}, style = ButtonStyle.Danger)
                MajesticButton(text = "Guardar", onClick = {}, enabled = false)
                MajesticButton(text = "Guardando", onClick = {}, loading = true)
            }
            MajesticButton(text = "Registrar pago · $1.284.000 en efectivo con propina", onClick = {}, size = ButtonSize.Large, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MajesticFloatingActionButton(text = "Nuevo pedido", icon = MajesticIcons.add, onClick = {})
                MajesticFloatingActionButton(text = "Nuevo pedido", icon = MajesticIcons.add, onClick = {}, expanded = false)
                MajesticIconButton(icon = MajesticIcons.moreOptions, contentDescription = "Más opciones", onClick = {})
            }
        }
    }

    @Test
    fun fields() = checkEveryFrame("campos") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MajesticTextField(value = "Limonada de coco con hierbabuena y jengibre", onValueChange = {}, label = "Nombre del producto", help = "Así aparece en el catálogo y en los pedidos.")
            MoneyField(digits = "1250000", onDigitsChange = {}, label = "Efectivo contado", error = "Explica la diferencia en la nota antes de guardar el cierre.")
            SearchField(query = "Cerveza", onQueryChange = {}, placeholder = "Buscar producto")
            SearchField(query = "", onQueryChange = {}, placeholder = "Buscar mesa, habitación o mesero")
        }
    }

    @Test
    fun choices() = checkEveryFrame("selección") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SegmentedControl(options = PaymentMethod.entries, selected = PaymentMethod.CASH, onSelect = {}, label = { if (it == PaymentMethod.CASH) "Efectivo" else "Transferencia" }, modifier = Modifier.fillMaxWidth())
            SegmentedControl(options = listOf("Cierre del día", "Cierre de turno"), selected = null, onSelect = {}, label = { it }, modifier = Modifier.fillMaxWidth())
            ScrollingChipRow {
                MajesticChip(text = "Todos", selected = true, onClick = {}, count = 7)
                MajesticChip(text = "Míos", selected = false, onClick = {}, count = 3)
                MajesticChip(text = "En curso", selected = false, onClick = {}, count = 5)
                MajesticChip(text = "Cerrados", selected = false, onClick = {}, count = 2)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(OrderDestination.Table(4), OrderDestination.Room(204), OrderDestination.Pool(), OrderDestination.Bar(), OrderDestination.Other).forEachIndexed { index, destination ->
                    MajesticChip(text = listOf("Mesa", "Habitación", "Piscina", "Bar", "Otro")[index], selected = index == 0, onClick = {}, icon = destination.icon(), selectedIcon = destination.icon(filled = true))
                }
            }
            QuantityStepper(quantity = 1, onIncrease = {}, onDecrease = {}, onRemove = {})
            QuantityStepper(quantity = 12, onIncrease = {}, onDecrease = {}, onRemove = {}, modifier = Modifier.fillMaxWidth())
            CountStepper(label = "Adultos que se quedan en la habitación", count = 1, onCountChange = {}, minimum = 1, maximum = 20)
            CountStepper(label = "Niños", count = 19, onCountChange = {}, maximum = 19)
        }
    }

    @Test
    fun orders() = checkEveryFrame("pedidos") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OrderState.entries.forEach { StatusBadge(it) }
                PendingToSendBadge()
            }
            OrderCard(
                destination = "Habitación 1204 · cama extra y cuna",
                destinationIcon = OrderDestination.Room(1204).icon(),
                state = OrderState.CLOSED,
                meta = "Ana María Rojas · 14 productos · cerrado por Andrés Gómez",
                age = "Hace 2 h 15 min",
                totalAmount = 1_284_000,
                onClick = {},
                isPendingToSend = true,
                selected = true,
            )
            OrderSummaryBar(detail = "Habitación 1204 · 14 productos sin enviar todavía", totalAmount = 1_284_000, actionText = "Confirmar", onAction = {})
        }
    }

    // RF-HAB-04: every state, a number of four digits, a long type and an inactive room.
    @Test
    fun rooms() = checkEveryFrame("tarjetas de habitación") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RoomStatus.entries.forEach { RoomStatusBadge(it) }
            }
            LazyVerticalGrid(columns = GridCells.Adaptive(RoomTileMinWidth), modifier = Modifier.height(520.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val rooms = listOf(
                    Triple(204, "Doble", RoomStatus.AVAILABLE),
                    Triple(9_999, "Suite presidencial con jacuzzi y vista al mar", RoomStatus.OCCUPIED),
                    Triple(305, "Sencilla", RoomStatus.DIRTY),
                    Triple(1_204, "Familiar", RoomStatus.CLEANING),
                )
                items(rooms) { (number, type, status) ->
                    RoomTile(number = number, typeName = type, status = status, onClick = {}, detail = if (status == RoomStatus.OCCUPIED) "Sale el dom 22 nov" else null)
                }
                item { RoomTile(number = 101, typeName = "Doble", status = RoomStatus.AVAILABLE, onClick = {}, inactive = true) }
            }
        }
    }

    @Test
    fun productGrid() = checkEveryFrame("productos") {
        LazyVerticalGrid(columns = GridCells.Adaptive(ProductTileMinWidth), modifier = Modifier.height(560.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val products = listOf(
                Triple("Cerveza Club Colombia", 7_000L, 2),
                Triple("Limonada de coco", 9_500L, 0),
                Triple("Hamburguesa de la casa con papas a la francesa y ensalada", 32_000L, 1),
                Triple("Agua", 5_000L, 99),
                Triple("Botella de vino reserva especial", 1_250_000L, 0),
            )
            items(products) { (name, price, quantity) ->
                ProductTile(name = name, price = price, quantity = quantity, onAdd = {}, onIncrease = {}, onDecrease = {}, onRemove = {})
            }
        }
    }

    @Test
    fun data() = checkEveryFrame("datos") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTileRow(
                tiles = listOf(
                    StatTileData("Total recibido", "$18.430.500", "1.284 pagos · ventas sin impuestos $15.214.000 · impuestos $2.124.500", StatEmphasis.Brand),
                    StatTileData("Propinas", "$1.092.000", "Aparte de las ventas"),
                    StatTileData("Por cobrar ahora", "$412.000", "9 pedidos abiertos"),
                ),
            )
            ListGroup {
                ListRow(title = "Ana María Rojas Villamizar de la Torre", subtitle = "Administración · Administrador", leading = { Avatar(initials = "AR") }, trailing = { AmountText(amount = 1_284_000) }, onClick = {})
                ListDivider()
                ListRow(title = "Cerveza Club Colombia", subtitle = "Bebidas · Impuesto al consumo 8 %", leading = { Avatar(icon = MajesticIcons.category) }, trailing = { AmountText(amount = 7_000) })
            }
            AmountRow(label = "Total a recibir", amount = 1_250_600, emphasized = true)
            AmountRow(label = "Propina", amount = 4_600)
        }
    }

    @Test
    fun notices() = checkEveryFrame("avisos") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Banner(tone = BannerTone.Warning, title = "1 pedido pendiente de enviar", message = "Se enviará solo cuando vuelva la conexión.", icon = MajesticIcons.cloudUpload)
            Banner(tone = BannerTone.Danger, title = "No se pudo cargar el balance", message = "Revisa la conexión e inténtalo de nuevo.", actionText = "Reintentar", onAction = {})
            Banner(tone = BannerTone.Info, message = "Cada día va de las 6:00 a. m. a las 5:59 a. m., para no partir el turno de la noche.")
            EmptyState(icon = MajesticIcons.receipt, title = "No hay pedidos en curso", message = "Los pedidos que tomes aparecen aquí y en los demás dispositivos al instante.", actionText = "Nuevo pedido", actionIcon = MajesticIcons.add, onAction = {})
        }
    }

    @Test
    fun navigation() = checkEveryFrame("navegación") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MajesticTopBar(title = "Habitación 1204 · pedido de la cama extra", onBack = {}, actions = { MajesticIconButton(icon = MajesticIcons.moreOptions, contentDescription = "Más opciones", onClick = {}) })
            MajesticNavigationBar(
                destinations = listOf(
                    NavigationDestination("orders", "Pedidos", MajesticIcons.receipt, MajesticIcons.receiptFilled, badgeCount = 3),
                    NavigationDestination("cash", "Caja", MajesticIcons.payments, MajesticIcons.paymentsFilled),
                    NavigationDestination("rooms", "Habitaciones", MajesticIcons.room, MajesticIcons.roomFilled),
                    NavigationDestination("more", "Más", MajesticIcons.menu, MajesticIcons.menu),
                ),
                selected = "cash",
                onSelect = {},
            )
            // Decision 2A of M11: the bar of reception, the longest one, with four sections and "Más".
            MajesticNavigationBar(
                destinations = listOf(
                    NavigationDestination("rooms", "Habitaciones", MajesticIcons.room, MajesticIcons.roomFilled),
                    NavigationDestination("reservations", "Reservas", MajesticIcons.reservation, MajesticIcons.reservationFilled),
                    NavigationDestination("orders", "Pedidos", MajesticIcons.receipt, MajesticIcons.receiptFilled, badgeCount = 12),
                    NavigationDestination("attendance", "Asistencia", MajesticIcons.attendance, MajesticIcons.attendanceFilled),
                    NavigationDestination("more", "Más", MajesticIcons.menu, MajesticIcons.menu),
                ),
                selected = "reservations",
                onSelect = {},
            )
            MajesticTabs(tabs = listOf("Por día", "Por método", "Por empleado", "Por producto"), selectedIndex = 0, onSelect = {})
        }
    }

    @Test
    fun sideNavigation() = checkEveryFrame("navegación lateral") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val sections = listOf(
                NavigationSection(
                    "Operación",
                    listOf(
                        NavigationDestination("orders", "Pedidos", MajesticIcons.receipt, MajesticIcons.receiptFilled, badgeCount = 2),
                        NavigationDestination("rooms", "Habitaciones", MajesticIcons.room, MajesticIcons.roomFilled),
                    ),
                ),
                NavigationSection(
                    "Administración",
                    listOf(
                        NavigationDestination("cash", "Caja y balance", MajesticIcons.payments, MajesticIcons.paymentsFilled),
                        NavigationDestination("inventory", "Inventario", MajesticIcons.inventory, MajesticIcons.inventoryFilled),
                        NavigationDestination("employees", "Empleados", MajesticIcons.employees, MajesticIcons.employeesFilled),
                        NavigationDestination("settings", "Configuración", MajesticIcons.settings, MajesticIcons.settingsFilled),
                    ),
                ),
            )
            SideNavigation(sections = sections, selected = "orders", onSelect = {}, employeeName = "Laura Méndez Restrepo de la Cruz", employeeArea = "Administración", onLogOut = {}, modifier = Modifier.height(420.dp))
            SideNavigation(sections = sections, selected = "cash", onSelect = {}, employeeName = "Laura Méndez", employeeArea = "Administración", onLogOut = {}, modifier = Modifier.height(420.dp), compact = true)
        }
    }

    @Test
    fun blocks() = checkEveryFrame("bloques") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HotelBrandHeader()
            SectionCard(
                title = "Inicio del día operativo de la recepción y del restaurante",
                help = "Las ventas de la madrugada cuentan para el día anterior hasta esta hora.",
            ) {
                FieldLabel(text = "Método de pago con el que el huésped canceló la cuenta")
                MajesticSwitchRow(title = "Activo", help = "Un producto inactivo no aparece al crear pedidos, pero se conserva.", checked = true, onCheckedChange = {})
                MajesticSwitchRow(title = "No puedes cambiar tu propio acceso de administrador", checked = false, onCheckedChange = {}, enabled = false)
            }
            SearchTopBar(
                query = "Habitación 1204 de Ana María",
                onQueryChange = {},
                placeholder = "Número de destino o mesero",
                closeDescription = "Cerrar la búsqueda",
                onClose = {},
            )
            MajesticTopBar(title = "Habitación 1204 · pedido de la cama extra") {
                MajesticOverflowMenu(actions = listOf(MenuAction(text = "Cancelar pedido", icon = MajesticIcons.block, onClick = {}, destructive = true)))
            }
            Box(modifier = Modifier.height(96.dp)) { MajesticLoading() }
        }
    }
}
