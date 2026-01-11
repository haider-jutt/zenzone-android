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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        val discoveredBridges = java.util.Collections.synchronizedList(ArrayList<Bridge>())
        var nUpnpFinished = false
        var mdnsFinished = false
        var ssdpFinished = false
        var hasReportedResult = false

        // Helper to report results once all methods finish or if we find something
        fun checkAndReportResults() {
            synchronized(LOCK) {
                if (hasReportedResult) return

                if (discoveredBridges.isNotEmpty()) {
                    hasReportedResult = true
                    // Convert to simple list to avoid concurrency issues during iteration
                    val distinctBridges = discoveredBridges.distinctBy { it.ipAddress }
                    tryNextBridge(distinctBridges, 0, taskCallback)
                    return
                }

                if (nUpnpFinished && mdnsFinished && ssdpFinished) {
                    hasReportedResult = true
                    mainHandler.post {
                        taskCallback?.onTaskError(context.getString(R.string.error_no_bridge_found))
                    }
                }
            }
        }

        // 1. Run mDNS Discovery
        val mdnsDiscovery = HueMdnsDiscovery(context)
        mdnsDiscovery.startDiscovery(object : HueMdnsDiscovery.DiscoveryCallback {
            override fun onBridgeFound(bridge: Bridge) {
                discoveredBridges.add(bridge)
            }

            override fun onDiscoveryFinished(bridges: List<Bridge>) {
                LogSystem.e("mDNS Discovery Finished. Found ${bridges.size} bridges.")
                mdnsFinished = true
                checkAndReportResults()
            }

            override fun onError(error: String) {
                LogSystem.e("mDNS Discovery Error: $error")
                mdnsFinished = true
                checkAndReportResults()
            }
        })

        // 2. Run SSDP Discovery
        val ssdpDiscovery = HueSsdpDiscovery(context)
        ssdpDiscovery.startDiscovery(object : HueSsdpDiscovery.DiscoveryCallback {
            override fun onBridgeFound(bridge: Bridge) {
                discoveredBridges.add(bridge)
            }

            override fun onDiscoveryFinished(bridges: List<Bridge>) {
                LogSystem.e("SSDP Discovery Finished. Found ${bridges.size} bridges.")
                ssdpFinished = true
                checkAndReportResults()
            }

            override fun onError(error: String) {
                LogSystem.e("SSDP Discovery Error: $error")
                ssdpFinished = true
                checkAndReportResults()
            }
        })

        // 2. Run N-UPnP Discovery (Original Logic)
        var client = OkHttpClient()
        val url = HueOkHttpClient.discoveryURL;
        val request: Request = Request.Builder().url(url).build()


        client.newCall(request).enqueue(responseCallback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                LogSystem.e("URL : $url\nError : ${e.message}")
                nUpnpFinished = true
                checkAndReportResults()
            }

            override fun onResponse(call: Call, response: Response) {
                var responseData = response.body?.string()
                LogSystem.e("URL : $url\nResult : $responseData")
                if (response.isSuccessful) {
                    try {
                        var json = JSONArray(responseData)
                        var typeToken = object : TypeToken<List<Bridge>>() {}
                        var bridges = Gson().fromJson<List<Bridge>>(json.toString(), typeToken.type)
                        discoveredBridges.addAll(bridges)
                    } catch (e: Exception) {
                         LogSystem.e("N-UPnP Parse Error: ${e.message}")
                    }
                }
                nUpnpFinished = true
                checkAndReportResults()
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
                                ensureZenZoneGroup() // Auto-sync Zone on fresh fetch
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

    private var groupedLightId: String? = null
    private val ZEN_ZONE_NAME = "Zen Immersive – Main Scene"

    fun ensureZenZoneGroup(callback: TaskCallback<String>? = null) {
        if (!isBridgeActive) {
            callback?.onTaskError(context.getString(R.string.error_bridge_not_active))
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Get selected lights
                val selectedLights = getLightListSyncronized()?.lights?.filter { it.systemUseCase == true } ?: emptyList()
                if (selectedLights.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback?.onTaskError("No lights selected")
                    }
                    return@launch
                }

                // 2. Fetch existing zones
                val zones = hueLightClient.getZones()
                var zenZone = zones.find { it.metadata?.name == ZEN_ZONE_NAME }

                // 3. Sync Zone
                if (zenZone == null) {
                    LogSystem.e("HueLightManager", "Creating new Zen Zone: $ZEN_ZONE_NAME with ${selectedLights.size} lights")
                    // Create new zone
                    zenZone = hueLightClient.createZone(ZEN_ZONE_NAME, selectedLights)
                } else {
                    // Check if update needed
                    // Simple check: count and IDs.
                    val currentChildIds = zenZone.children.map { it.rid }.toSet()
                    val selectedIds = selectedLights.map { it.id }.toSet()
                    
                    if (currentChildIds != selectedIds) {
                        LogSystem.e("HueLightManager", "Updating Zen Zone: $ZEN_ZONE_NAME. Lights changed from ${currentChildIds.size} to ${selectedIds.size}")
                        hueLightClient.updateZone(zenZone.id!!, selectedLights)
                        // Re-fetch zone to be sure we have latest services? 
                        // Actually updateZone returns response string, simpler to just use existing object ID and rely on bridge
                    }
                }

                // 4. Resolve Grouped Light ID
                // We need to look for 'grouped_light' service in the zone
                // If we arguably just created it, the local object 'zenZone' has services. 
                // However, createZone return might be null or valid. 
                // If updated, we might need to re-fetch to get services if they weren't in list result?
                // Usually list result includes services.
                
                // Let's re-fetch the specific zone to be 100% sure we have services if we just updated it or if list was partial
                if (zenZone != null) {
                     val zonesRefreshed = hueLightClient.getZones() 
                     zenZone = zonesRefreshed.find { it.metadata?.name == ZEN_ZONE_NAME }
                }

                val groupedLightService = zenZone?.services?.find { it.rtype == "grouped_light" }
                groupedLightId = groupedLightService?.rid
                
                withContext(Dispatchers.Main) {
                    if (groupedLightId != null) {
                        LogSystem.e("HueLightManager", "Grouped Light ID Resolved: $groupedLightId")
                        callback?.onTaskComplete(groupedLightId!!)
                    } else {
                        LogSystem.e("HueLightManager", "Failed to resolve Grouped Light ID")
                        callback?.onTaskError("Failed to resolve Grouped Light ID")
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback?.onTaskError(e.message)
                }
            }
        }
    }

    fun controlGroup(
        on: Boolean,
        brightness: Float?,
        x: Double?,
        y: Double?,
        effect: String?
    ) {
        if (groupedLightId != null) {
            hueLightClient.controlGroupedLight(groupedLightId!!, on, brightness, x, y, effect)
        } else {
            // Fallback to individual light control
            // Note: effect logic for fallback is NOT supported per requirements (or minimal support)
            // Requirements say: "Fallback ... to per-light requests... No change at timeline level"
            // So we just iterate.
            getLightListSyncronized()?.let {
                for (light in it.lights ?: emptyList()) {
                    if (light.systemUseCase == true) {
                        if (x != null && y != null) {
                             hueLightClient.changeLightColor(light, x, y, brightness?.toInt() ?: 100)
                        } else if (brightness != null) {
                             hueLightClient.changeBrightness(light, brightness)
                        }
                    }
                }
            }
        }
    }

    fun getLightListSyncronized(): LightListResult? {
        synchronized(LOCK) {
            return lightListResult
        }
    }
}