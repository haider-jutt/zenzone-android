package com.zenimmersive.android.helper

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText

class FocusClickUtil {
    companion object {
        fun setupUI(view: View) {
            // Set up touch listener for non-text box views to hide keyboard.

            if (view !is EditText && view !is Button) {
                view.isClickable = true
                view.isFocusable = true
            }

            //If a layout container, iterate over children and seed recursion.
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val innerView = view.getChildAt(i)
                    setupUI(innerView)
                }
            }
        }
    }

}
