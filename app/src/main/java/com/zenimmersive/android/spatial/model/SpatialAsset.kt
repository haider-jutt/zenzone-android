package com.zenimmersive.android.spatial.model

import android.graphics.Bitmap

/**
 * Represents a spatial image asset with background and optional foreground layers.
 * Used for creating 2.5D parallax effects in ZEN IMMERSIVE.
 */
data class SpatialAsset(
    val id: String,
    val bgUrl: String? = null,           // Wide 16:9 background URL
    val fgUrl: String? = null,           // Optional foreground with alpha URL
    val thumbUrl: String? = null,
    val previewUrl: String? = null,
    val subjectBox: SubjectBox? = null,  // Bounding box for subject
    val parallaxBg: Float = 0.35f,       // Background parallax coefficient
    val parallaxFg: Float = 1.0f,        // Foreground parallax coefficient
    val safeZoom: Float = 1.08f,         // Safe zoom to prevent edge visibility
    val amplitudePx: Int = 40,           // Max motion amplitude in pixels
    val assetVersion: Int = 1,
    
    // For local/demo use - direct bitmap references
    var bgBitmap: Bitmap? = null,
    var fgBitmap: Bitmap? = null
) {
    companion object {
        /**
         * Create a demo asset from local bitmaps
         */
        fun createDemo(
            id: String,
            background: Bitmap?,
            foreground: Bitmap?,
            parallaxBg: Float = 0.3f,
            parallaxFg: Float = 1.0f,
            safeZoom: Float = 1.12f,
            amplitudePx: Int = 50
        ): SpatialAsset {
            return SpatialAsset(
                id = id,
                bgBitmap = background,
                fgBitmap = foreground,
                parallaxBg = parallaxBg,
                parallaxFg = parallaxFg,
                safeZoom = safeZoom,
                amplitudePx = amplitudePx
            )
        }
    }
}

/**
 * Bounding box for the subject in normalized coordinates (0..1)
 */
data class SubjectBox(
    val x: Float,      // Left position (0..1)
    val y: Float,      // Top position (0..1)
    val w: Float,      // Width (0..1)
    val h: Float       // Height (0..1)
)
