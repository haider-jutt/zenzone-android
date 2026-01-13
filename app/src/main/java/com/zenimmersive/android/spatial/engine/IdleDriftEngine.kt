package com.zenimmersive.android.spatial.engine

import com.zenimmersive.android.spatial.model.SpatialConfig
import kotlin.math.sin

/**
 * Generates smooth, procedural "idle drift" motion when the device is still.
 * Creates a gentle, organic movement that prevents the spatial image from feeling static.
 * 
 * Uses combination of sine waves with different periods and phases
 * to create natural, non-robotic motion.
 */
class IdleDriftEngine(
    private var config: SpatialConfig = SpatialConfig.DEFAULT
) {
    
    // Starting position (where device motion left off)
    private var startX = 0f
    private var startY = 0f
    
    // Time when drift started
    private var startTimeMs = 0L
    
    // Random phases for variety
    private var phaseX1 = 0f
    private var phaseX2 = 0f
    private var phaseY1 = 0f
    private var phaseY2 = 0f
    
    // Seed for deterministic drift per session/track
    private var seed = System.currentTimeMillis()
    
    init {
        randomizePhases()
    }
    
    /**
     * Reset drift to start from current position
     */
    fun reset(currentX: Float = 0f, currentY: Float = 0f) {
        startX = currentX
        startY = currentY
        startTimeMs = System.currentTimeMillis()
        randomizePhases()
    }
    
    /**
     * Set a deterministic seed for reproducible drift patterns
     */
    fun setSeed(newSeed: Long) {
        seed = newSeed
        randomizePhases()
    }
    
    /**
     * Update configuration
     */
    fun updateConfig(newConfig: SpatialConfig) {
        config = newConfig
    }
    
    /**
     * Calculate current drift position based on time
     * @return Pair of (x, y) motion values in range -1..+1
     */
    fun calculateDrift(currentTimeMs: Long): Pair<Float, Float> {
        val elapsed = currentTimeMs - startTimeMs
        
        // Use combination of two sine waves with different periods
        // This creates more organic, less predictable motion
        
        // Primary X wave
        val x1 = sin(
            (2.0 * Math.PI * elapsed / config.idleDriftPeriodX) + phaseX1
        ).toFloat()
        
        // Secondary X wave (slower, smaller amplitude)
        val x2 = sin(
            (2.0 * Math.PI * elapsed / (config.idleDriftPeriodX * 1.7)) + phaseX2
        ).toFloat() * 0.3f
        
        // Primary Y wave
        val y1 = sin(
            (2.0 * Math.PI * elapsed / config.idleDriftPeriodY) + phaseY1
        ).toFloat()
        
        // Secondary Y wave (slower, smaller amplitude)
        val y2 = sin(
            (2.0 * Math.PI * elapsed / (config.idleDriftPeriodY * 1.5)) + phaseY2
        ).toFloat() * 0.25f
        
        // Combine waves
        val combinedX = (x1 + x2) / 1.3f  // Normalize
        val combinedY = (y1 + y2) / 1.25f
        
        // Apply amplitude and add to start position
        val driftX = startX + (combinedX * config.idleDriftAmplitude)
        val driftY = startY + (combinedY * config.idleDriftAmplitude)
        
        // Clamp to valid range
        return Pair(
            driftX.coerceIn(-1f, 1f),
            driftY.coerceIn(-1f, 1f)
        )
    }
    
    private fun randomizePhases() {
        val random = java.util.Random(seed)
        phaseX1 = (random.nextFloat() * 2 * Math.PI).toFloat()
        phaseX2 = (random.nextFloat() * 2 * Math.PI).toFloat()
        phaseY1 = (random.nextFloat() * 2 * Math.PI).toFloat()
        phaseY2 = (random.nextFloat() * 2 * Math.PI).toFloat()
    }
}
