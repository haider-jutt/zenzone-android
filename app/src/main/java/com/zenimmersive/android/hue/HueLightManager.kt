package com.zenimmersive.android.hue

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.zenimmersive.android.R
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.model.Light
import com.zenimmersive.android.model.LightListResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException


class HueLightManager(var context: Context) {
    private var LOCK = Any()

    companion object {
        private var hueLightManager: HueLightManager? = null
        fun getInstance(context: Context): HueLightManager {
            synchronized(HueLightManager::class.java)
            {
                if (hueLightManager == null) hueLightManager = HueLightManager(context)
                else {
                    hueLightManager!!.context = context
                    hueLightManager!!.bridgeIp = KeyStorage.getInstance(context).getBridgeIp()
                    hueLightManager!!.userHueApiToken =
                        KeyStorage.getInstance(context).getBridgeHueApiToken()
                }
                return hueLightManager!!
            }
        }

        fun destroyInstance() {
            hueLightManager = null
        }

    }

    var bridgeIp = KeyStorage.getInstance(context).getBridgeIp()
    var userHueApiToken = KeyStorage.getInstance(context).getBridgeHueApiToken()

    var mainHandler = Handler(Looper.getMainLooper())

    var hueTokenClient = HueTokenClient(context)
    var hueLightClient = HueLightClient(context)

    var isBridgeActive = false

    var lightListResult: LightListResult? = null

    fun isBridgeDiscovered(): Boolean = bridgeIp.isNotEmpty()
    fun isBridgeHueApiTokenValid(): Boolean =
        KeyStorage.getInstance(context).getBridgeHueApiToken().isNotEmpty()

    @Synchronized
    fun isBridgeFunctional(): Boolean {
        return isBridgeDiscovered() && isBridgeHueApiTokenValid() && isBridgeActive
    }

