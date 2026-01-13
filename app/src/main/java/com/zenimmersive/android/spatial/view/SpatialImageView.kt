package com.zenimmersive.android.spatial.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.zenimmersive.android.spatial.engine.MotionEngine
import com.zenimmersive.android.spatial.model.MotionState
import com.zenimmersive.android.spatial.model.SpatialAsset
import com.zenimmersive.android.spatial.model.SpatialConfig

/**
 * Custom View that renders spatial 2.5D images with parallax effect.
 * 
 * Features:
 * - Background layer (moves slightly with motion)
 * - Foreground layer (moves more with motion)
 * - Responds to device tilt via MotionEngine
 * - Idle drift when device is still
 * - Smooth transitions between assets
 * - 16:9 master canvas viewed through 9:16 viewport on mobile
 */
class SpatialImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), MotionEngine.MotionListener {
    
    companion object {
        private const val TAG = "SpatialImageView"
    }
    
    // Current spatial asset
    private var currentAsset: SpatialAsset? = null
    
    // Bitmaps for rendering
    private var backgroundBitmap: Bitmap? = null
    private var foregroundBitmap: Bitmap? = null
    
    // Matrices for transformations
    private val bgMatrix = Matrix()
    private val fgMatrix = Matrix()
    
    // Paint objects
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val debugPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        isAntiAlias = true
    }
    
    // Motion state
    private var currentMotionX = 0f
    private var currentMotionY = 0f
    private var targetMotionX = 0f
    private var targetMotionY = 0f
    
    // Configuration
    private var config = SpatialConfig.DEFAULT
    private var motionEngine: MotionEngine? = null
    
    // Debug mode - shows motion indicators
    var debugMode = true  // Enable by default for demo
    private var debugInfo = ""
    
    // Debug paint for motion indicator
    private val motionIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val motionDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
    }
    
    // Transition animator
    private var transitionAnimator: ValueAnimator? = null
    private var transitionProgress = 1f
    private var previousBgBitmap: Bitmap? = null
    private var previousFgBitmap: Bitmap? = null
    
    // Loading state
    var isLoading = false
        private set
    
    // Listener
    interface SpatialImageListener {
        fun onAssetLoaded()
        fun onAssetLoadError(error: String)
    }
    
    private var listener: SpatialImageListener? = null
    
    init {
        setWillNotDraw(false)  // Enable onDraw
        setLayerType(LAYER_TYPE_HARDWARE, null)  // Hardware acceleration
    }
    
    /**
     * Set the spatial asset to display
     */
    fun setAsset(asset: SpatialAsset, animate: Boolean = true) {
        // Store previous bitmaps for transition
        if (animate && backgroundBitmap != null) {
            previousBgBitmap = backgroundBitmap
            previousFgBitmap = foregroundBitmap
        }
        
        currentAsset = asset
        
        // Check if bitmaps are provided directly (demo mode)
        if (asset.bgBitmap != null) {
            backgroundBitmap = asset.bgBitmap
            foregroundBitmap = asset.fgBitmap
            
            if (animate) {
                startTransition()
            } else {
                transitionProgress = 1f
            }
            
            // Post invalidation to ensure view is laid out
            post {
                invalidate()
                listener?.onAssetLoaded()
            }
            return
        }
        
        // Load from URLs
        isLoading = true
        loadBitmapsFromUrls(asset, animate)
    }
    
    /**
     * Set a listener for asset load events
     */
    fun setListener(listener: SpatialImageListener) {
        this.listener = listener
    }
    
    /**
     * Set configuration
     */
    fun setConfig(config: SpatialConfig) {
        this.config = config
    }
    
    /**
     * Start receiving motion updates
     */
    fun startMotion(motionEngine: MotionEngine) {
        this.motionEngine = motionEngine
        motionEngine.start(this)
    }
    
    /**
     * Stop receiving motion updates
     */
    fun stopMotion() {
        motionEngine?.stop()
        motionEngine = null
    }
    
    /**
     * Manually set motion offset (for testing or external control)
     */
    fun setMotionOffset(x: Float, y: Float) {
        targetMotionX = x.coerceIn(-1f, 1f)
        targetMotionY = y.coerceIn(-1f, 1f)
        
        // Use direct assignment for immediate response
        // with slight smoothing to prevent jitter
        val alpha = 0.5f  // Higher = more responsive
        currentMotionX = currentMotionX + alpha * (targetMotionX - currentMotionX)
        currentMotionY = currentMotionY + alpha * (targetMotionY - currentMotionY)
        
        // Force redraw
        postInvalidateOnAnimation()
    }
    
    /**
     * Set motion offset immediately without smoothing (for testing)
     */
    fun setMotionOffsetImmediate(x: Float, y: Float) {
        currentMotionX = x.coerceIn(-1f, 1f)
        currentMotionY = y.coerceIn(-1f, 1f)
        targetMotionX = currentMotionX
        targetMotionY = currentMotionY
        postInvalidateOnAnimation()
    }
    
    override fun onMotionUpdate(state: MotionState) {
        targetMotionX = state.motionX
        targetMotionY = state.motionY
        
        if (debugMode) {
            debugInfo = "Motion: (${String.format("%.2f", state.motionX)}, ${String.format("%.2f", state.motionY)})\n" +
                    "Still: ${state.isStill}, Drift: ${state.isUsingIdleDrift}"
        }
        
        // Smooth interpolation towards target
        val alpha = 0.2f
        currentMotionX += alpha * (targetMotionX - currentMotionX)
        currentMotionY += alpha * (targetMotionY - currentMotionY)
        
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val asset = currentAsset ?: return
        val bgBmp = backgroundBitmap ?: return
        
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        
        if (viewWidth <= 0 || viewHeight <= 0) return
        
        // Draw previous frame if transitioning
        if (transitionProgress < 1f && previousBgBitmap != null) {
            drawSpatialLayers(
                canvas, 
                previousBgBitmap!!, 
                previousFgBitmap,
                asset,
                viewWidth,
                viewHeight,
                1f - transitionProgress  // Fade out
            )
        }
        
        // Draw current frame
        drawSpatialLayers(
            canvas,
            bgBmp,
            foregroundBitmap,
            asset,
            viewWidth,
            viewHeight,
            if (transitionProgress < 1f) transitionProgress else 1f  // Fade in
        )
        
        // Draw debug info and motion indicator
        if (debugMode) {
            // Draw motion indicator circle in center
            val centerX = viewWidth / 2f
            val centerY = viewHeight / 2f
            val indicatorRadius = 80f
            
            // Background circle
            motionIndicatorPaint.color = Color.argb(100, 255, 255, 255)
            canvas.drawCircle(centerX, centerY, indicatorRadius, motionIndicatorPaint)
            
            // Cross lines
            motionIndicatorPaint.color = Color.argb(50, 255, 255, 255)
            canvas.drawLine(centerX - indicatorRadius, centerY, centerX + indicatorRadius, centerY, motionIndicatorPaint)
            canvas.drawLine(centerX, centerY - indicatorRadius, centerX, centerY + indicatorRadius, motionIndicatorPaint)
            
            // Moving dot showing current motion
            val dotX = centerX + (currentMotionX * indicatorRadius * 0.8f)
            val dotY = centerY + (currentMotionY * indicatorRadius * 0.8f)
            
            motionDotPaint.color = Color.parseColor("#4CAF50")
            canvas.drawCircle(dotX, dotY, 16f, motionDotPaint)
            
            // Draw text info
            canvas.drawText(debugInfo, 20f, height - 60f, debugPaint)
            
            // Draw current motion values
            debugPaint.textSize = 28f
            canvas.drawText(
                "Motion: X=${String.format("%.2f", currentMotionX)} Y=${String.format("%.2f", currentMotionY)}",
                20f, 
                height - 100f, 
                debugPaint
            )
        }
    }
    
    private fun drawSpatialLayers(
        canvas: Canvas,
        bgBitmap: Bitmap,
        fgBitmap: Bitmap?,
        asset: SpatialAsset,
        viewWidth: Float,
        viewHeight: Float,
        alpha: Float
    ) {
        // Calculate the scale to fit and fill the view
        val bmpWidth = bgBitmap.width.toFloat()
        val bmpHeight = bgBitmap.height.toFloat()
        
        // Apply safe zoom to prevent edges from showing during motion
        val safeZoom = asset.safeZoom
        
        // Scale to cover the view with safe zoom
        val scaleX = (viewWidth / bmpWidth) * safeZoom
        val scaleY = (viewHeight / bmpHeight) * safeZoom
        val scale = maxOf(scaleX, scaleY)
        
        val scaledWidth = bmpWidth * scale
        val scaledHeight = bmpHeight * scale
        
        // Calculate motion offsets
        val maxOffsetX = (scaledWidth - viewWidth) / 2f
        val maxOffsetY = (scaledHeight - viewHeight) / 2f
        
        // Amplitude from asset
        val amplitudeX = minOf(asset.amplitudePx.toFloat(), maxOffsetX)
        val amplitudeY = minOf(asset.amplitudePx.toFloat(), maxOffsetY)
        
        // --- Draw Background Layer ---
        bgMatrix.reset()
        bgMatrix.setScale(scale, scale)
        
        // Center the image
        val bgCenterOffsetX = (viewWidth - scaledWidth) / 2f
        val bgCenterOffsetY = (viewHeight - scaledHeight) / 2f
        
        // Apply parallax motion offset (background moves less)
        val bgMotionOffsetX = currentMotionX * amplitudeX * asset.parallaxBg
        val bgMotionOffsetY = currentMotionY * amplitudeY * asset.parallaxBg
        
        bgMatrix.postTranslate(
            bgCenterOffsetX + bgMotionOffsetX,
            bgCenterOffsetY + bgMotionOffsetY
        )
        
        bgPaint.alpha = (255 * alpha).toInt()
        canvas.drawBitmap(bgBitmap, bgMatrix, bgPaint)
        
        // --- Draw Foreground Layer (if present) ---
        fgBitmap?.let { fg ->
            val fgWidth = fg.width.toFloat()
            val fgHeight = fg.height.toFloat()
            
            // Scale foreground to match background scale
            val fgScaleX = (viewWidth / fgWidth) * safeZoom
            val fgScaleY = (viewHeight / fgHeight) * safeZoom
            val fgScale = maxOf(fgScaleX, fgScaleY)
            
            val scaledFgWidth = fgWidth * fgScale
            val scaledFgHeight = fgHeight * fgScale
            
            fgMatrix.reset()
            fgMatrix.setScale(fgScale, fgScale)
            
            // Center foreground
            val fgCenterOffsetX = (viewWidth - scaledFgWidth) / 2f
            val fgCenterOffsetY = (viewHeight - scaledFgHeight) / 2f
            
            // Apply parallax motion offset (foreground moves more)
            val fgMotionOffsetX = currentMotionX * amplitudeX * asset.parallaxFg
            val fgMotionOffsetY = currentMotionY * amplitudeY * asset.parallaxFg
            
            fgMatrix.postTranslate(
                fgCenterOffsetX + fgMotionOffsetX,
                fgCenterOffsetY + fgMotionOffsetY
            )
            
            fgPaint.alpha = (255 * alpha).toInt()
            canvas.drawBitmap(fg, fgMatrix, fgPaint)
        }
    }
    
    private fun loadBitmapsFromUrls(asset: SpatialAsset, animate: Boolean) {
        // Load background
        asset.bgUrl?.let { url ->
            Glide.with(context)
                .asBitmap()
                .load(url)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        backgroundBitmap = resource
                        
                        // Load foreground if present
                        if (asset.fgUrl != null) {
                            loadForeground(asset, animate)
                        } else {
                            isLoading = false
                            if (animate) startTransition() else transitionProgress = 1f
                            invalidate()
                            listener?.onAssetLoaded()
                        }
                    }
                    
                    override fun onLoadCleared(placeholder: Drawable?) {
                        backgroundBitmap = null
                    }
                    
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        isLoading = false
                        listener?.onAssetLoadError("Failed to load background image")
                    }
                })
        }
    }
    
    private fun loadForeground(asset: SpatialAsset, animate: Boolean) {
        asset.fgUrl?.let { url ->
            Glide.with(context)
                .asBitmap()
                .load(url)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        foregroundBitmap = resource
                        isLoading = false
                        if (animate) startTransition() else transitionProgress = 1f
                        invalidate()
                        listener?.onAssetLoaded()
                    }
                    
                    override fun onLoadCleared(placeholder: Drawable?) {
                        foregroundBitmap = null
                    }
                    
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        // Foreground is optional, proceed without it
                        isLoading = false
                        if (animate) startTransition() else transitionProgress = 1f
                        invalidate()
                        listener?.onAssetLoaded()
                    }
                })
        }
    }
    
    private fun startTransition() {
        transitionAnimator?.cancel()
        transitionProgress = 0f
        
        transitionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500  // 500ms crossfade
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                transitionProgress = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopMotion()
        transitionAnimator?.cancel()
    }
}
