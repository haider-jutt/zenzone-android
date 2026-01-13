ZEN IMMERSIVE - Spatial Demo Setup
===================================

This folder contains assets for the Spatial Image Demo.

TO ADD YOUR DEMO IMAGE:
-----------------------

1. Save your foreground image (person/subject with transparent background) as:
   
   foreground.png
   
   Place it in this folder: app/src/main/assets/spatial_demo/

2. (Optional) Add a custom background image as:
   
   background.png
   
   If not provided, a beautiful gradient will be generated automatically.

IMAGE REQUIREMENTS:
-------------------

FOREGROUND (foreground.png):
- Format: PNG with transparency (alpha channel)
- Recommended size: 600-1200px width
- The subject should be on transparent background
- For best effect, use images of people, objects, or characters
- The image will be scaled to fit the viewport

BACKGROUND (background.png) - Optional:
- Format: PNG or JPG
- Recommended aspect ratio: 16:9 (wide)
- Will be used as the parallax background
- If not provided, a gradient is auto-generated

HOW THE EFFECT WORKS:
---------------------

1. Background layer moves SLIGHTLY when you tilt your phone (parallaxBg = 0.25)
2. Foreground layer moves MORE when you tilt your phone (parallaxFg = 1.0)
3. This creates a 2.5D depth illusion
4. When device is still for ~1.2 seconds, "idle drift" kicks in
5. Idle drift creates gentle, organic movement

TO LAUNCH THE DEMO:
-------------------

1. Run the app
2. Go to Settings tab
3. Tap "Spatial Demo" (marked as NEW)

Or programmatically:
   startActivity(Intent(context, SpatialDemoActivity::class.java))

CONFIGURATION:
--------------

You can customize the spatial effect by modifying SpatialConfig in:
app/src/main/java/com/zenimmersive/android/spatial/model/SpatialConfig.kt

Key parameters:
- motionSensitivity: How responsive to device tilt (default: 1.0)
- idleDriftAmplitude: How much drift when idle (default: 0.15)
- stillnessThreshold: Motion threshold for detecting stillness (default: 0.02)
- stillnessDurationMs: Time before idle drift starts (default: 1200ms)

TROUBLESHOOTING:
----------------

1. "Placeholder - Add your image to assets" appears:
   → You haven't added foreground.png to this folder

2. Image doesn't move:
   → Device sensors may be unavailable
   → Try "Force Drift" button to see procedural motion

3. Motion feels too sensitive/sluggish:
   → Adjust motionSensitivity in SpatialConfig

4. Edge of image visible during motion:
   → Increase safeZoom in SpatialAsset (default: 1.15)
