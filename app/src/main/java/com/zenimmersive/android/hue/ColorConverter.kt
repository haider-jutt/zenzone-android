package com.zenimmersive.android.hue

import android.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ColorConverter {

    fun xyToRGB(x: Double, y: Double, brightness: Double): Int {
        // Ensure x and y are within the valid range
        if (x < 0 || x > 1 || y < 0 || y > 1) {
            return Color.WHITE
        }

        // Calculate z
        val z = 1.0 - x - y

        // Y is the brightness (luminance)
        val Y = (brightness / 100.0)

        // Assuming the white point
        val X = (Y / y) * x
        val Z = (Y / y) * z

        // Convert to RGB using the conversion matrix for sRGB
        var r = X * 1.656492 - Y * 0.354851 - Z * 0.255038
        var g = -X * 0.707196 + Y * 1.655397 + Z * 0.036152
        var b = X * 0.051713 - Y * 0.121364 + Z * 1.011530

        // Normalize if values are out of bounds
        if (r > 1.0 || g > 1.0 || b > 1.0) {
            val maxValue = max(r, max(g, b))
            r /= maxValue
            g /= maxValue
            b /= maxValue
        }

        // Apply gamma correction (sRGB gamma correction)
        r = if (r > 0.0031308) (1.055 * r.pow(1.0 / 2.4) - 0.055) else (r * 12.92)
        g = if (g > 0.0031308) (1.055 * g.pow(1.0 / 2.4) - 0.055) else (g * 12.92)
        b = if (b > 0.0031308) (1.055 * b.pow(1.0 / 2.4) - 0.055) else (b * 12.92)

        // Clamp values to the range [0, 1]
        r = min(max(r, 0.0), 1.0)
        g = min(max(g, 0.0), 1.0)
        b = min(max(b, 0.0), 1.0)

        // Convert to 0-255 range and return as Android Color Int
        return Color.rgb((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }


    fun rgbToXY(color: Int): DoubleArray {
        // Extract RGB components and normalize them to [0, 1]
        var r = Color.red(color) / 255.0
        var g = Color.green(color) / 255.0
        var b = Color.blue(color) / 255.0

        // Apply inverse gamma correction (sRGB gamma correction)
        r = if (r > 0.04045) ((r + 0.055) / 1.055).pow(2.4) else (r / 12.92)
        g = if (g > 0.04045) ((g + 0.055) / 1.055).pow(2.4) else (g / 12.92)
        b = if (b > 0.04045) ((b + 0.055) / 1.055).pow(2.4) else (b / 12.92)

        // Convert to XYZ color space using the sRGB transformation matrix
        val X = r * 0.4124 + g * 0.3576 + b * 0.1805
        val Y = r * 0.2126 + g * 0.7152 + b * 0.0722
        val Z = r * 0.0193 + g * 0.1192 + b * 0.9505

        // Calculate xy chromaticity coordinates
        val sum = X + Y + Z
        val x = if (sum != 0.0) X / sum else 0.0
        val y = if (sum != 0.0) Y / sum else 0.0

        return doubleArrayOf(x, y)
    }




    fun colorToHueColorXY(color: Int): DoubleArray {
        return rgbToXY(
            color
        )
    }

    fun generateRandomColor(): Int {
        var red = (0..255).random()
        var green = (0..255).random()
        var blue = (0..255).random()
        return Color.rgb(red, green, blue)
    }
}
