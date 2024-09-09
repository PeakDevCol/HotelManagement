package com.peakdevcol.project.hotelmanagement.core.dialog

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BasicDialog {

    companion object {

        /**
         * Remember that the lambda structure is
         * *name of vale*: (a provider/types of parameters that receive) -> value of return
         * and if i need get return value i can
         * val result = *name of vale*.invoke(a provider/types of parameters that receive)
         */
        fun create(
            context: Context,
            background: Drawable,
            title: String,
            msg: String,
            positiveMsg: String,
            clickPositive: (result: DialogInterface) -> Unit
        ): AlertDialog {
            return MaterialAlertDialogBuilder(context)
                .setBackground(background)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(positiveMsg) { dialog, _ ->
                    // dialog in this case is a provider of parameter and its value arrives
                    // in the part where fun create calls it
                    clickPositive.invoke(dialog)
                    // if i need de return value
                    // val result = clickPositive.invoke(dialog)
                }
                .show()

        }

    }
}