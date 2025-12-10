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
 * - Small drift (<30ms): Adjust playback speed slightly (0.98x - 1.02x)
 * - Medium drift (30-150ms): Faster speed adjustment (0.95x - 1.05x)
 * - Large drift (>150ms): Hard seek to correct position
 */
class VideoSyncController(
    private val syncIntervalMs: Long = 100,
    private val smallDriftThresholdMs: Long = 30,
    private val mediumDriftThresholdMs: Long = 150,
    private val largeDriftThresholdMs: Long = 300
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
        
        // Log significant drift for debugging
        if (abs(drift) > smallDriftThresholdMs) {
            LogSystem.e(TAG, "Drift detected: ${drift}ms (video=$videoPosition, audio=$audioPosition)")
        }
        
        when {
            // CRITICAL DRIFT: Hard seek required (video is way off)
            abs(drift) > largeDriftThresholdMs -> {
                consecutiveLargeDrifts++
                LogSystem.e(TAG, "LARGE DRIFT: ${drift}ms - Performing hard seek (count: $consecutiveLargeDrifts)")
                
                // Only perform hard seek if drift persists
                if (consecutiveLargeDrifts >= maxConsecutiveLargeDrifts) {
                    player.seekTo(audioPosition)
                    player.setPlaybackParameters(PlaybackParameters(1.0f))
                    consecutiveLargeDrifts = 0
                } else {
                    // Try aggressive speed adjustment first
                    val speedAdjustment = if (drift > 0) 0.90f else 1.10f
                    player.setPlaybackParameters(PlaybackParameters(speedAdjustment))
                }
            }
            
            // MEDIUM DRIFT: Aggressive speed adjustment
            abs(drift) > mediumDriftThresholdMs -> {
                consecutiveLargeDrifts = 0
                val speedAdjustment = when {
                    drift > 0 -> 0.95f // Video ahead, slow down
                    else -> 1.05f      // Video behind, speed up
                }
                player.setPlaybackParameters(PlaybackParameters(speedAdjustment))
                LogSystem.e(TAG, "Medium drift: ${drift}ms - Speed: $speedAdjustment")
            }
            
            // SMALL DRIFT: Gentle speed adjustment
            abs(drift) > smallDriftThresholdMs -> {
                consecutiveLargeDrifts = 0
                val speedAdjustment = when {
                    drift > 20 -> 0.98f   // Video ahead, slightly slow down
                    drift < -20 -> 1.02f  // Video behind, slightly speed up
                    else -> 1.0f          // Within acceptable range
                }
                player.setPlaybackParameters(PlaybackParameters(speedAdjustment))
            }
            
            // PERFECT SYNC: Normal playback speed
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

