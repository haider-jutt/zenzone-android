package com.zenimmersive.android.spatial

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.zenimmersive.android.R
import com.zenimmersive.android.databinding.ActivitySpatialDemoBinding
import com.zenimmersive.android.spatial.engine.MotionEngine
import com.zenimmersive.android.spatial.model.MotionState
import com.zenimmersive.android.spatial.model.SpatialAsset
import com.zenimmersive.android.spatial.model.SpatialConfig
import com.zenimmersive.android.spatial.view.SpatialImageView
import java.io.IOException

/**
 * Demo Activity to showcase the ZEN IMMERSIVE Spatial Image System.
 * 
 * This demonstrates:
 * - 2.5D parallax effect with background and foreground layers
 * - Device motion response (tilt phone to move image)
 * - Idle drift when device is still
 * - Real-time motion data visualization
 * 
 * To launch this demo from anywhere in the app:
 * ```
 * startActivity(Intent(context, SpatialDemoActivity::class.java))
 * ```
 * 
 * SETUP INSTRUCTIONS:
 * 1. Place your foreground image (person/subject) at:
 *    assets/spatial_demo/foreground.png
 * 
 * 2. Optionally place a background image at:
 *    assets/spatial_demo/background.png
 *    (If not provided, a gradient will be generated)
 *
 * NOTE: If the image has a white background, it will be automatically
 * converted to transparent.
 */
class SpatialDemoActivity : AppCompatActivity(), MotionEngine.MotionListener {
    
    companion object {
        private const val TAG = "SpatialDemo"
        
        // Asset paths for demo images
        private const val FOREGROUND_ASSET = "spatial_demo/foreground.png"
        private const val BACKGROUND_ASSET = "spatial_demo/background.png"
    }
    
    private lateinit var binding: ActivitySpatialDemoBinding
    private lateinit var motionEngine: MotionEngine
    
    private var forceDriftMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge
        setupFullscreen()
        
        binding = ActivitySpatialDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize motion engine
        motionEngine = MotionEngine(this, SpatialConfig.DEFAULT)
        
