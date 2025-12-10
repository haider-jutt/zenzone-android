package com.zenimmersive.android.hue

import android.animation.ObjectAnimator
import android.content.Context
import android.util.Log
import android.view.View
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.helper.MusicPackDownloader
import com.zenimmersive.android.helper.toFile
import com.zenimmersive.android.ui.player.PlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

object HueColorManager {

    private val colorIntervals = mutableListOf<ColorInterval>()
    var isLoaded = false
    var colorList = arrayListOf<String>()


    fun swapColors() {
        var random = java.util.Random()
        colorIntervals.forEach {
            var index = random.nextInt(colorList.size)
            it.x = colorIntervals[index].x
            it.y = colorIntervals[index].y
            it.brightness = colorIntervals[index].brightness
        }
    }

    private var lastInterval: ColorInterval? = null
    fun findColorByPlayerPosition(playerPositionMillis: Long): ColorInterval {
        try {
            if (isLoaded) lastInterval?.let {
                if (playerPositionMillis in it.startTime..it.endTime) return it
            }
            if (isLoaded) {
                val found = binarySearchInterval(playerPositionMillis)
                lastInterval = found
                return lastInterval!!
            }
        } catch (e: Exception) {
        }
        return ColorInterval(0L, 0L, "0.3127", "0.3290", "100")
    }

    fun binarySearchInterval(playerPositionMillis: Long): ColorInterval {
        var low = 0
        var high = colorIntervals.lastIndex

        if (playerPositionMillis >= colorIntervals.last().endTime) {
            return colorIntervals.last()
        }

        while (low <= high) {
            val mid = (low + high) / 2
            val interval = colorIntervals[mid]

            when {
                playerPositionMillis < interval.startTime -> {
                    high = mid - 1
                }

                playerPositionMillis > interval.endTime -> {
                    low = mid + 1
                }

                else -> {
                    // playerPositionMillis is within interval
                    return interval
                }
            }
        }

        if (colorIntervals.isNotEmpty()) return colorIntervals.last()
        // Default if not found
        return ColorInterval(0L, 0L, "0.3127", "0.3290", "100")
    }

    fun changeColor(context: Context, playerPositionMillis: Long) {
        var hueLightManager = HueLightManager.getInstance(context)
        changeColor(hueLightManager, playerPositionMillis)
    }

    var lastColor: ColorInterval? = null
    fun changeColor(
        hueLightManager: HueLightManager,
        playerPositionMillis: Long,
        view: View? = null
    ) {
        if (hueLightManager.isBridgeActive && isLoaded) {
            CoroutineScope(Dispatchers.IO).launch {
                var colorXY = localViewColor ?: findColorByPlayerPosition(playerPositionMillis)
                //changeViewColorDirectly(view, playerPositionMillis, colorXY)
                LogSystem.e(
                    "HueColorManager",
                    "Position : $playerPositionMillis Color :" + colorXY.toString() + " PreviousColor : " + lastColor?.toString()
                )
                if ((lastColor?.startTime?.equals(colorXY.startTime) == true) && (lastColor?.endTime?.equals(
                        colorXY.endTime
                    ) == true)
                ) {
                    return@launch
                }

                hueLightManager.getLightListSyncronized()?.let {
                    var colorChange = false
                    (it.lights ?: emptyList()).forEach {
                        if (it.systemUseCase == true) {
                            hueLightManager.changeLightColor(
                                it,
                                colorXY.x.toDouble(),
                                colorXY.y.toDouble(),
                                PlayerManager.getInstance()?.staticBrightness
                                    ?: colorXY.brightness.toInt()
                            )
                            colorChange = true
                        }
                    }

                    if (colorChange) {
                        lastColor = colorXY
                    }
                }

            }

        }
    }

    private fun changeViewColor(
        view: View,
        prevColor: Int,
        color: Int,
        animationDuration: Long = 1000
    ) {
        // Animator for the color fade
        view?.clearAnimation()
        val colorFade = ObjectAnimator.ofArgb(view, "backgroundColor", prevColor, color).apply {
            duration = animationDuration // Animation duration in milliseconds
        }
        // Start the animation
        colorFade.start()
    }


    var localViewColor: ColorInterval? = null
    public fun changeViewColorDirectly(view: View? = null, playerPositionMillis: Long) {
        //LogSystem.e("changeViewColorDirectly Invoked : " + playerPositionMillis + " Visible : ${view?.isVisible}")
        if (view == null) return
        CoroutineScope(Dispatchers.IO).launch {
            var colorXY = findColorByPlayerPosition(playerPositionMillis)

            if ((localViewColor?.startTime?.equals(colorXY.startTime) == true) && (localViewColor?.endTime?.equals(
                    colorXY.endTime
                ) == true)
            ) {
                return@launch
            }

//            LogSystem.e(
//                "changeViewColorDirectly Color : " +colorXY.toString() + " PreviousColor : " + localViewColor?.toString()
//            )

            val prevColor = ColorConverter.xyToRGB(
                localViewColor?.x?.toDouble() ?: 2.0,
                localViewColor?.y?.toDouble() ?: 2.0,
                localViewColor?.brightness?.toDouble() ?: 1.0
            )
            localViewColor = colorXY

            CoroutineScope(Dispatchers.Main).launch {

                view?.let {
                    var color = ColorConverter.xyToRGB(
                        colorXY.x.toDouble(),
                        colorXY.y.toDouble(),
                        colorXY.brightness.toDouble()
                    )
                    changeViewColor(it!!, prevColor, color)
                }
            }
        }
    }

