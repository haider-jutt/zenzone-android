package com.zenimmersive.android.hue

import android.content.Context
import android.os.Handler
import com.zenimmersive.android.model.Light

class HueRandomColorHandler(var context: Context) {
    private var handler: Handler? = null

    fun startChangeColor(light: Light) {
        handler = Handler()
        postColorChange(light)
    }

    private fun postColorChange(light: Light) {
        handler?.postDelayed(Runnable {
            var randomColor = ColorConverter.colorToHueColorXY(ColorConverter.generateRandomColor())
            HueLightClient(context!!).changeLightColor(light, randomColor[0],randomColor[1])
            postColorChange(light)
        }, 1000)
    }

    fun stopChangeColor() {
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }

    fun toggleChangeColor(light: Light) {
        if (handler != null)
            stopChangeColor()
        else
            startChangeColor(light)
    }


    companion object {
        private var hueRandomColorHandler: HueRandomColorHandler? = null

        fun getInstance(context: Context): HueRandomColorHandler {
            if (hueRandomColorHandler == null) {
                hueRandomColorHandler = HueRandomColorHandler(context)
            }
            return hueRandomColorHandler!!
        }
    }
}