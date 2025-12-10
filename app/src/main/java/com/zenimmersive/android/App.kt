package com.zenimmersive.android

import androidx.multidex.MultiDexApplication
import com.zenimmersive.android.helper.ContextWrapper
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import com.zenimmersive.android.ui.payment.PurchaseHelper
import com.zenimmersive.android.ui.player.AppCastManager
import java.util.Locale

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        ContextWrapper.saveContext(this)
        PurchaseHelper.getInstance(this)

        AppCastManager.setupCastSession(this)

        // If not en and fr then default en or fr
        var languageCode = when (Locale.getDefault().language.lowercase(Locale.ROOT)) {
            "en", "en-us", "en-gb", "en-au","en-in","en-ca" -> "en"
            else -> "fr" // default
        }

        var _appLanguageCode = KeyStorage.getInstance(this).getString(APP_SELECTED_LANGUAGE)
        if (_appLanguageCode.isEmpty()) {
            KeyStorage.getInstance(this).setString(APP_SELECTED_LANGUAGE, languageCode)
        }
        //KeyStorage.getInstance(this).removeCacheMusic()
    }

}