    public fun changeViewColorDirectlyNoThread(view: View? = null, playerPositionMillis: Long) {
        //LogSystem.e("changeViewColorDirectlyNoThread Invoked : " + playerPositionMillis)
        if (view == null) return
        var colorXY = findColorByPlayerPosition(playerPositionMillis)
//        LogSystem.e(
//            "changeViewColorDirectlyNoThread Color " + colorXY.toString() + " PreviousColor : " + localViewColor?.toString()
//        )

        val prevColor = ColorConverter.xyToRGB(
            localViewColor?.x?.toDouble() ?: 2.0,
            localViewColor?.y?.toDouble() ?: 2.0,
            localViewColor?.brightness?.toDouble() ?: 1.0
        )
        localViewColor = colorXY


        view?.let {
            var color = ColorConverter.xyToRGB(
                colorXY.x.toDouble(),
                colorXY.y.toDouble(),
                colorXY.brightness.toDouble()
            )
            //it.clearAnimation()
            //it.setBackgroundColor(color)
            changeViewColor(it!!, prevColor, color, 10L)
        }
    }

    private fun timeStringToMillis(time: String): Long {
        val parts = time.split(":")
        return when (parts.size) {
            2 -> (parts[0].toLong() * 60 + parts[1].toLong()) * 1000
            3 -> ((parts[0].toLong() * 3600) + (parts[1].toLong() * 60) + parts[2].toLong()) * 1000
            else -> 0
        }
    }

    fun clearColor() {
        lastColor = null
        localViewColor = null
    }

    fun loadColorData(context: Context, musicPack : AlbumMusic) {
        lastColor = null
        localViewColor = null
        var fileURL = musicPack.getHueFile(context)
        if (fileURL.endsWith("default-user.jpg")) {
            colorList.clear()
            isLoaded = true
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            // Use MusicPackDownloader to find local file or download if needed
            var localFile = MusicPackDownloader.findLocalFile(context, musicPack, fileURL)?.let {
                if(it.isNotEmpty() && !it.startsWith("http")) File(it)
                else null
            } ?: fileURL.toFile(context, musicPack)
            
            LogSystem.e("TAG","Loading Hue Color File URL : ${fileURL}\nLocalFile : ${localFile.path}")

            //Read File URL, Text File
            var colorData: String =  ""
            /* = "" +
                    "00:00,00:05,0.561,0.348,255\n" +
                    "00:05,00:08,0.352,0.508,255\n" +
                    "00:08,00:12,0.212,0.121,255\n" +
                    "00:12,00:20,0.417,0.504,255\n" +
                    "00:20,00:25,0.210,0.355,100\n" +
                    "00:25,00:30,0.463,0.465,255\n" +
                    "00:30,00:33,0.321,0.262,255\n" +
                    "00:33,00:40,0.182,0.094,255\n" +
                    "00:40,00:45,0.311,0.328,255\n" +
                    "00:45,00:50,0.311,0.328,1\n" +
                    "00:50,00:55,0.243,0.309,255\n" +
                    "00:55,00:60,0.309,0.582,127"*/

            try {
                if(localFile.exists()) {
                    try {
                        colorData = localFile.readText()
                    } catch (e: Exception) {
                        colorData = ""
                        try {
                            localFile.delete()
                        } catch (e: Exception) {
                        }
                    }
                }
                else {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(fileURL).build()
                    val response = client.newCall(request).execute()
                    if(!localFile.exists()) {
                        localFile.parentFile?.mkdirs()
                        localFile.createNewFile()
                    }
                    response.body?.bytes()?.let { bytes->
                        localFile.writeBytes(bytes)
                        try {
                            colorData = localFile.readText()
                        } catch (e: Exception) {
                            colorData = ""
                            try {
                                localFile.delete()
                            } catch (e: Exception) {
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                colorData = ""
            }

            colorIntervals.clear()
            colorData.split("\n").forEach { line ->
                LogSystem.e("HueColor", "Line : ${line}")
                try {
                    if (!line.trim().isNullOrBlank()) {

                        val parts = line.split(",")
                        val startTime = convertTimeToSeconds(parts[0])
                        val endTime = convertTimeToSeconds(parts[1])
                        val x = parts[2]
                        val y = parts[3]
                        Log.d("HueColor", "loadColorData: ${parts[4]}")
//                        val brightness = if (parts[4].contains("\r")) {
//                            ((parts[4].replace("\r", "")).toInt() / 255.0).toString()
//                        } else {
//                            ((parts[4].toInt() / 255.0)).toString()
//                        }
                        val brightness =
                            if (parts[4].contains("\r")) parts[4].replace("\r", "") else parts[4]

                        colorIntervals.add(
                            ColorInterval(
                                startTime = startTime,
                                endTime = endTime,
                                x = x,
                                y = y,
                                brightness = brightness
                            )
                        )
                    }

                    val sortedColorIntervals = colorIntervals.sortedBy { it.startTime }
                    colorIntervals.clear()
                    colorIntervals.addAll(sortedColorIntervals)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.Main) {
                isLoaded = true
            }
        }


    }

    // Helper function to convert time format "MM:SS" to seconds
    fun convertTimeToSeconds(time: String): Long {
        val (minutes, seconds) = time.split(":").map { it.toLong() }
        return (minutes * 60 + seconds) * 1000L
    }

    data class ColorInterval(
        val startTime: Long,
        val endTime: Long,
        var x: String,
        var y: String,
        var brightness: String
    ) {
        override fun toString(): String {
            return "ColorInterval($startTime, $endTime, $x, $y, $brightness)"
        }
    }
}