        // Setup UI
        setupSpatialView()
        setupControls()
    }
    
    private fun setupFullscreen() {
        // Make status bar transparent
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Keep screen on during demo
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Hide system bars for immersive experience
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = 
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    private fun setupSpatialView() {
        binding.progressBar.visibility = View.VISIBLE
        
        // Load the demo images
        loadDemoAsset()
    }
    
    private fun loadDemoAsset() {
        try {
            // Try to load foreground from assets first
            var foregroundBitmap = loadBitmapFromAssets(FOREGROUND_ASSET)
            
            // If loaded, check if it needs white-to-transparent conversion
            if (foregroundBitmap != null) {
                foregroundBitmap = removeWhiteBackground(foregroundBitmap)
            }
            
            // Note: To use a drawable resource, add your image as:
            // res/drawable/spatial_demo_foreground.png
            // and uncomment the following:
            // if (foregroundBitmap == null) {
            //     foregroundBitmap = try {
            //         val bmp = BitmapFactory.decodeResource(resources, R.drawable.spatial_demo_foreground)
            //         if (bmp != null) removeWhiteBackground(bmp) else null
            //     } catch (e: Exception) { null }
            // }
            
            // If still no foreground, create a placeholder
            if (foregroundBitmap == null) {
                // Create a sample foreground (silhouette placeholder)
                foregroundBitmap = createPlaceholderForeground(600, 1200)
            }
            
            // Try to load background from assets
            var backgroundBitmap = loadBitmapFromAssets(BACKGROUND_ASSET)
            
            // Fallback: generate a beautiful gradient background
            if (backgroundBitmap == null) {
                backgroundBitmap = createGradientBackground(
                    foregroundBitmap?.width ?: 1080,
                    foregroundBitmap?.height ?: 1920
                )
            }
            
            // Create the spatial asset
            val demoAsset = SpatialAsset.createDemo(
                id = "demo_001",
                background = backgroundBitmap,
                foreground = foregroundBitmap,
                parallaxBg = 0.25f,     // Background moves slightly
                parallaxFg = 1.0f,      // Foreground moves more (full motion)
                safeZoom = 1.15f,       // 15% zoom to prevent edge visibility
                amplitudePx = 60        // Max movement in pixels
            )
            
            // Set asset to spatial view
            binding.spatialImageView.setAsset(demoAsset, animate = false)
            
            // Enable debug mode to show motion indicator
            binding.spatialImageView.debugMode = true
            
            // Set listener for load events
            binding.spatialImageView.setListener(object : SpatialImageView.SpatialImageListener {
                override fun onAssetLoaded() {
                    binding.progressBar.visibility = View.GONE
                }
                
                override fun onAssetLoadError(error: String) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvMotionStatus.text = "Error: $error"
                }
            })
            
            binding.progressBar.visibility = View.GONE
            
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            binding.tvMotionStatus.text = "Error loading demo: ${e.message}"
            e.printStackTrace()
        }
    }
    
    /**
     * Load a bitmap from the assets folder
     */
    private fun loadBitmapFromAssets(assetPath: String): Bitmap? {
        return try {
            assets.open(assetPath).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            null
        }
    }
    
    /**
     * Removes white/near-white background from an image and replaces with transparency.
     * This is useful for images that have white backgrounds instead of transparent ones.
     */
    private fun removeWhiteBackground(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        
        // Create a mutable bitmap with alpha
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Threshold for "white" detection (allowing some tolerance)
        val whiteThreshold = 240
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            
            // Check if pixel is white or near-white
            if (r > whiteThreshold && g > whiteThreshold && b > whiteThreshold) {
                // Make transparent
                pixels[i] = Color.TRANSPARENT
            } else if (r > 220 && g > 220 && b > 220) {
                // Semi-transparent for edge smoothing
                val alpha = 255 - ((r + g + b) / 3 - 220) * 7
                pixels[i] = Color.argb(alpha.coerceIn(0, 255), r, g, b)
            }
        }
        
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * Creates a placeholder foreground when no image is available.
     * Shows a simple silhouette to demonstrate the effect.
     */
    private fun createPlaceholderForeground(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Draw transparent background
        canvas.drawColor(Color.TRANSPARENT)
        
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        
        // Draw a simple humanoid silhouette
        val centerX = width / 2f
        val scale = height / 1200f
        
        // Head
        paint.color = Color.parseColor("#455A64")
        canvas.drawCircle(centerX, 100 * scale, 60 * scale, paint)
        
        // Body (t-shirt shape)
        paint.color = Color.parseColor("#78909C")  // Gray like the t-shirt
        val bodyPath = android.graphics.Path()
        bodyPath.moveTo(centerX - 100 * scale, 160 * scale)  // Left shoulder
        bodyPath.lineTo(centerX - 150 * scale, 200 * scale)  // Left sleeve
        bodyPath.lineTo(centerX - 120 * scale, 300 * scale)  // Left arm end
        bodyPath.lineTo(centerX - 90 * scale, 280 * scale)   // Left arm inner
        bodyPath.lineTo(centerX - 80 * scale, 550 * scale)   // Left torso
        bodyPath.lineTo(centerX + 80 * scale, 550 * scale)   // Right torso
        bodyPath.lineTo(centerX + 90 * scale, 280 * scale)   // Right arm inner
        bodyPath.lineTo(centerX + 120 * scale, 300 * scale)  // Right arm end
        bodyPath.lineTo(centerX + 150 * scale, 200 * scale)  // Right sleeve
        bodyPath.lineTo(centerX + 100 * scale, 160 * scale)  // Right shoulder
        bodyPath.close()
        canvas.drawPath(bodyPath, paint)
        
        // Legs (jeans)
        paint.color = Color.parseColor("#1565C0")  // Blue like jeans
        
        // Left leg
        val leftLegPath = android.graphics.Path()
        leftLegPath.moveTo(centerX - 80 * scale, 550 * scale)
        leftLegPath.lineTo(centerX - 10 * scale, 550 * scale)
        leftLegPath.lineTo(centerX - 20 * scale, 1050 * scale)
        leftLegPath.lineTo(centerX - 70 * scale, 1050 * scale)
        leftLegPath.close()
        canvas.drawPath(leftLegPath, paint)
        
        // Right leg
        val rightLegPath = android.graphics.Path()
        rightLegPath.moveTo(centerX + 10 * scale, 550 * scale)
        rightLegPath.lineTo(centerX + 80 * scale, 550 * scale)
        rightLegPath.lineTo(centerX + 70 * scale, 1050 * scale)
        rightLegPath.lineTo(centerX + 20 * scale, 1050 * scale)
        rightLegPath.close()
        canvas.drawPath(rightLegPath, paint)
        
        // Shoes
        paint.color = Color.parseColor("#212121")  // Black shoes
        canvas.drawRoundRect(
            centerX - 80 * scale, 1050 * scale,
            centerX - 15 * scale, 1100 * scale,
            10f, 10f, paint
        )
        canvas.drawRoundRect(
            centerX + 15 * scale, 1050 * scale,
            centerX + 80 * scale, 1100 * scale,
            10f, 10f, paint
        )
        
        // Info text
        paint.color = Color.WHITE
        paint.textSize = 20 * scale
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("Place foreground.png in", centerX, 1130 * scale, paint)
        canvas.drawText("assets/spatial_demo/", centerX, 1160 * scale, paint)
        
        return bitmap
    }
    
    /**
     * Creates a gradient background bitmap.
     * This simulates a scenic background for the demo.
     * In production, this would be replaced by actual scenic images from backend.
     */
    private fun createGradientBackground(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Create a calming gradient (soft blue to soft purple - zen/meditation theme)
        val gradient = android.graphics.LinearGradient(
            0f, 0f, 
            width * 0.3f, height.toFloat(),
            intArrayOf(
                Color.parseColor("#0d1b2a"),  // Very dark blue
                Color.parseColor("#1b263b"),  // Dark blue
                Color.parseColor("#415a77"),  // Medium blue
                Color.parseColor("#778da9"),  // Light blue-gray
                Color.parseColor("#e0e1dd")   // Very light gray
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        
        val paint = android.graphics.Paint()
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // Add some subtle particles for atmosphere
        addAtmosphericParticles(canvas, width, height)
        
        return bitmap
    }
    
    private fun addAtmosphericParticles(canvas: android.graphics.Canvas, width: Int, height: Int) {
        val paint = android.graphics.Paint()
        paint.isAntiAlias = true
        
        val random = java.util.Random(42)  // Fixed seed for consistency
        
        // Draw grid lines to make parallax visible
        paint.color = Color.argb(30, 255, 255, 255)
        paint.strokeWidth = 2f
        val gridSpacing = 100
        for (x in 0 until width step gridSpacing) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), paint)
        }
        for (y in 0 until height step gridSpacing) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
        }
        
        // Bright visible stars (larger and more visible)
        paint.color = Color.WHITE
        paint.style = android.graphics.Paint.Style.FILL
        for (i in 0 until 80) {
            val x = random.nextFloat() * width
            val y = random.nextFloat() * height
            val radius = random.nextFloat() * 6 + 3  // Bigger stars
            val alpha = (random.nextFloat() * 150 + 100).toInt()  // More visible
            paint.alpha = alpha
            canvas.drawCircle(x, y, radius, paint)
        }
        
        // Some larger soft glows
        for (i in 0 until 30) {
            val x = random.nextFloat() * width
            val y = random.nextFloat() * height * 0.7f
            val radius = random.nextFloat() * 60 + 30
            
            val glowGradient = android.graphics.RadialGradient(
                x, y, radius,
                Color.argb(40, 255, 255, 255),
                Color.TRANSPARENT,
                android.graphics.Shader.TileMode.CLAMP
            )
            paint.shader = glowGradient
            paint.alpha = 255
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
        }
        
        // Add some colored orbs for visual interest
        val colors = intArrayOf(
            Color.parseColor("#4FC3F7"),  // Light blue
            Color.parseColor("#81C784"),  // Green
            Color.parseColor("#FFB74D"),  // Orange
            Color.parseColor("#F06292")   // Pink
        )
        for (i in 0 until 15) {
            val x = random.nextFloat() * width
            val y = random.nextFloat() * height
            val radius = random.nextFloat() * 40 + 20
            val color = colors[random.nextInt(colors.size)]
            
            val orbGradient = android.graphics.RadialGradient(
                x, y, radius,
                Color.argb(60, Color.red(color), Color.green(color), Color.blue(color)),
                Color.TRANSPARENT,
                android.graphics.Shader.TileMode.CLAMP
            )
            paint.shader = orbGradient
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
        }
    }
    
    private fun setupControls() {
        // Calibrate button - sets current position as neutral
        binding.btnCalibrate.setOnClickListener {
            motionEngine.calibrate()
            binding.tvMotionStatus.text = "Calibrated to current position"
        }
        
        // Toggle drift mode - forces idle drift regardless of motion
        binding.btnToggleMode.setOnClickListener {
            forceDriftMode = !forceDriftMode
            
            if (forceDriftMode) {
                binding.btnToggleMode.text = "Device Motion"
                binding.btnToggleMode.setBackgroundColor(Color.parseColor("#4CAF50"))
                // Stop motion engine and start manual drift
                motionEngine.stop()
                startManualDrift()
            } else {
                binding.btnToggleMode.text = "Force Drift"
                binding.btnToggleMode.setBackgroundColor(Color.parseColor("#6200EE"))
                // Restart motion engine
                motionEngine.start(this)
            }
        }
        
        // Close button
        binding.btnClose.setOnClickListener {
            finish()
        }
    }
    
    private var driftRunnable: Runnable? = null
    private val driftHandler = android.os.Handler(android.os.Looper.getMainLooper())
    
    private fun startManualDrift() {
        val startTime = System.currentTimeMillis()
        
        driftRunnable = object : Runnable {
            override fun run() {
                if (!forceDriftMode) return
                
                val elapsed = System.currentTimeMillis() - startTime
                
                // Generate smooth drift using sine waves
                val driftX = (kotlin.math.sin(elapsed / 8000.0) * 0.3).toFloat()
                val driftY = (kotlin.math.sin(elapsed / 12000.0) * 0.2).toFloat()
                
                // Update spatial view directly
                binding.spatialImageView.setMotionOffset(driftX, driftY)
                
                // Update UI
                updateMotionUI(MotionState(driftX, driftY, true, true))
                
                driftHandler.postDelayed(this, 16) // ~60fps
            }
        }
        driftHandler.post(driftRunnable!!)
    }
    
    private fun stopManualDrift() {
        driftRunnable?.let { driftHandler.removeCallbacks(it) }
        driftRunnable = null
    }
    
    override fun onMotionUpdate(state: MotionState) {
        // Update spatial image view
        binding.spatialImageView.setMotionOffset(state.motionX, state.motionY)
        
        // Update UI
        updateMotionUI(state)
    }
    
    private fun updateMotionUI(state: MotionState) {
        runOnUiThread {
            binding.tvMotionX.text = String.format("%.2f", state.motionX)
            binding.tvMotionY.text = String.format("%.2f", state.motionY)
            
            when {
                state.isUsingIdleDrift -> {
                    binding.tvMotionMode.text = "Drift"
                    binding.tvMotionMode.setTextColor(Color.parseColor("#2196F3"))
                    binding.tvMotionStatus.text = "Idle drift active - move phone to take control"
                }
                state.isStill -> {
                    binding.tvMotionMode.text = "Still"
                    binding.tvMotionMode.setTextColor(Color.parseColor("#FFC107"))
                    binding.tvMotionStatus.text = "Device is still..."
                }
                else -> {
                    binding.tvMotionMode.text = "Active"
                    binding.tvMotionMode.setTextColor(Color.parseColor("#4CAF50"))
                    binding.tvMotionStatus.text = "Tilt your phone to see the effect"
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (!forceDriftMode) {
            motionEngine.start(this)
        }
    }
    
    override fun onPause() {
        super.onPause()
        motionEngine.stop()
        stopManualDrift()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        motionEngine.stop()
        stopManualDrift()
    }
}
