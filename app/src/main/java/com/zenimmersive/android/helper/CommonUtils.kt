package com.zenimmersive.android.helper

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Patterns
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import com.zenimmersive.android.R
import com.google.gson.Gson
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


object CommonUtils {

    private val datePattern = "yyyy-MM-dd HH:mm:ss"
    fun getUtcTime(): String {
        val sdf = SimpleDateFormat(datePattern, Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val purchaseTime = sdf.format(Date())
        return purchaseTime
    }
    fun parseUtcTime(utcString: String?): Date {
        val sdf = SimpleDateFormat(datePattern, Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")  // interpret as UTC
        if(utcString.isNullOrEmpty()) {
            return Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000))
        }
        return sdf.parse(utcString)!!
    }

    fun formatToLocal(date: Date): String {
        val sdf = SimpleDateFormat(datePattern, Locale.ENGLISH)
        sdf.timeZone = TimeZone.getDefault() // your device timezone
        return sdf.format(date)
    }
    @JvmStatic
    fun extractFileName(input: String?): String? {
        return try {
            if(input == null) return null
            val path = if (input.contains("/")) {
                URI(input).path.substringAfterLast('/')
            } else {
                input.substringAfterLast('/')
            }
            path.split(".")[0]
        } catch (e: Exception) {
            e.printStackTrace()
           return null
        }
    }

    private val TAG = "CommonUtils"

    @SuppressLint("MissingPermission")
    fun isNetworkAvailable(context: Context?): Boolean {
        try {
            if (context == null) return false
//        if (BuildConfig.DEBUG) return false
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (connectivityManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val activeNetwork = connectivityManager.activeNetwork ?: return false
                    val networkCapability =
                        connectivityManager.getNetworkCapabilities(activeNetwork)
                            ?: return false
                    if (networkCapability.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return true
                    if (networkCapability.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return true
                    return if (networkCapability.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) true else networkCapability.hasTransport(
                        NetworkCapabilities.TRANSPORT_BLUETOOTH
                    )
                } else {
                    val activeNetworkInfo = connectivityManager.activeNetworkInfo
                    if (activeNetworkInfo != null) { // connected to the internet
                        return activeNetworkInfo.isConnected
                    }
                }
            }
        } catch (ignore: Exception) {

        }
        return false
    }

    fun isValidEmail(email: String?): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun parseDate(time: String?, inputPattern: String?, outputPattern: String?): String? {
        val inputFormat = SimpleDateFormat(inputPattern, Locale.getDefault())
        val outputFormat = SimpleDateFormat(outputPattern, Locale.getDefault())
        var date: Date? = null
        var str: String? = null
        try {
            date = inputFormat.parse(time)
            str = outputFormat.format(date)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return str
    }


    fun getLocalDate(dateString: String?, new_format: String?): String? {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        var value: Date? = null
        var dt = dateString
        try {
            value = formatter.parse(dateString)
            val dateFormatter = SimpleDateFormat(new_format, Locale.getDefault())
            dateFormatter.timeZone = TimeZone.getDefault()
            dt = dateFormatter.format(value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dt
    }

    fun getDateFromMillis(milliSeconds: Long, dateFormat: String = "yyyy-MM-dd"): Date? {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        var strDate = formatter.format(calendar.time)
        return formatter.parse(strDate)
    }

    fun getDateFromString(dateString: String?, dateFormat: String = "yyyy-MM-dd"): Date? {
        writeEvent("$TAG getDateFromString: $dateString")
        try {
            return getDateFromStringOrThrow(dateString, dateFormat)
        } catch (e: Exception) {
            //e.printStackTrace()
        }
        return null
    }


    fun getDateFromStringOrThrow(dateString: String?, dateFormat: String = "yyyy-MM-dd"): Date {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat)
        var date: Date? = null
        var dt = dateString
        date = formatter.parse(dateString)
        val dateFormatter = SimpleDateFormat(dateFormat, Locale.getDefault())
        dateFormatter.timeZone = TimeZone.getDefault()
        dt = dateFormatter.format(date)
        return date
    }

    fun getDateStrFromMillis(milliSeconds: Long, dateFormat: String?): String? {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        var strDate = formatter.format(calendar.time)
        return strDate
    }

    fun isLetters(string: String): Boolean {
        return string.matches(".*[a-zA-Z]+.*".toRegex())
    }

    fun isPhone(string: String): Boolean {
        var temp = string + ""
        temp = temp.replace("+", "").replace(" ", "")
        if (temp.isEmpty()) return false
        return isLetters(temp)
    }

    fun writeEvent(s: String) {
        try {
            LogManager.getLogManager()?.writeLog("EVENT $s")
        } catch (e: Exception) {
            LogSystem.e(TAG, "EVENT $s", e)
        }
    }


    fun convertIt(e: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        e.printStackTrace(pw)
        writeEvent(sw.toString())
        return sw.toString()
    }

    fun errorBodyMessage(errorBody: ResponseBody?, context: Context): String {
        try {
            errorBody?.let {
                var errorJson = JSONObject(it.string())
                if (errorJson.has("error")) {
                    var msg = errorJson.get("error").toString()
                    if (msg.contains("required")) {
                        return context.getString(R.string.server_error_required_values);
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return context.getString(R.string.error_something_went_wrong, e.message)
        }
        return context.getString(R.string.error_something_went_wrong, "#206")
    }

    fun isNumber(powerLimit: String?): Boolean {
        if (powerLimit == null) return false;
        if (powerLimit?.isEmpty() == true) false;
        try {
            powerLimit?.toDouble()
            return true
        } catch (e: Exception) {
        }
        return false;
    }


    interface CustomDialogCallback {
        fun positiveBtnClick(dialog: Dialog)
        fun cancelBtnClick(dialog: Dialog)
    }

    fun customPositiveNegativeDialog(context: Context, title: String, description: String, positiveTitle: String, negativeTitle: String, callBack: CustomDialogCallback): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_layout)

        val lp = dialog.window!!.attributes
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = dialog.findViewById<TextView>(R.id.tvDesc)
        val tvBtnOk = dialog.findViewById<TextView>(R.id.tvBtnOk)
        val tvBtnCancel = dialog.findViewById<TextView>(R.id.tvBtnCancel)

        tvTitle.text = title
        tvDescription.text = description
        tvBtnOk.text = positiveTitle
        tvBtnCancel.text = negativeTitle

        tvBtnOk.setOnClickListener {
            callBack.positiveBtnClick(dialog)
        }
        tvBtnCancel.setOnClickListener {
            callBack.cancelBtnClick(dialog)
        }
        dialog.show()
        return dialog
    }

    fun Any.toJson(): String {
        return Gson().toJson(this)
    }

    inline fun <reified T> String.fromJson(): T {
        return Gson().fromJson(this, T::class.java)
    }

    fun convertToJson(mapData: Map<String, Any>?): String? {
        val gsn = Gson()
        return gsn.toJson(mapData)
    }


    fun setLocale(context: Context) {
        var languageCode =KeyStorage.getInstance(context).getString(KeyStorage.Companion.APP_SELECTED_LANGUAGE)
        setLocale(context, languageCode)
    }
    @Suppress("DEPRECATION")
    fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode.ifEmpty { "en" })
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}