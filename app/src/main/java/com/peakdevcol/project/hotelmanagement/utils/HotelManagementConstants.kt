package com.peakdevcol.project.hotelmanagement.utils

object HotelManagementConstants {

    const val MIN_PASSWORD_LENGTH = 6

    //Base User data
    const val USER_ID = "userId"
    const val FULL_NAME = "fullName"
    const val EMAIL = "email"
    const val PROVIDER = "provider"
    const val TYPE = "type"
    //Full User sign in
    const val PASSWORD = "password"
    const val PASSWORD_CONFIRMATION = "passwordConfirmation"
    //Type Product
    const val ROOM = "Habitación"
    const val TABLE = "Mesa"
    const val OTHER = "Otro"

    const val USER_COLLECTION = "Users"

    //Orders
    const val ORDER_COLLECTION = "Orders"
    const val ORDER_ID = "id"
    const val ORDER_EMPLOYEE_ID = "idEmployee"
    const val ORDER_PRODUCTS = "products"
    const val ORDER_TYPE = "typeOrder"
    const val ORDER_TYPE_NUMBER = "typeOrderNumber"
    const val ORDER_DATE = "dateOrder"
    const val ORDER_STATE = "state"
    const val ORDER_TOTAL = "totalPayAmount"
    //Order products
    const val PRODUCT_ID = "productId"
    const val PRODUCT_NAME = "productName"
    const val PRODUCT_QUANTITY = "quantity"
    const val PRODUCT_AMOUNT = "unitAmount"
    const val PRODUCT_TOTAL = "totalAmount"

}