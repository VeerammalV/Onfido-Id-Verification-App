package com.id.verification

import android.app.AlertDialog
import android.content.Context

object Utility {

    fun dialogPermissionDenied(context: Context, cancelable: Boolean, callback: AlertCallback) {
        AlertDialog.Builder(context)
            .setTitle("Permissions Required")
            .setMessage("Allow necessary permissions to continue")
            .setCancelable(false)
            .setPositiveButton("Settings") { dialog, _ ->
                callback.response(true)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                callback.response(false)
                dialog.dismiss()
            }
            .show()
    }


    fun exitApp(context: Context, cancelable: Boolean, callback: AlertCallback) {
        AlertDialog.Builder(context)
            .setTitle("Process Completed")
            .setMessage("Confirm Exit")
            .setCancelable(cancelable)
            .setPositiveButton("OK") { dialog, _ ->
                callback.response(true)
                dialog.dismiss()
            }
            .show()
    }

}