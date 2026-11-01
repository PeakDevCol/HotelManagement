package com.peakdevcol.project.hotelmanagement.core.ex

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager


/**
 * To validate if the keyboard is active and close it
 */
fun View.dismissKeyboard(completed: () -> Unit = {}) {
    val inputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val wasOpened = inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    if (!wasOpened) completed()
}