package com.peakdevcol.project.hotelmanagement.core.ex

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

fun EditText.onTextChanged(listener: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {

        override fun afterTextChanged(s: Editable?) {
            listener(s.toString())
            //When the user finishes typing
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            //Not yet implemented
            //Before that user types
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            //Not yet implemented
            //As the user types
        }
    })
}


/**
 * With this method can validate when the user presses "NEXT|DONE"
 * - clear focus on the EditText
 * - close KeyBoard
 */
fun EditText.loseFocusAfterAction(action: Int) {
    this.setOnEditorActionListener { v, actionId, _ ->
        if (actionId == action) {
            this.dismissKeyboard()
            v.clearFocus()
        }
        return@setOnEditorActionListener false
    }
}