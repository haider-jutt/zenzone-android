package com.zenimmersive.android.helper

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.zenimmersive.android.databinding.SimpleLoaderMessageViewBinding


class SimpleLoadingMessageView : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    var simpleLoaderMessageViewBinding: SimpleLoaderMessageViewBinding? = null

    init {
        removeAllViews()
        simpleLoaderMessageViewBinding =
            SimpleLoaderMessageViewBinding.inflate(LayoutInflater.from(context), null, false)
        var params = FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER
        addView(simpleLoaderMessageViewBinding!!.root, params)

        visibility = visibility
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == VISIBLE) {
            simpleLoaderMessageViewBinding?.root?.let {
                it.post {
                    addAnimation(it)
                }
            }
        } else {
            rotateAnimation?.cancel()
        }
    }

    var rotateAnimation: RotateAnimation? = null
    private fun addAnimation(linearLayout: LinearLayout) {
        var view = linearLayout.getChildAt(0)

        rotateAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )

        rotateAnimation?.duration = 900
        rotateAnimation?.repeatCount = Animation.INFINITE
        view.startAnimation(rotateAnimation)
    }

    fun setMessage(message: String) {
        simpleLoaderMessageViewBinding?.tvMessage?.setText(message)
    }
}