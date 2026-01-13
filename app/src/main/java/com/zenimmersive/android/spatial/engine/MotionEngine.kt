package com.zenimmersive.android.spatial.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.zenimmersive.android.spatial.model.MotionState
import com.zenimmersive.android.spatial.model.SpatialConfig
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Handles device motion sensing for spatial image parallax effect.
 * Uses rotation vector sensor for smooth, accurate motion tracking.
 * 
 * Features:
 * - Real device motion from gyroscope/accelerometer
 * - Low-pass filtering for smooth output
 * - Stillness detection with configurable threshold
 * - Automatic switch to idle drift when still
 * - Respects system "Reduce Motion" setting
 */
class MotionEngine(
    private val context: Context,
    private var config: SpatialConfig = SpatialConfig.DEFAULT
) : SensorEventListener {
    
    companion object {
        private const val TAG = "MotionEngine"
    }
    
    interface MotionListener {
        fun onMotionUpdate(state: MotionState)
    }
    
    private val sensorManager: SensorManager = 
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private var rotationVectorSensor: Sensor? = null
    private var gravitySensor: Sensor? = null
    
    private var listener: MotionListener? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // Idle drift engine
    private val idleDriftEngine = IdleDriftEngine()
    
    // Current state
    private var rawMotionX = 0f
    private var rawMotionY = 0f
    private var smoothedMotionX = 0f
    private var smoothedMotionY = 0f
    
    // Stillness tracking
    private var isStill = true
    private var lastSignificantMotionTime = 0L
    private var motionMagnitudeHistory = FloatArray(10)
    private var historyIndex = 0
    
    // Reference orientation (calibration)
    private var referenceX = 0f
    private var referenceY = 0f
    private var isCalibrated = false
    
    private var isRunning = false
    private var useIdleDrift = false
    
    // Reduce motion check
    private val reduceMotionEnabled: Boolean
        get() {
            return try {
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE
                ) == 0f
            } catch (e: Exception) {
                false
            }
        }
    
    /**
     * Start listening to device motion
     */
    fun start(listener: MotionListener) {
        if (isRunning) return
        
        this.listener = listener
        
        // Check if reduce motion is enabled
        if (config.respectReduceMotion && reduceMotionEnabled) {
            // Just use idle drift, no motion
            isRunning = true
            useIdleDrift = true
            startIdleDriftLoop()
            return
        }
        
        // Try rotation vector first (most accurate)
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        
        // Fallback to gravity sensor
        if (rotationVectorSensor == null) {
            gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
        
        val sensor = rotationVectorSensor ?: gravitySensor
        
        if (sensor != null) {
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME  // ~20ms updates
            )
            isRunning = true
            lastSignificantMotionTime = System.currentTimeMillis()
        } else {
            // No sensors available, use idle drift only
            isRunning = true
            useIdleDrift = true
            startIdleDriftLoop()
        }
    }
    
    /**
     * Stop listening to device motion
     */
    fun stop() {
        if (!isRunning) return
        
        sensorManager.unregisterListener(this)
        handler.removeCallbacksAndMessages(null)
        isRunning = false
        listener = null
        isCalibrated = false
    }
    
    /**
     * Recalibrate to current device orientation
     */
    fun calibrate() {
        referenceX = rawMotionX
        referenceY = rawMotionY
        isCalibrated = true
    }
    
    /**
     * Update configuration
     */
    fun updateConfig(newConfig: SpatialConfig) {
        config = newConfig
        idleDriftEngine.updateConfig(newConfig)
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                processRotationVector(event.values)
            }
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_ACCELEROMETER -> {
                processGravity(event.values)
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
    
    private fun processRotationVector(values: FloatArray) {
        if (values.size < 3) return
        
        // Convert rotation vector to orientation angles
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)
        
        // Extract pitch and roll, normalize to -1..+1
        // Pitch: rotation around X axis (tilting forward/back)
        // Roll: rotation around Y axis (tilting left/right)
        val pitch = orientation[1]  // -π to π
        val roll = orientation[2]   // -π to π
        
        // Normalize to -1..+1 range (clamp at ±45 degrees for usable range)
        val maxAngle = Math.PI.toFloat() / 4f  // 45 degrees
        rawMotionX = (roll / maxAngle).coerceIn(-1f, 1f)
        rawMotionY = (pitch / maxAngle).coerceIn(-1f, 1f)
        
        processMotion()
    }
    
    private fun processGravity(values: FloatArray) {
        if (values.size < 3) return
        
        // Use gravity vector to determine tilt
        // When phone is flat: x≈0, y≈0, z≈9.8
        // Tilting right: x increases
        // Tilting forward: y decreases
        val x = values[0]
        val y = values[1]
        
        // Normalize to -1..+1 (gravity ~9.8, use ~5 as max tilt)
        val maxTilt = 5f
        rawMotionX = (-x / maxTilt).coerceIn(-1f, 1f)
        rawMotionY = (y / maxTilt).coerceIn(-1f, 1f)
        
        processMotion()
    }
    
    private fun processMotion() {
        // Apply calibration offset
        var adjustedX = rawMotionX
        var adjustedY = rawMotionY
        
        if (isCalibrated) {
            adjustedX -= referenceX
            adjustedY -= referenceY
        }
        
        // Apply sensitivity
        adjustedX *= config.motionSensitivity
        adjustedY *= config.motionSensitivity
        
        // Clamp after sensitivity
        adjustedX = adjustedX.coerceIn(-1f, 1f)
        adjustedY = adjustedY.coerceIn(-1f, 1f)
        
        // Apply low-pass filter (exponential smoothing)
        val alpha = config.motionSmoothingFactor
        smoothedMotionX = smoothedMotionX + alpha * (adjustedX - smoothedMotionX)
        smoothedMotionY = smoothedMotionY + alpha * (adjustedY - smoothedMotionY)
        
        // Update stillness detection
        updateStillnessDetection()
        
        // Determine final motion values
        val finalState: MotionState
        
        if (useIdleDrift) {
            // Use idle drift values
            val driftState = idleDriftEngine.calculateDrift(System.currentTimeMillis())
            finalState = MotionState(
                motionX = driftState.first,
                motionY = driftState.second,
                isStill = isStill,
                isUsingIdleDrift = true
            )
        } else {
            finalState = MotionState(
                motionX = smoothedMotionX,
                motionY = smoothedMotionY,
                isStill = isStill,
                isUsingIdleDrift = false
            )
        }
        
        // Notify listener
        listener?.onMotionUpdate(finalState)
    }
    
    private fun updateStillnessDetection() {
        // Calculate motion magnitude (change from previous)
        val deltaX = abs(rawMotionX - (motionMagnitudeHistory.getOrNull((historyIndex - 1).mod(10)) ?: 0f))
        val deltaY = abs(rawMotionY - (motionMagnitudeHistory.getOrNull((historyIndex - 2).mod(10)) ?: 0f))
        val magnitude = sqrt(deltaX * deltaX + deltaY * deltaY)
        
        // Store in history
        motionMagnitudeHistory[historyIndex] = magnitude
        historyIndex = (historyIndex + 1) % 10
        
        // Calculate average magnitude
        val avgMagnitude = motionMagnitudeHistory.average().toFloat()
        
        val currentTime = System.currentTimeMillis()
        
        if (avgMagnitude > config.stillnessThreshold) {
            // Significant motion detected
            lastSignificantMotionTime = currentTime
            if (isStill || useIdleDrift) {
                isStill = false
                useIdleDrift = false
            }
        } else {
            // Check if still for long enough
            val stillDuration = currentTime - lastSignificantMotionTime
            if (stillDuration >= config.stillnessDurationMs) {
                if (!isStill) {
                    isStill = true
                    // Enable idle drift if configured
                    if (config.idleDriftEnabled) {
                        useIdleDrift = true
                        idleDriftEngine.reset(smoothedMotionX, smoothedMotionY)
                    }
                }
            }
        }
    }
    
    private fun startIdleDriftLoop() {
        val updateRunnable = object : Runnable {
            override fun run() {
                if (!isRunning) return
                
                val driftState = idleDriftEngine.calculateDrift(System.currentTimeMillis())
                val state = MotionState(
                    motionX = driftState.first,
                    motionY = driftState.second,
                    isStill = true,
                    isUsingIdleDrift = true
                )
                listener?.onMotionUpdate(state)
                
                handler.postDelayed(this, 16) // ~60fps
            }
        }
        handler.post(updateRunnable)
    }
}
