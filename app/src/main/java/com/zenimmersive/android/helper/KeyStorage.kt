package com.zenimmersive.android.helper

import android.content.Context
import android.content.SharedPreferences
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.helper.Constants.ACCEPT_TERMS
import com.zenimmersive.android.model.LightListResult
import com.google.gson.Gson
import javax.annotation.Nonnull
import org.json.JSONObject
import java.util.concurrent.TimeUnit


class KeyStorage(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("KeyStorage-Default", Context.MODE_PRIVATE)
    private val sharedPreferencesEditor: SharedPreferences.Editor = sharedPreferences.edit()

    companion object {
        private val TAG: String = KeyStorage::class.java.simpleName
        private var prefManager: KeyStorage? = null

        @JvmStatic
        fun getInstance(context: Context = ContextWrapper.getContext()): KeyStorage {
            if (prefManager == null) prefManager = KeyStorage(context)
            return prefManager!!
        }


        private var KEY_PREF_NAME = "data";
        var KEY_USER_TOKEN = "KEY_USER_TOKEN";
        var KEY_USER_ID = "KEY_USER_ID"
        var KEY_LAST_QUOTE_MESSAGE_FR = "KEY_LAST_QUOTE_MESSAGE_FR"
        var KEY_LAST_QUOTE_MESSAGE_EN = "KEY_LAST_QUOTE_MESSAGE_EN"

        @JvmField
        var APP_SELECTED_LANGUAGE = "AppSelectedLanguage"


        @JvmField
        var HOME_PAGE_SERVER_CALL = "HOME_PAGE_SERVER_CALL"

        const val KEY_ACTIVE_SUBSCRIPTION_PRODUCT = "KEY_ACTIVE_SUBSCRIPTION_PRODUCT"
        const val KEY_ACTIVE_SUBSCRIPTION_EXPIRY = "KEY_ACTIVE_SUBSCRIPTION_EXPIRY"
        const val KEY_ACTIVE_SUBSCRIPTION_TYPE = "KEY_ACTIVE_SUBSCRIPTION_TYPE"

    }


    fun getBridgeIp() =
        sharedPreferences
            .getString("hue_bridge_ip", "") ?: ""

    fun setBridgeIp(ip: String) {
        sharedPreferencesEditor
            .putString("hue_bridge_ip", ip)

        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }

    fun getBridgeHueApiToken() =
        sharedPreferences
            .getString("hue_bridge_api_token", "") ?: ""

    fun setBridgeHueApiToken(token: String) {
        sharedPreferencesEditor
            .putString("hue_bridge_api_token", token)

        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }

    fun getBridgeApiVersion(ipAddress: String): String = sharedPreferences
        .getString("hue_bridge_api_${ipAddress.replace(".", "_")}", "") ?: ""

    fun setBridgeApiVersion(ipAddress: String, version: String) {
        sharedPreferencesEditor
            .putString("hue_bridge_api_${ipAddress.replace(".", "_")}", version)

        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }

    var jsonKey = "hue_bridge_json_result_%s"
    fun getCacheLightListResult(ipAddress: String): LightListResult? {
        var json = sharedPreferences
            .getString(jsonKey.format(ipAddress.replace(".", "_")), "")
        if (json.isNullOrEmpty()) return null
        return Gson().fromJson(json, LightListResult::class.java)
    }

    fun getCacheLightListResultJson(ipAddress: String): String? {
        var json = sharedPreferences
            .getString(jsonKey.format(ipAddress.replace(".", "_")), "")
        if (json.isNullOrEmpty()) return null
        return json
    }

    fun setCacheLightListResult(ipAddress: String, lightListResult: LightListResult) {
        sharedPreferencesEditor
            .putString(
                jsonKey.format(ipAddress.replace(".", "_")),
                Gson().toJson(lightListResult)
            )
        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }

    fun clearBridgeDetails() {
        var ipAddress = getBridgeIp().replace(".", "_")
        sharedPreferencesEditor
            .remove("hue_bridge_ip")
            .remove("hue_bridge_api_token")
            .remove("hue_bridge_api_$ipAddress")
            .remove(jsonKey.format(ipAddress))

        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }

    fun clearAllData() {
        var lan = getString(APP_SELECTED_LANGUAGE)
        sharedPreferencesEditor.clear()
        sharedPreferencesEditor.putBoolean(ACCEPT_TERMS, true)
        sharedPreferencesEditor.putString(APP_SELECTED_LANGUAGE, lan)
        sharedPreferencesEditor.apply()
    }

    fun getLastSyncStateCheckTime(): Long {
        var ip = getBridgeIp()
        return sharedPreferences.getLong(
            "hue_bridge_${ip.replace(".", "_")}_last_sync_state_check_time",
            System.currentTimeMillis() - 10001
        )
    }

    fun setLastSyncStateCheckTime() {
        var ip = getBridgeIp()
        sharedPreferencesEditor.putLong(
            "hue_bridge_${ip.replace(".", "_")}_last_sync_state_check_time",
            System.currentTimeMillis()
        )
        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }

    fun getLightState(id: String): Boolean {
        return sharedPreferences.getBoolean("light_state_${id.replace("-", "")}", false)
    }

    fun setLightState(id: String, state: Boolean) {
        sharedPreferencesEditor.putBoolean("light_state_${id.replace("-", "")}", state)
        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }


    var logFileName: String?
        get() {
            var fileName = sharedPreferences.getString("log_file_name", "")
            if (fileName!!.isEmpty()) {
                fileName = "log_manager_" + System.currentTimeMillis() + ".txt"
                logFileName = fileName
            }
            return fileName
        }
        set(fileName) {
            sharedPreferencesEditor.putString("log_file_name", fileName)
            sharedPreferencesEditor.apply()
            sharedPreferencesEditor.commit()
        }

    fun setString(key: String, value: String?) {
        sharedPreferencesEditor.putString(key, value ?: "")
        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }

    fun getString(key: String): String {
        return sharedPreferences!!.getString(key, "")!!
    }

    @Nonnull
    fun getString(key: String, defaultKeyValue: String): String {
        return sharedPreferences.getString(key, defaultKeyValue) ?: defaultKeyValue
    }

    fun setDouble(key: String?, value: Double) {
        sharedPreferencesEditor.putString(key, value.toString() + "")
        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }

    fun getDouble(key: String?): Double? {
        return if (sharedPreferences!!.getString(key, "")!!.length > 0) {
            sharedPreferences!!.getString(key, "")!!.toDouble()
        } else {
            null
        }
    }

    fun setBoolean(key: String?, value: Boolean) {
        sharedPreferencesEditor.putBoolean(key, value)
        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }

    fun getBoolean(key: String?): Boolean {
        return sharedPreferences!!.getBoolean(key, false)
    }


    fun setInt(key: String?, value: Int?) {
        sharedPreferencesEditor.putInt(key, value ?: 0)
        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }

    fun getInt(key: String?): Int {
        return sharedPreferences!!.getInt(key, 0)
    }

    fun cacheMusic(songData: AlbumMusic? = null) {
        songData?.let {
            try {
                if ((songData.isPurchased ?: 0) != 1) {
                    songData.lastTimeMusicPosition = 0
                }
                //Log.e(TAG,"Cache Music Id : ${it.songId}")
                sharedPreferencesEditor.putString("music", Gson().toJson(it))
                sharedPreferencesEditor.apply()
            }
            catch (e : Exception) {}
            catch (e : RuntimeException) {}
        }
    }

    fun removeCacheMusic() {
        sharedPreferencesEditor.remove("music")
        sharedPreferencesEditor.apply()
    }

    fun getCachedMusic(): AlbumMusic? {
        val json = sharedPreferences.getString("music", null)
        return if (json != null) {
            Gson().fromJson(json, AlbumMusic::class.java)
        } else {
            null
        }
    }

    /**
     * Utility function to check if SubscriptionBSFragment should be shown for a song and user.
     * @param context Context
     * @param songData AlbumMusic
     * @return Boolean
     */
    fun shouldShowPaidStatus(context: Context, songData: AlbumMusic): Boolean {
        if(songData.isPaid == 0) return false
        if(songData?.isPurchased == 1) return false
        val userJson = getInstance(context).getString(Constants.USER_DATA, "")
        return shouldShowPaidStatus(songData, userJson)
    }

    fun shouldShowPaidStatus(songData: AlbumMusic?, userJsonString: String): Boolean {
        if((songData?.isPaid ?: 0) == 0) return false
        if(songData?.isPurchased == 1) return false
        var puchased = _shouldShowPaidStatus(songData, userJsonString)
        if(!puchased) {
            songData?.isPurchased = 1
        }
        else {
            songData?.isPurchased = 0
        }
        return puchased
    }

    private fun _shouldShowPaidStatus(
        songData: AlbumMusic?,
        userJsonString: String
    ) : Boolean {
        if(songData == null) return false
        if((songData?.isPaid ?: 0) == 0) return false
        if(songData?.isPurchased == 1) return false
        try {
            if (songData == null) return false
            val userPackageType = getUserSubscriptionTypeFromJson(userJsonString)
            // 1 = Free, 2 = Premium, 3 = Infinite
            var musicPackType = songData.musicPackType ?: 1
            // 1 = Free, 2 = Premium, 3 = Infinite

            if(userPackageType >= musicPackType) return false


            if(songData.isPurchased == 0) {
                var purchasedLocalMusicPackList = KeyStorage.getInstance().getPurchasePackIds()
                if (purchasedLocalMusicPackList.contains("${songData.songId}")) {
                    return false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return (songData?.musicPackType ?: 1) > 1
    }

    fun fetchMaxHueLights(): Int {
        if(Configrations.isTestMode) return 10
        try {
            val userPackageType = getUserSubscriptionTypeFromJson(getString(Constants.USER_DATA, ""))
            // 1 = Free, 2 = Premium, 3 = Infinite
            if (userPackageType == 2) return 10
            else if (userPackageType == 3) return 20
            else return 1
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 3
    }

    //storePurchasePackId
    fun storePurchasePackId(songId: Int?) {
        songId?.let { songId ->
            var idList: Set<String> =
                sharedPreferences.getStringSet("purchasePackIds", mutableSetOf()) ?: mutableSetOf()
            var cloneList = mutableSetOf<String>()
            idList.forEach {
                cloneList.add(it)
            }
            cloneList.add(songId.toString())
            sharedPreferencesEditor.putStringSet("purchasePackIds", cloneList)
            sharedPreferencesEditor.apply()
            sharedPreferencesEditor.commit()
        }
    }

    fun getPurchasePackIds(): Set<String> {
        return sharedPreferences.getStringSet("purchasePackIds", mutableSetOf()) ?: mutableSetOf()
    }

    fun isHomePageServerCallNeed(): Boolean {
        var lastCall = sharedPreferences.getLong(HOME_PAGE_SERVER_CALL, System.currentTimeMillis() - (TimeUnit.MINUTES.toMillis(5)))
        if((System.currentTimeMillis() - lastCall) >= TimeUnit.MINUTES.toMillis(5)) {
            return true
        }
        return false
    }
    fun storeHomePageServerCall() {
        sharedPreferencesEditor.putLong(HOME_PAGE_SERVER_CALL, System.currentTimeMillis())
        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }

    fun storeSubscriptionInfo(productId: String?, subscriptionType: Int, expiryMillis: Long) {
        sharedPreferencesEditor.putString(KEY_ACTIVE_SUBSCRIPTION_PRODUCT, productId ?: "")
        sharedPreferencesEditor.putLong(KEY_ACTIVE_SUBSCRIPTION_EXPIRY, expiryMillis)
        sharedPreferencesEditor.putInt(KEY_ACTIVE_SUBSCRIPTION_TYPE, subscriptionType)
        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }

    fun clearSubscriptionInfo() {
        sharedPreferencesEditor.remove(KEY_ACTIVE_SUBSCRIPTION_PRODUCT)
        sharedPreferencesEditor.remove(KEY_ACTIVE_SUBSCRIPTION_EXPIRY)
        sharedPreferencesEditor.remove(KEY_ACTIVE_SUBSCRIPTION_TYPE)
        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }

    fun getSubscriptionExpiry(): Long {
        return sharedPreferences.getLong(KEY_ACTIVE_SUBSCRIPTION_EXPIRY, 0L)
    }

    fun getActiveSubscriptionProduct(): String {
        return sharedPreferences.getString(KEY_ACTIVE_SUBSCRIPTION_PRODUCT, "") ?: ""
    }

    fun getStoredSubscriptionType(): Int {
        return sharedPreferences.getInt(KEY_ACTIVE_SUBSCRIPTION_TYPE, 0)
    }

    fun isSubscriptionActive(): Boolean {
        val expiry = getSubscriptionExpiry()
        if (expiry <= 0L) return true
        return expiry > System.currentTimeMillis()
    }

    fun updateUserSubscriptionType(newType: Int) {
        val rawJson = getString(Constants.USER_DATA, "")
        if (rawJson.isEmpty()) {
            sharedPreferencesEditor.putInt(KEY_ACTIVE_SUBSCRIPTION_TYPE, newType)
            sharedPreferencesEditor.apply()
            sharedPreferencesEditor.commit()
            return
        }
        try {
            val userObj = JSONObject(rawJson)
            val data = userObj.optJSONObject("data") ?: JSONObject().also { userObj.put("data", it) }
            val result = data.optJSONObject("result") ?: JSONObject().also { data.put("result", it) }
            result.put("userSubscriptionType", newType)
            setString(Constants.USER_DATA, userObj.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            sharedPreferencesEditor.putInt(KEY_ACTIVE_SUBSCRIPTION_TYPE, newType)
            sharedPreferencesEditor.apply()
            sharedPreferencesEditor.commit()
        }
    }

    fun getUserSubscriptionTypeFromJson(userJsonString: String?): Int {
        if (userJsonString.isNullOrEmpty()) return 1
        return try {
            val userObj = JSONObject(userJsonString)
            val userPackageTypeString =
                userObj.optJSONObject("data")?.optJSONObject("result")
                    ?.optString("userSubscriptionType") ?: "1"
            var userPackageType = userPackageTypeString.toIntOrNull() ?: 1
            if (userPackageType > 1 && !isSubscriptionActive()) {
                userPackageType = 1
            }
            userPackageType
        } catch (e: Exception) {
            e.printStackTrace()
            1
        }
    }
    fun getUserKeyFromJson(userJsonString: String?,key:String): String {
        if (userJsonString.isNullOrEmpty()) return ""
        return try {
            val userObj = JSONObject(userJsonString)
            val keyResult =
                userObj.optJSONObject("data")?.optJSONObject("result")
                    ?.optString(key) ?: ""
            keyResult
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun enforceSubscriptionValidity() {
        val storedType = getUserSubscriptionTypeFromJson(getString(Constants.USER_DATA, ""))
        if (storedType == 1 && getStoredSubscriptionType() > 1) {
            clearSubscriptionInfo()
            updateUserSubscriptionType(1)
        }
    }

    fun getEffectiveUserSubscriptionType(): Int {
        return getUserSubscriptionTypeFromJson(getString(Constants.USER_DATA, ""))
    }
    fun getEffectiveUserSubscriptionOrderId(): String {
        return getUserKeyFromJson(getString(Constants.USER_DATA, ""),"orderId")
    }


}
