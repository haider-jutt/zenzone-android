package com.zenimmersive.android.helper

import android.util.Log
import com.zenimmersive.android.BuildConfig

class LogSystem {
    companion object {
        @JvmStatic
        fun e(tag: String = "LogSystem", message: String) {
            if(BuildConfig.DEBUG) Log.e(tag, message)
            //LogManager.getLogManager().writeLog("$tag >> $message")
        }

        @JvmStatic
        fun e(tag: String = "LogSystem", message: String, any: Throwable) {
            if(BuildConfig.DEBUG) Log.e(tag, message, any)
            //LogManager.getLogManager().writeLog("$tag >> $message")
            //LogManager.getLogManager().writeLog(any.stackTraceToString())
        }

        @JvmStatic
        fun d(tag: String = "LogSystem", message: String) {
            if(BuildConfig.DEBUG) Log.e(tag, message)
            //LogManager.getLogManager().writeLog("$tag >> $message")
        }

        @JvmStatic
        fun d(tag: String = "LogSystem", message: String, any: Throwable) {
            if(BuildConfig.DEBUG) Log.e(tag, message, any)
            //LogManager.getLogManager().writeLog("$tag >> $message")
            //LogManager.getLogManager().writeLog(any.stackTraceToString())
        }

        @JvmStatic
        fun LogSystem(tag: String = "LogSystem", message: String) {
            if(BuildConfig.DEBUG) Log.e(tag, message)
            //LogManager.getLogManager().writeLog("$tag >> $message")
        }

        fun LogSystem(tag: String = "LogSystem", message: String, e: Exception) {
            if(BuildConfig.DEBUG) e.printStackTrace()
            //LogManager.getLogManager().writeLog("$tag >> $message")
            //LogManager.getLogManager().writeLog(e.stackTraceToString())
        }

        @JvmStatic
        fun e(message: String) {
            if(BuildConfig.DEBUG) e("LogSystem", message)
        }
    }
}
