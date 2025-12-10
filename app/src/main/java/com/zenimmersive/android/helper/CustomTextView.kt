package com.zenimmersive.android.helper

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.zenimmersive.android.R

class CustomTextView : AppCompatTextView {


    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        attrs?.let { setCustomFont(context, it) }
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        attrs?.let { setCustomFont(context, it) }
    }

    private fun setCustomFont(ctx: Context, attrs: AttributeSet) {
        val a = ctx.obtainStyledAttributes(attrs, R.styleable.FontView)
        val customFont = a.getString(R.styleable.FontView_customFont)
        val style = a.getString(R.styleable.FontView_textStyle);
        if (customFont != null) {
            if (style != null && style.equals("bold")) {
                setCustomFont(ctx, customFont, Typeface.BOLD)
            } else {
                setCustomFont(ctx, customFont)
            }
            a.recycle()
        }
    }

    fun setCustomFont(ctx: Context, asset: String?, style: Int = Typeface.NORMAL): Boolean {
        val tf: Typeface?
        try {
            tf = Typeface.create(Typeface.createFromAsset(ctx.assets, "fonts/" + asset!!), style);
        } catch (e: Exception) {
            LogSystem.e("CustomTextView", "Could not get typeface: " + e.message)
            return false
        }

        typeface = tf
        if (asset == "abel") {
            if (letterSpacing < 0.055f) letterSpacing = 0.055f
        }
        return true
    }

    override fun setLongClickable(longClickable: Boolean) {
        super.setLongClickable(false)
    }


}
