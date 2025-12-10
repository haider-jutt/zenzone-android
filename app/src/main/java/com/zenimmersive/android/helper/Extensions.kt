package com.zenimmersive.android.helper


import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale

fun View.hide() {
    this.visibility = View.GONE
}

fun View.show() {
    this.visibility = View.VISIBLE
}


fun String.toKwH(): String {
    try {
        var energy = this.replace(",", ".").toDouble()
        var energyKwh = energy / 1000
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat("0.000", symbols)
        return df.format(energyKwh)
    } catch (e: Exception) {
        return "0.000"
    }
}

fun String.formatKwH(): String {
    try {
        var energy = this.replace(",", ".").toDouble()
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat("0.000", symbols)
        return "${df.format(energy)} [kWh]"
    } catch (e: Exception) {
        return "0.000 [kWh]"
    }
}

fun Double.formatKwH(): String {
    try {
        var energy = this
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat("0.000", symbols)
        return "${df.format(energy)} [kWh]"
    } catch (e: Exception) {
        return "0.000 [kWh]"
    }
}

fun Double.toKwH(): String {
    try {
        var energyKwh = this / 1000
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat("0.000", symbols)
        return df.format(energyKwh)
    } catch (e: Exception) {
        return "0.000"
    }
}


fun Calendar.setDayStart() {
    this.set(Calendar.MINUTE, 0)
    this.set(Calendar.HOUR_OF_DAY, 0)
    this.set(Calendar.SECOND, 0)
}

fun Calendar.setDayEnd() {
    this.set(Calendar.MINUTE, 59)
    this.set(Calendar.HOUR_OF_DAY, 23)
    this.set(Calendar.SECOND, 59)
}

fun String?.toHtmlString(): String {
    var htmlString = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "   <head>\n" +
            "       <style>" +
            "           pre {\n" +
            "               white-space: -moz-pre-wrap; /* Mozilla, supported since 1999 */\n" +
            "               white-space: -pre-wrap; /* Opera */\n" +
            "               white-space: -o-pre-wrap; /* Opera */\n" +
            "               white-space: pre-wrap; /* CSS3 - Text module (Candidate Recommendation) http://www.w3.org/TR/css3-text/#white-space */\n" +
            "               word-wrap: break-word; /* IE 5.5+ */\n" +
            "           }" +
            "       </style>" +
            "   </head>\n" +
            "<body>\n" +
            "${this}" +
            "</body>\n" +
            "</html>"
    Log.e("TAG",htmlString)
    return htmlString
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun String.stripHtmlTags(): String {
    return this.replace(Regex("<.*?>"), "")
}


fun View.setEnabledState(enabled: Boolean) {
    isClickable = enabled
    isEnabled = enabled
    if (enabled) {
        alpha = 1f
    }
}

fun View.fadeIn(duration: Long = 300) {
    clearAnimation()
    animate().alpha(1f).setDuration(duration).start()
    show()
}

fun View.fadeOut(
    duration: Long = 300,
    onEnd: () -> Unit = {},
    hideOnEnd: Boolean = true
) {
    clearAnimation()
    animate().alpha(0f).setDuration(duration).withEndAction {
        if (hideOnEnd) {
            hide()
            alpha = 1f
        }
        onEnd()
    }.start()
}

fun String?.toSeconds(defaultSeconds: Int = 45) : Int {
    if(this.isNullOrEmpty()) return defaultSeconds
    try {
        if(this?.contains(":") == true) {
            val split = this.split(":")
            if(split.size == 2) {
                return split[0].toInt() * 60 + split[1].toInt()
            }
            else if(split.size == 3) {
                return split[0].toInt() * 3600 + split[1].toInt() * 60 + split[2].toInt()
            }
            else {
                return split[0].toInt()
            }
        }
        else {
            return this.toInt()
        }
    }
    catch (e : Exception) {}
    return defaultSeconds
}

fun String.toFile(
    context: Context,
    musicPack: AlbumMusic
): File {
    var rootFolder = File(context.filesDir, "${Configrations.MusicPackFolderName}/${musicPack.songId}")
    var fileName = CommonUtils.extractFileName(this)
    var file = File(rootFolder, fileName)
    return file
}