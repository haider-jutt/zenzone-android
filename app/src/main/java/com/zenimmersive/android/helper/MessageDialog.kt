package com.zenimmersive.android.helper

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.zenimmersive.android.R

class MessageDialog(context: Context) : Dialog(context) {

    var view: View? = null

    init {
        view = LayoutInflater.from(context).inflate(R.layout.alert_dialog, null)
        // Inflate the layout for the progress dialog
        setContentView(view!!)

        // Make the dialog not cancellable
        setCancelable(false)

        // Set background to transparent
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set the dialog width to match parent
        window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    // Function to update the message
    fun setTitleMessage(message: CharSequence?) {
        // Find the TextView in your layout and update its text
        val messageTextView: TextView? = findViewById(R.id.viewTitle)
        messageTextView?.text = message
    }

    fun setMessage(message: CharSequence?) {
        // Find the TextView in your layout and update its text
        val messageTextView: TextView? = findViewById(R.id.tvExtraMessage)
        messageTextView?.text = message
    }

    fun setPositiveButtonText(text: String?, positiveListener: DialogInterface.OnClickListener) {
        val button: Button? = findViewById(R.id.positiveButton)
        if (text.isNullOrEmpty()) button?.hide()
        else button?.setText(text)

        button?.setOnClickListener {
            positiveListener.onClick(this, DialogInterface.BUTTON_POSITIVE)
        }
    }

    fun setNegativeButtonText(text: String?, negativeListener: DialogInterface.OnClickListener) {
        val button: Button? = findViewById(R.id.negetiveButton)
        if (text.isNullOrEmpty()) button?.hide()
        else button?.setText(text)

        button?.setOnClickListener {
            negativeListener.onClick(this, DialogInterface.BUTTON_NEGATIVE)
        }
    }
}
