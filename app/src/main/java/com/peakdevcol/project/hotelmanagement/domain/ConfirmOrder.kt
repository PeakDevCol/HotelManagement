package com.peakdevcol.project.hotelmanagement.domain

import java.util.UUID

data class ConfirmOrder(
    val id: String = UUID.randomUUID().toString(),
    val idEmployee: String,
    val listProduct: Product,
    val typeOrder: ProviderTypeOrder,
    val dateOrder: String,
    val state: OrderState,
    val totalPayAmount: String,
)
// Puedo crear una metodo que reciva las lo necesario para confirmar la orde
// y que asigne la fecha de hoy y retorne la orden completa