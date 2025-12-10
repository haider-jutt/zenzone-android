package com.zenimmersive.android.helper

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.TextView
import com.zenimmersive.android.R

class ProgressDialog(context: Context) : Dialog(context) {

    init {
        // Inflate the layout for the progress dialog
        val inflate = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null)
        setContentView(inflate)

        // Make the dialog not cancellable
        setCancelable(false)

        // Set background to transparent
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    // Function to update the message
    fun setMessage(message: CharSequence?) {
        // Find the TextView in your layout and update its text
        val messageTextView: TextView? = findViewById(R.id.viewMessage)
        messageTextView?.text = message
    }

    companion object {
        fun progressDialog(context: Context): Dialog {
            val dialog = Dialog(context)
            val inflate = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null)
            dialog.setContentView(inflate)
            dialog.setCancelable(false)
            dialog.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )
            return dialog
        }
    }
}
