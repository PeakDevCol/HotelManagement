package com.peakdevcol.project.hotelmanagement.ui.designsystem.theme

import com.peakdevcol.project.hotelmanagement.domain.model.DestinationKind
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.ic_add
import com.peakdevcol.project.hotelmanagement.resources.ic_add_shopping_cart
import com.peakdevcol.project.hotelmanagement.resources.ic_arrow_back
import com.peakdevcol.project.hotelmanagement.resources.ic_arrow_downward
import com.peakdevcol.project.hotelmanagement.resources.ic_arrow_upward
import com.peakdevcol.project.hotelmanagement.resources.ic_backup
import com.peakdevcol.project.hotelmanagement.resources.ic_badge
import com.peakdevcol.project.hotelmanagement.resources.ic_badge_filled
import com.peakdevcol.project.hotelmanagement.resources.ic_bed
import com.peakdevcol.project.hotelmanagement.resources.ic_bed_filled
import com.peakdevcol.project.hotelmanagement.resources.ic_block
import com.peakdevcol.project.hotelmanagement.resources.ic_calendar_month
import com.peakdevcol.project.hotelmanagement.resources.ic_category
import com.peakdevcol.project.hotelmanagement.resources.ic_check
import com.peakdevcol.project.hotelmanagement.resources.ic_check_circle
import com.peakdevcol.project.hotelmanagement.resources.ic_chevron_left
import com.peakdevcol.project.hotelmanagement.resources.ic_chevron_right
import com.peakdevcol.project.hotelmanagement.resources.ic_close
import com.peakdevcol.project.hotelmanagement.resources.ic_cloud_upload
import com.peakdevcol.project.hotelmanagement.resources.ic_content_copy
import com.peakdevcol.project.hotelmanagement.resources.ic_delete
import com.peakdevcol.project.hotelmanagement.resources.ic_edit
import com.peakdevcol.project.hotelmanagement.resources.ic_edit_note
import com.peakdevcol.project.hotelmanagement.resources.ic_error
import com.peakdevcol.project.hotelmanagement.resources.ic_expand_more
import com.peakdevcol.project.hotelmanagement.resources.ic_filter_list
import com.peakdevcol.project.hotelmanagement.resources.ic_group
import com.peakdevcol.project.hotelmanagement.resources.ic_group_filled
import com.peakdevcol.project.hotelmanagement.resources.ic_info
import com.peakdevcol.project.hotelmanagement.resources.ic_inventory_2
import com.peakdevcol.project.hotelmanagement.resources.ic_inventory_2_filled
import com.peakdevcol.project.hotelmanagement.resources.ic_local_bar
import com.peakdevcol.project.hotelmanagement.resources.ic_local_bar_filled
import com.peakdevcol.project.hotelmanagement.resources.ic_logout
import com.peakdevcol.project.hotelmanagement.resources.ic_mail
import com.peakdevcol.project.hotelmanagement.resources.ic_menu
import com.peakdevcol.project.hotelmanagement.resources.ic_more_vert
import com.peakdevcol.project.hotelmanagement.resources.ic_payments
import com.peakdevcol.project.hotelmanagement.resources.ic_payments_filled
import com.peakdevcol.project.hotelmanagement.resources.ic_percent
import com.peakdevcol.project.hotelmanagement.resources.ic_person
import com.peakdevcol.project.hotelmanagement.resources.ic_point_of_sale
import com.peakdevcol.project.hotelmanagement.resources.ic_pool
import com.peakdevcol.project.hotelmanagement.resources.ic_pool_filled
import com.peakdevcol.project.hotelmanagement.resources.ic_receipt_long
import com.peakdevcol.project.hotelmanagement.resources.ic_receipt_long_filled
import com.peakdevcol.project.hotelmanagement.resources.ic_remove
import com.peakdevcol.project.hotelmanagement.resources.ic_room_service
import com.peakdevcol.project.hotelmanagement.resources.ic_room_service_filled
import com.peakdevcol.project.hotelmanagement.resources.ic_schedule
import com.peakdevcol.project.hotelmanagement.resources.ic_search
import com.peakdevcol.project.hotelmanagement.resources.ic_settings
import com.peakdevcol.project.hotelmanagement.resources.ic_settings_filled
import com.peakdevcol.project.hotelmanagement.resources.ic_table_restaurant
import com.peakdevcol.project.hotelmanagement.resources.ic_table_restaurant_filled
import com.peakdevcol.project.hotelmanagement.resources.ic_timelapse
import com.peakdevcol.project.hotelmanagement.resources.ic_visibility
import com.peakdevcol.project.hotelmanagement.resources.ic_visibility_off
import com.peakdevcol.project.hotelmanagement.resources.ic_warning
import org.jetbrains.compose.resources.DrawableResource

