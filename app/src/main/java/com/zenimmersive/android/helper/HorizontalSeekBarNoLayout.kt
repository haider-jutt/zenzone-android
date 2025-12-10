package com.zenimmersive.android.helper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.zenimmersive.android.R

/**
 * A custom Horizontal SeekBar without a layout.
 */
open class HorizontalSeekBarNoLayout : View {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init(context, attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    companion object {
        private const val DEFAULT_MAX_VALUE = 100
        private const val DEFAULT_PROGRESS = 50
        private const val DEFAULT_DRAWABLE_BACKGROUND: String = "#24242E"
        private const val DEFAULT_DRAWABLE_PROGRESS_START: String = "#4D88E1"
        private const val DEFAULT_DRAWABLE_PROGRESS_END: String = "#7BA1DB"
    }

    var startColor = Color.parseColor(DEFAULT_DRAWABLE_PROGRESS_START)
    var endColor = Color.parseColor(DEFAULT_DRAWABLE_PROGRESS_END)
    var textSize = 50f
    private var progress = DEFAULT_PROGRESS
    private var maxValue = DEFAULT_MAX_VALUE
    private var rectF: RectF? = null
    private val path: Path = Path()
    private var cornerRadius = 15f

    var actionMode = 0

    private fun init(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val attributes =
                context.obtainStyledAttributes(attrs, R.styleable.HorizontalSeekBarNoLayout, 0, 0)
            cornerRadius = attributes.getDimensionPixelSize(
                R.styleable.HorizontalSeekBarNoLayout_hsbnl_radius,
                15
            ).toFloat()

            startColor = attributes.getColor(
                R.styleable.HorizontalSeekBarNoLayout_hsbnl_start_color,
                Color.parseColor(DEFAULT_DRAWABLE_PROGRESS_START)
            )
            endColor = attributes.getColor(
                R.styleable.HorizontalSeekBarNoLayout_hsbnl_end_color,
                Color.parseColor(DEFAULT_DRAWABLE_PROGRESS_END)
            )
            maxValue = attributes.getInteger(
                R.styleable.HorizontalSeekBarNoLayout_hsbnl_max_value,
                DEFAULT_MAX_VALUE
            )
            progress = attributes.getInteger(
                R.styleable.HorizontalSeekBarNoLayout_hsbnl_progress,
                DEFAULT_PROGRESS
            )

            textSize = attributes.getDimensionPixelSize(
                R.styleable.HorizontalSeekBarNoLayout_hsbnl_text_size,
                50
            ).toFloat()

            attributes.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (rectF != null) {
            resetPath()
            canvas.clipPath(path)
        }

        canvas.drawColor(Color.parseColor(DEFAULT_DRAWABLE_BACKGROUND))

        // Draw the progress
        val progressWidth = width * (progress / maxValue.toFloat())
        val progressRectF = RectF(0f, 0f, progressWidth, height.toFloat())
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        progressPaint.shader = LinearGradient(
            0f,
            0f,
            progressWidth,
            0f,
            startColor,
            endColor,
            Shader.TileMode.MIRROR
        )
        canvas.drawRect(progressRectF, progressPaint)

        // Draw the progress text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = Color.WHITE
        textPaint.textSize = textSize
        textPaint.textAlign = Align.CENTER
        canvas.drawText("${progress}%", width / 2f, height / 2f + textSize / 3, textPaint)
    }

    private fun resetPath() {
        if (rectF == null) return
        path.reset()
        path.addRoundRect(rectF!!, cornerRadius, cornerRadius, Path.Direction.CW)
        path.close()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rectF = RectF(0f, 0f, w.toFloat(), h.toFloat())
        resetPath()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                updateProgress(event.x)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (actionMode == 1) {
                    postProgressChanges()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateProgress(x: Float) {
        val newProgress = (maxValue * (x / width)).toInt()
        progress = when {
            newProgress < 0 -> 0
            newProgress > maxValue -> maxValue
            else -> newProgress
        }

        if (actionMode == 0) postProgressChanges()
        invalidate()
    }

    private fun postProgressChanges() {
        onProgressChangeListener?.invoke(progress)
    }

    fun bindActionMode(actionMode: Int) {
        this.actionMode = actionMode
    }

    fun setProgress(progress: Int) {
        this.progress = when {
            progress < 0 -> 0
            progress > maxValue -> maxValue
            else -> progress
        }
        invalidate()
    }

    fun getProgress(): Int {
        return progress
    }

    fun setMaxValue(maxValue: Int) {
        this.maxValue = maxValue
        invalidate()
    }

    fun getMaxValue(): Int {
        return maxValue
    }

    fun setOnProgressChangeListener(listener: ((Int) -> Unit)?) {
        this.onProgressChangeListener = listener
    }

    private var onProgressChangeListener: ((Int) -> Unit)? = null
}
