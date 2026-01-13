package com.zenimmersive.android.spatial.model

/**
 * Configuration for the spatial rendering system
 */
data class SpatialConfig(
    // Motion settings
    val motionSensitivity: Float = 1.0f,
    val motionSmoothingFactor: Float = 0.15f,  // Lower = smoother, higher = more responsive
    
    // Idle drift settings
    val idleDriftEnabled: Boolean = true,
    val idleDriftAmplitude: Float = 0.15f,     // Max drift amplitude (0..1)
    val idleDriftPeriodX: Float = 12000f,      // X drift period in ms
    val idleDriftPeriodY: Float = 16000f,      // Y drift period in ms
    
    // Stillness detection
    val stillnessThreshold: Float = 0.02f,     // Motion magnitude threshold
    val stillnessDurationMs: Long = 1200,      // Time to wait before idle drift
    
    // Rendering
    val enableParallax: Boolean = true,
    val respectReduceMotion: Boolean = true
) {
    companion object {
        val DEFAULT = SpatialConfig()
        
        val HIGH_SENSITIVITY = SpatialConfig(
            motionSensitivity = 1.5f,
            motionSmoothingFactor = 0.2f
        )
        
        val CALM = SpatialConfig(
            motionSensitivity = 0.7f,
            motionSmoothingFactor = 0.1f,
            idleDriftAmplitude = 0.1f,
            idleDriftPeriodX = 18000f,
            idleDriftPeriodY = 22000f
        )
    }
}

/**
 * Current motion state from the engine
 */
data class MotionState(
    val motionX: Float = 0f,      // -1..+1 horizontal
    val motionY: Float = 0f,      // -1..+1 vertical
    val isStill: Boolean = true,
    val isUsingIdleDrift: Boolean = false
)