    fun refreshBridgeStatus(taskCallback: TaskCallback<String>? = null, force: Boolean = false) {
        if (isBridgeActive && !force) {
            taskCallback?.onTaskComplete(
                KeyStorage.getInstance(context).getBridgeApiVersion(bridgeIp)
            )
            return
        }
        if (isBridgeDiscovered()) {
            LogSystem.e(
                "TAG",
                "System : ${System.currentTimeMillis()} Last : ${
                    KeyStorage.getInstance(context).getLastSyncStateCheckTime()
                } = ${
                    (System.currentTimeMillis() - KeyStorage.getInstance(context)
                        .getLastSyncStateCheckTime())
                }"
            )
            if ((System.currentTimeMillis() - KeyStorage.getInstance(context)
                    .getLastSyncStateCheckTime()) > 10000
            ) {

                KeyStorage.getInstance(context).getBridgeApiVersion("")
                fetchApiConfig(bridgeIp, object : TaskCallback<String> {
                    override fun onTaskError(error: String?) {
                        mainHandler.post { taskCallback?.onTaskError(error) }
                    }

                    override fun onTaskComplete(result: String) {
                        isBridgeActive = true
                        KeyStorage.getInstance(context).setLastSyncStateCheckTime()
                        mainHandler.post { taskCallback?.onTaskComplete(result) }
                    }
                }, "RefreshStatus")
            } else {
                taskCallback?.onTaskComplete(
                    KeyStorage.getInstance(context).getBridgeApiVersion(bridgeIp)
                )
            }
        } else {
            mainHandler.post { taskCallback?.onTaskError(context.getString(R.string.error_no_bridge)) }
        }
    }


    //V2 API 1948086000
    fun discoverBridges(taskCallback: TaskCallback<List<Bridge>>? = null) {
        KeyStorage.getInstance(context).clearBridgeDetails()
        lightListResult = null
        bridgeIp = ""
        userHueApiToken = ""

        var client = OkHttpClient()
        val url = HueOkHttpClient.discoveryURL;
        val request: Request = Request.Builder().url(url).build()


        client.newCall(request).enqueue(responseCallback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                LogSystem.e("URL : $url\nError : ${e.message}")
                mainHandler.post { 
                    taskCallback?.onTaskError(
                        context.getString(R.string.error_bridge_discovery_failed, e.message ?: "Unknown")
                    ) 
                }
            }

            override fun onResponse(call: Call, response: Response) {
                var responseData = response.body?.string()
                LogSystem.e("URL : $url\nResult : $responseData")
                if (response.isSuccessful) {
                    var json = JSONArray(responseData)
                    var typeToken = object : TypeToken<List<Bridge>>() {}
                    var bridges = Gson().fromJson<List<Bridge>>(json.toString(), typeToken.type)

                    if (bridges.isNotEmpty()) {
                        // Try each bridge recursively until we find an active one
                        tryNextBridge(bridges, 0, taskCallback)
                    } else {
                        mainHandler.post { 
                            taskCallback?.onTaskError(context.getString(R.string.error_no_bridge_found)) 
                        }
                    }
                } else {
                    mainHandler.post { taskCallback?.onTaskError(responseData) }
                }
            }

        })
    }

    /**
     * Recursively tries each bridge in the list until an active one is found
     * @param bridges List of discovered bridges
     * @param currentIndex Current bridge index being tested
     * @param taskCallback Callback to notify when active bridge is found or all bridges fail
     */
    private fun tryNextBridge(
        bridges: List<Bridge>,
        currentIndex: Int,
        taskCallback: TaskCallback<List<Bridge>>?
    ) {
        // Base case: If we've tried all bridges, report error
        if (currentIndex >= bridges.size) {
            LogSystem.e("All ${bridges.size} bridge(s) failed to respond. No active bridge found.")
            mainHandler.post { 
                taskCallback?.onTaskError(
                    context.getString(R.string.error_all_bridges_inactive, bridges.size)
                ) 
            }
            return
        }

        val currentBridge = bridges[currentIndex]
        val currentBridgeIp = currentBridge.ipAddress ?: ""
        
        if (currentBridgeIp.isEmpty()) {
            LogSystem.e("Bridge at index $currentIndex has no IP address, trying next...")
            // Skip this bridge and try the next one
            tryNextBridge(bridges, currentIndex + 1, taskCallback)
            return
        }

        LogSystem.e("Testing bridge ${currentIndex + 1}/${bridges.size} at IP: $currentBridgeIp")

        // Try to fetch API config to verify this bridge is active
        fetchApiConfig(currentBridgeIp, object : TaskCallback<String> {
            override fun onTaskComplete(result: String) {
                // Success! This bridge is active
                LogSystem.e("Bridge at $currentBridgeIp is active (API version: $result)")
                bridgeIp = currentBridgeIp
                KeyStorage.getInstance(context).setBridgeIp(bridgeIp)
                mainHandler.post { taskCallback?.onTaskComplete(bridges) }
            }

            override fun onTaskError(error: String?) {
                // This bridge failed, try the next one
                LogSystem.e("Bridge at $currentBridgeIp failed: $error. Trying next bridge...")
                tryNextBridge(bridges, currentIndex + 1, taskCallback)
            }

        }, "BridgeDiscovery#${currentIndex}")
    }

    fun createUser(
        ipAddress: String = bridgeIp,
        taskCallback: TaskCallback<String>?,
        caller: String = "System"
    ) {
        LogSystem.e("Token Generate Caller : $caller")
        if (!isBridgeHueApiTokenValid()) {
            hueTokenClient.generateHueToken(
                ipAddress,
                object : TaskCallback<String> {
                    override fun onTaskComplete(result: String) {
                        fetchLights(object : TaskCallback<LightListResult> {
                            override fun onTaskComplete(lightResult: LightListResult) {
                                taskCallback?.onTaskComplete(result)
                            }

                            override fun onTaskError(error: String?) {
                                taskCallback?.onTaskError(error)
                            }
                        })
                    }

                    override fun onTaskError(error: String?) {
                        taskCallback?.onTaskError(error)
                    }
                },
                caller = "createUser#L165 $caller"
            )
        } else {
            LogSystem.e("Token Already Generated")
            mainHandler.post { taskCallback?.onTaskComplete(userHueApiToken) }
        }
    }

    fun fetchApiConfig(
        ipAddress: String = bridgeIp,
        taskCallback: TaskCallback<String>?,
        caller: String
    ) {
        if (KeyStorage.getInstance(context).getBridgeApiVersion(ipAddress).isNotEmpty()) {
            taskCallback?.onTaskComplete(
                KeyStorage.getInstance(context).getBridgeApiVersion(ipAddress)
            )
            return
        }
        var client = HueOkHttpClient.getInstance(context, ipAddress)
        LogSystem.e("API Config Caller : $caller")
        //Verify that the bridge has software version 1948086000 or higher
        // (check “swversion” in the returned parameter when performing a GET on https://<ip-address>/api/config
        val url = HueOkHttpClient.getConfigurationURL(ipAddress)
        val request: Request =
            Request.Builder().url(url).addHeader("hue-application-key", userHueApiToken).build()

        KeyStorage.getInstance(context).getBridgeApiVersion("0")
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                LogSystem.e("URL : $url\nError : ${e.message}")
                taskCallback?.onTaskError()
            }

            override fun onResponse(call: Call, response: Response) {
                var responseData = response.body?.string()
                LogSystem.e("URL : $url\nResult : $responseData")
                if (response.isSuccessful) {
                    //1948086000
                    var result = JSONObject(responseData)
                    var versionNumber = result.optString("swversion")
                    KeyStorage.getInstance(context).setBridgeApiVersion(ipAddress, versionNumber)
                    taskCallback?.onTaskComplete(versionNumber)
                } else {
                    taskCallback?.onTaskError(context.getString(R.string.error_bridge_not_active))
                }
            }

        })
    }

    fun fetchLightListByCache(): LightListResult? {
        var result = KeyStorage.getInstance(context).getCacheLightListResultJson(bridgeIp)
        LogSystem.e("Cache Light List Invoked >> ${result}")
        if (result != null) {
            try {
                lightListResult = Gson().fromJson(result, LightListResult::class.java)
                return lightListResult
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }

    fun fetchLights(taskCallback: TaskCallback<LightListResult>?, allowCache: Boolean = true) {
        LogSystem.e("Fetch Lights Invoked Token >> ${isBridgeHueApiTokenValid()}")
        try {
            hueLightClient.refreshCache()

            if (!isBridgeHueApiTokenValid()) {
                taskCallback?.onTaskError(context.getString(R.string.error_token_not_found))
            } else {
                LogSystem.e("Token Generated, Fetching Lights Cache Light Result ${(getLightListSyncronized() != null)}")
                if (getLightListSyncronized() != null && allowCache) {
                    taskCallback?.onTaskComplete(getLightListSyncronized()!!)
                    return
                }
                hueLightClient.fetchLightListAsync(object : TaskCallback<LightListResult> {
                    override fun onTaskError(error: String?) {
                        isBridgeActive = false
                        taskCallback?.onTaskError(error)
                    }

                    override fun onTaskComplete(result: LightListResult) {
                        try {
                            if (result.error.isNullOrEmpty()) {
                                lightListResult = result
                                taskCallback?.onTaskComplete(result)
                            } else {
                                taskCallback?.onTaskError(result.error)
                            }
                        } catch (e: Exception) {
                            taskCallback?.onTaskError(e.message)
                        }
                    }
                })
            }
        } catch (e: Exception) {
            taskCallback?.onTaskError(e.message)
        }

    }

    fun saveLightListByCache(result: LightListResult) {
        if (result.error.isNullOrEmpty()) KeyStorage.getInstance(context)
            .setCacheLightListResult(bridgeIp, result)
    }

    fun changeLightColor(light: Light, x: Double, y: Double, brightness: Int) {
        hueLightClient.changeLightColor(light, x, y, brightness)
    }

    fun changeColor() {
        getLightListSyncronized()?.let {
            for (light in it.lights ?: emptyList()) {
                hueLightClient.changeLightColor(light, 0.291, 0.311, 100)
            }
        }
    }

    fun changeColor(color: Int) {
        getLightListSyncronized()?.let {
            for (light in it.lights ?: emptyList()) {
                var xyColor = ColorConverter.rgbToXY(color)
                hueLightClient.changeLightColor(light, xyColor[0], xyColor[1])
            }
        }
    }

    fun changeBrightness(level: Float) {
        LogSystem.e("Change Brightness Invoked $level")
        getLightListSyncronized()?.let {
            for (light in it.lights ?: emptyList()) {
                hueLightClient.changeBrightness(light, level)
            }
        }
    }

    fun stopRequests() {

    }

    fun syncPreferences() {
        bridgeIp = KeyStorage.getInstance(context).getBridgeIp()
        userHueApiToken = KeyStorage.getInstance(context).getBridgeHueApiToken()
    }

    fun clearBridgeDetails() {
        KeyStorage.getInstance(context).clearBridgeDetails();
        syncPreferences()
    }

    fun changeLightState(light: Light, isOn: Boolean) {
        getLightListSyncronized()?.lights?.forEach {
            if (it.id == light.id) {
                it.systemUseCase = isOn
            }
        }
        getLightListSyncronized()?.rooms?.forEach { room ->
            room.roomLightList?.forEach {
                if (it.id == light.id) {
                    it.systemUseCase = isOn
                }
            }
        }
        hueLightClient.changeLightStateAsync(light, isOn)
    }

    fun getWorkingLightCount(): Int {
        var count = 0
        getLightListSyncronized()?.lights?.forEach {
            if (it.systemUseCase == true) count += 1
        }
        return count
    }

    fun revertLightStatesAsync() {
        var temp = getLightListSyncronized()
        LogSystem.e("Revert Light States Invoked : ${temp?.lights?.size}")
        getLightListSyncronized()?.lights?.forEach { light ->
            hueLightClient.changeLightStateAsync(light, false, isRevert = true)
        }
    }

    fun getLightListSyncronized(): LightListResult? {
        synchronized(LOCK) {
            return lightListResult
        }
    }
}