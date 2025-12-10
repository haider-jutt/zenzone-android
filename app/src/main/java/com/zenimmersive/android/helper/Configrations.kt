package com.zenimmersive.android.helper

import com.zenimmersive.android.BuildConfig

object Configrations {
    val MusicPackFolderName: String = "DownloadedMusicPackFiles"
    val autoPlayStart: Boolean = false && BuildConfig.DEBUG



    fun isDebugPhone(): Boolean {
        if(BuildConfig.DEBUG) {
            val device = "${android.os.Build.MODEL}-${android.os.Build.MANUFACTURER}-${android.os.Build.ID}"
            if (device == "SM-M515F-samsung-SP1A.210812.016") {
                //Keshav Device
                return true
            }
            else if (device == "RMX3851-realme-UKQ1.231108.001") {
                //Ashvin Realme Phone
                return true
            }
        }
        return false
    }

    var isTestMode = true && isDebugPhone() && BuildConfig.DEBUG
}
