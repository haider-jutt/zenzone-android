package com.zenimmersive.android.ui.player

import android.os.Handler
import android.os.Looper
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import com.zenimmersive.android.helper.LogSystem
import kotlin.math.abs

/**
 * Synchronizes video player with audio players (music + voice) to prevent drift.
 * Audio is the master clock - video follows audio precisely.
 * 
 * Sync Strategy:
 * - Very small drift (<50ms): No correction needed - within acceptable range
 * - Small drift (50-150ms): Adjust playback speed slightly (0.97x - 1.03x)
 * - Medium drift (150-500ms): Faster speed adjustment (0.93x - 1.07x)
 * - Large drift (>500ms): Hard seek to correct position immediately
 * 
 * Updated to handle typical Chromecast delays and provide tighter synchronization
 * for lip-sync accuracy requirements.
 */
class VideoSyncController(
    private val syncIntervalMs: Long = 100,
    private val verySmallDriftThresholdMs: Long = 50,   // Within acceptable range
    private val smallDriftThresholdMs: Long = 150,      // Minor correction needed
    private val mediumDriftThresholdMs: Long = 500,     // Moderate correction needed
    private val largeDriftThresholdMs: Long = 1000      // Immediate hard seek
) {
    private val TAG = "VideoSyncController"
    private val handler = Handler(Looper.getMainLooper())
    private var isActive = false
    
    // Master clock provider (returns current audio position)
    private var masterClockProvider: (() -> Long)? = null
    
    // Video player to sync
    private var videoPlayer: ExoPlayer? = null
    
    // Track if video is ready to sync
    private var videoPlayerReady: (() -> Boolean)? = null
    
    private var consecutiveLargeDrifts = 0
    private val maxConsecutiveLargeDrifts = 3
    
    private val syncRunnable = object : Runnable {
        override fun run() {
            try {
                if (isActive) {
                    performSync()
                    handler.postDelayed(this, syncIntervalMs)
                }
            } catch (e: Exception) {
                LogSystem.e(TAG, "Sync error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Start continuous synchronization
     */
    fun start(
        videoPlayer: ExoPlayer,
        masterClockProvider: () -> Long,
        videoPlayerReady: () -> Boolean
    ) {
        if (isActive) {
            LogSystem.e(TAG, "Sync already active, stopping previous session")
            stop()
        }
        
        this.videoPlayer = videoPlayer
        this.masterClockProvider = masterClockProvider
        this.videoPlayerReady = videoPlayerReady
        this.isActive = true
        this.consecutiveLargeDrifts = 0
        
        LogSystem.e(TAG, "Video sync started")
        handler.post(syncRunnable)
    }
    
    /**
     * Stop synchronization
     */
    fun stop() {
        isActive = false
        handler.removeCallbacks(syncRunnable)
        
        // Reset video to normal playback speed
        try {
            videoPlayer?.setPlaybackParameters(PlaybackParameters(1.0f))
        } catch (e: Exception) {
            LogSystem.e(TAG, "Error resetting playback speed: ${e.message}")
        }
        
        videoPlayer = null
        masterClockProvider = null
        videoPlayerReady = null
        
        LogSystem.e(TAG, "Video sync stopped")
    }
    
    /**
     * Perform synchronization check and correction
     */
    private fun performSync() {
        val player = videoPlayer ?: return
        val getAudioPosition = masterClockProvider ?: return
        val isVideoReady = videoPlayerReady ?: return
        
        // Only sync if video player is ready and playing
        if (!isVideoReady() || !player.isPlaying) {
            // Reset to normal speed when not playing
            player.setPlaybackParameters(PlaybackParameters(1.0f))
            return
        }
        
        val audioPosition = getAudioPosition()
        val videoPosition = player.currentPosition
        val drift = videoPosition - audioPosition
        
        // Log all drift for comprehensive debugging
        LogSystem.e(TAG, "Sync check: drift=${drift}ms (video=$videoPosition, audio=$audioPosition)")
        
        when {
            // CRITICAL DRIFT: Hard seek required (video is way off - likely initial buffering or major issue)
            abs(drift) > largeDriftThresholdMs -> {
                consecutiveLargeDrifts++
                LogSystem.e(TAG, "*** CRITICAL DRIFT ***: ${drift}ms - Hard seek needed (count: $consecutiveLargeDrifts)")
                
                // Perform hard seek immediately for large drift
                if (consecutiveLargeDrifts >= maxConsecutiveLargeDrifts) {
                    LogSystem.e(TAG, "Performing HARD SEEK to audio position: $audioPosition")
                    player.seekTo(audioPosition)
                    player.setPlaybackParameters(PlaybackParameters(1.0f))
                    consecutiveLargeDrifts = 0
                } else {
                    // Try very aggressive speed adjustment first
                    val speedAdjustment = if (drift > 0) 0.85f else 1.15f
                    LogSystem.e(TAG, "Attempting aggressive speed correction: $speedAdjustment")
                    player.setPlaybackParameters(PlaybackParameters(speedAdjustment))
                }
            }
            
            // MEDIUM DRIFT: Aggressive speed adjustment (typically 150-500ms)
            abs(drift) > mediumDriftThresholdMs -> {
                consecutiveLargeDrifts = 0
                val speedAdjustment = when {
                    drift > 0 -> 0.93f // Video ahead, slow down significantly
                    else -> 1.07f      // Video behind, speed up significantly
                }
                player.setPlaybackParameters(PlaybackParameters(speedAdjustment))
                LogSystem.e(TAG, "Medium drift: ${drift}ms - Speed adjusted to: $speedAdjustment")
            }
            
            // SMALL DRIFT: Gentle speed adjustment (typically 50-150ms)
            abs(drift) > smallDriftThresholdMs -> {
                consecutiveLargeDrifts = 0
                val speedAdjustment = when {
                    drift > 100 -> 0.97f   // Video ahead, slightly slow down
                    drift < -100 -> 1.03f  // Video behind, slightly speed up
                    drift > 0 -> 0.98f     // Video slightly ahead
                    else -> 1.02f          // Video slightly behind
                }
                player.setPlaybackParameters(PlaybackParameters(speedAdjustment))
                LogSystem.e(TAG, "Small drift: ${drift}ms - Gentle speed adjustment: $speedAdjustment")
            }
            
            // VERY SMALL DRIFT: Still in acceptable range
            abs(drift) > verySmallDriftThresholdMs -> {
                consecutiveLargeDrifts = 0
                // Very gentle correction for fine-tuning
                val speedAdjustment = when {
                    drift > 80 -> 0.99f
                    drift < -80 -> 1.01f
                    else -> 1.0f
                }
                player.setPlaybackParameters(PlaybackParameters(speedAdjustment))
            }
            
            // PERFECT SYNC: Normal playback speed (<50ms drift)
            else -> {
                consecutiveLargeDrifts = 0
                player.setPlaybackParameters(PlaybackParameters(1.0f))
            }
        }
    }
    
    /**
     * Check if sync is currently active
     */
    fun isActive(): Boolean = isActive
    
    /**
     * Get current drift (for monitoring)
     */
    fun getCurrentDrift(): Long? {
        val player = videoPlayer ?: return null
        val getAudioPosition = masterClockProvider ?: return null
        return player.currentPosition - getAudioPosition()
    }
}