/**
 * Material Symbols Rounded, copied as vectors (Apache 2.0, see `licenses/`). The filled version of
 * an icon only marks the active navigation destination and the chosen order destination.
 */
object MajesticIcons {
    val add = Res.drawable.ic_add
    val addToCart = Res.drawable.ic_add_shopping_cart
    val arrowBack = Res.drawable.ic_arrow_back
    val arrowDown = Res.drawable.ic_arrow_downward
    val arrowUp = Res.drawable.ic_arrow_upward
    val backup = Res.drawable.ic_backup
    val block = Res.drawable.ic_block
    val calendar = Res.drawable.ic_calendar_month
    val category = Res.drawable.ic_category
    val check = Res.drawable.ic_check
    val checkCircle = Res.drawable.ic_check_circle
    val chevronLeft = Res.drawable.ic_chevron_left
    val chevronRight = Res.drawable.ic_chevron_right
    val close = Res.drawable.ic_close
    val cloudUpload = Res.drawable.ic_cloud_upload
    val copy = Res.drawable.ic_content_copy
    val delete = Res.drawable.ic_delete
    val edit = Res.drawable.ic_edit
    val editNote = Res.drawable.ic_edit_note
    val error = Res.drawable.ic_error
    val expandMore = Res.drawable.ic_expand_more
    val filter = Res.drawable.ic_filter_list
    val info = Res.drawable.ic_info
    val logOut = Res.drawable.ic_logout
    val mail = Res.drawable.ic_mail
    val menu = Res.drawable.ic_menu
    val moreOptions = Res.drawable.ic_more_vert
    val percent = Res.drawable.ic_percent
    val person = Res.drawable.ic_person
    val pointOfSale = Res.drawable.ic_point_of_sale
    val receipt = Res.drawable.ic_receipt_long
    val receiptFilled = Res.drawable.ic_receipt_long_filled
    val remove = Res.drawable.ic_remove
    val schedule = Res.drawable.ic_schedule
    val search = Res.drawable.ic_search
    val timelapse = Res.drawable.ic_timelapse
    val visibility = Res.drawable.ic_visibility
    val visibilityOff = Res.drawable.ic_visibility_off
    val warning = Res.drawable.ic_warning

    val payments = Res.drawable.ic_payments
    val paymentsFilled = Res.drawable.ic_payments_filled
    val inventory = Res.drawable.ic_inventory_2
    val inventoryFilled = Res.drawable.ic_inventory_2_filled
    val employees = Res.drawable.ic_group
    val employeesFilled = Res.drawable.ic_group_filled
    val settings = Res.drawable.ic_settings
    val settingsFilled = Res.drawable.ic_settings_filled
    val attendance = Res.drawable.ic_badge
    val attendanceFilled = Res.drawable.ic_badge_filled
    val room = Res.drawable.ic_bed
    val roomFilled = Res.drawable.ic_bed_filled
}

/** The icon of each kind of destination (Mesa, Habitación, Piscina, Bar, Otro). */
fun OrderDestination?.icon(filled: Boolean = false): DrawableResource = when (this) {
    is OrderDestination.Table -> if (filled) Res.drawable.ic_table_restaurant_filled else Res.drawable.ic_table_restaurant
    is OrderDestination.Room -> if (filled) Res.drawable.ic_bed_filled else Res.drawable.ic_bed
    is OrderDestination.Pool -> if (filled) Res.drawable.ic_pool_filled else Res.drawable.ic_pool
    is OrderDestination.Bar -> if (filled) Res.drawable.ic_local_bar_filled else Res.drawable.ic_local_bar
    OrderDestination.Other, null -> if (filled) Res.drawable.ic_room_service_filled else Res.drawable.ic_room_service
}

/** The icon of a kind of destination, as the destination picker offers it. */
fun DestinationKind.icon(filled: Boolean = false): DrawableResource = when (this) {
    DestinationKind.TABLE -> OrderDestination.Table(0).icon(filled)
    DestinationKind.ROOM -> OrderDestination.Room(0).icon(filled)
    DestinationKind.POOL -> OrderDestination.Pool().icon(filled)
    DestinationKind.BAR -> OrderDestination.Bar().icon(filled)
    DestinationKind.OTHER -> OrderDestination.Other.icon(filled)
}
