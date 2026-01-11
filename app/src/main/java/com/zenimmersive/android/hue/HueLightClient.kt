package com.zenimmersive.android.hue

import android.content.Context
import com.zenimmersive.android.helper.BackgroundTask
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.model.Color
import com.zenimmersive.android.model.Dimming
import com.zenimmersive.android.model.HueLightListResult
import com.zenimmersive.android.model.HueRoom
import com.zenimmersive.android.model.HueRoomListResult
import com.zenimmersive.android.model.HueZone
import com.zenimmersive.android.model.HueZoneListResult
import com.zenimmersive.android.model.Light
import com.zenimmersive.android.model.LightListResult
import com.zenimmersive.android.model.Xy
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class HueLightClient(var context: Context) {

    private val TAG = "HueLightClient"
    var bridgeIp = KeyStorage.getInstance(context).getBridgeIp()
    var userHueApiToken = KeyStorage.getInstance(context).getBridgeHueApiToken()

    private var client = initClient()

    private fun initClient(): OkHttpClient {
        return HueOkHttpClient.getInstance(context, bridgeIp)
    }

    fun refreshCache() {
        bridgeIp = KeyStorage.getInstance(context).getBridgeIp()
        userHueApiToken = KeyStorage.getInstance(context).getBridgeHueApiToken()
        initClient()
    }

    fun fetchLightListAsync(taskCallback: TaskCallback<LightListResult>? = null) {

        var hueSoftwareVersion = KeyStorage.getInstance(context).getBridgeApiVersion(bridgeIp)
        //if(hueSoftwareVersion.toLong() >= 1948086000L) == V2

        if (userHueApiToken.isNotEmpty()) {

            BackgroundTask.execute("", object : BackgroundTask.Listener<LightListResult> {
                override fun doWork(): LightListResult {
                    var lightListResult = LightListResult()

                    try {
                        var lights = fetchLights()

                        lights.forEach { light ->
                            light?.systemUseCase =
                                KeyStorage.getInstance(context).getLightState(light.id ?: "")
                        }

                        lights.forEach { light ->
                            if (light?.systemUseCase == true) {
                                changeLightStateAsync(light, true)
                            }
                        }

                        var lightMap = hashMapOf<String, Light>()
                        var lightMap2 = hashMapOf<String, Light>()
                        for (light in lights) {
                            light.owner?.rid?.let {
                                lightMap.put(it, light)
                                lightMap2.put(it, light)
                            }
                        }
                        var rooms = fetchRooms()
                        for (room in rooms) {
                            var roomLightList = arrayListOf<Light>()
                            for (child in room.children) {
                                child.rid?.let { regId ->
                                    lightMap.get(regId)?.let {
                                        roomLightList.add(it)
                                        lightMap2.remove(regId)
                                    }
                                }
                            }
                            room.roomLightList = roomLightList
                        }

                        lightListResult.lights = lights
                        lightListResult.rooms = rooms

                        var unConfigLights = arrayListOf<Light>()
                        for (key in lightMap2.keys) {
                            unConfigLights.add(lightMap2.get(key)!!)
                        }
                        lightListResult.unConfigLights = unConfigLights

                        KeyStorage.getInstance(context)
                            .setCacheLightListResult(bridgeIp, lightListResult)

                    } catch (e: Exception) {
                        LogSystem.e("HueClient", "Error ?", e)
                        e.printStackTrace()
                        lightListResult.error = e.message
                    }
                    return lightListResult
                }

                override fun onTaskDone(result: LightListResult) {
                    taskCallback?.onTaskComplete(result)
                }

            })


        } else {
            LogSystem.e("No Light Token!")
            taskCallback?.onTaskError("Token Not Generated!")
        }
    }

    private fun fetchRooms(): List<HueRoom> {
        LogSystem.e(TAG,"Fetch Room List $bridgeIp Invoked")
        val url = HueOkHttpClient.getRoomURL(bridgeIp)
        val request: Request =
            Request.Builder().url(url).addHeader("hue-application-key", userHueApiToken).build()

        var response = client.newCall(request).execute()
        var responseData = response.body?.string()
        LogSystem.e(TAG,"URL : $url\nResult : $responseData")
        if (response.isSuccessful) {
            var result = Gson().fromJson(responseData, HueRoomListResult::class.java)
            return result.data
        }
        return emptyList()
    }

    private fun fetchLights(): List<Light> {
        LogSystem.e(TAG,"Fetch Light List $bridgeIp Invoked")
        val url = HueOkHttpClient.getLightURL(bridgeIp)
        val request: Request =
            Request.Builder().url(url).addHeader("hue-application-key", userHueApiToken).build()

        var response = client.newCall(request).execute()
        var responseData = response.body?.string()
        LogSystem.e(TAG,"URL : $url\nResult : $responseData")
        if (response.isSuccessful) {
            var result = Gson().fromJson(responseData, HueLightListResult::class.java)
            return result.data
        }
        return emptyList()
    }

    internal fun changeLightStateAsync(light: Light, state: Boolean, isRevert: Boolean = false) {
        LogSystem.e(TAG,"Change Light State Invoked Id : ${light.id} State : $state isSystemUseCase : ${light.systemUseCase}")
        val url = HueOkHttpClient.getLightURL(bridgeIp, light.id)
        var params = JSONObject()

        if (state) {
            params.put(
                "color",
                JSONObject().put(
                    "xy",
                    JSONObject().put("x", 0.457).put("y", 0.41)
                )
            )
                .put("dimming", JSONObject().put("brightness", 80.0))

            params.put("on", JSONObject().put("on", state))
        } else {
            params.put(
                "color",
                JSONObject().put(
                    "xy",
                    JSONObject().put("x", light.color?.xy?.x).put("y", light.color?.xy?.y)
                )
            )
                .put("dimming", JSONObject().put("brightness", light.dimming?.brightness ?: 100))
            if (!isRevert) params.put("on", JSONObject().put("on", state))
        }


        processAPI(light, params, object : TaskCallback<Response> {
            override fun onTaskComplete(result: Response) {
                if (state) {
                    light.preBrightness = 80f
                }
            }

            override fun onTaskError(error: String?) {

            }
        }, force = !state)
    }

    fun changeLightColor(light: Light, x: Double, y: Double, brightness: Int) {
        LogSystem.e(TAG,"Change Light Color Invoked X : $x Y : $y Brightness : $brightness \nId : ${light.id} State : ${light.systemUseCase}")
        if (light.systemUseCase == false) return
        if (light?.preState?.xy?.x == x && light?.preState?.xy?.y == y) return
        var params = JSONObject()
            .put(
                "color",
                JSONObject().put(
                    "xy",
                    JSONObject().put("x", x).put("y", y)
                )
            )
            .put("dimming", JSONObject().put("brightness", brightness))
            .put(
                "dynamics", JSONObject().put(
                    "duration", 1000
                )
            );

        processAPI(light, params, object : TaskCallback<Response> {
            override fun onTaskComplete(result: Response) {
                light.preState = Color(Xy(x, y))
            }

            override fun onTaskError(error: String?) {

            }
        })
    }

    fun changeLightColor(light: Light, x: Double, y: Double) {
        LogSystem.e(TAG,"Change Light Color Invoked X : $x Y : $y Id : ${light.id} State : ${light.systemUseCase}")
        if (light.systemUseCase == false) return
        if (light?.preState?.xy?.x == x && light?.preState?.xy?.y == y) return
        var params = JSONObject()
            .put(
                "color",
                JSONObject().put(
                    "xy",
                    JSONObject().put("x", x).put("y", y)
                )
            )
            .put(
                "dynamics", JSONObject().put(
                    "duration", 1000
                )
            );

        processAPI(light, params, object : TaskCallback<Response> {
            override fun onTaskComplete(result: Response) {
                light.preState = Color(Xy(x, y))
            }

            override fun onTaskError(error: String?) {

            }
        })
    }

    private fun processAPI(
        light: Light,
        params: JSONObject,
        taskCallback: TaskCallback<Response>? = null, force: Boolean = false
    ) {
        if (light.systemUseCase == false && !force) return //If its not selected in preference users are not allowed to change
        if (bridgeIp.isEmpty()) return

        val url = HueOkHttpClient.getLightURL(bridgeIp, light.id)
        LogSystem.e(TAG,"URL : $url\nParams : ${params.toString()}")
        val request: Request =
            Request.Builder().url(url)
                .put(

                    params.toString()
                        .toRequestBody()
                )
                .addHeader("hue-application-key", userHueApiToken).build()

        HueOkHttpClient.getInstance().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                LogSystem.e("URL : $url\nError : ${e.message}")
                taskCallback?.onTaskError(e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                var responseData = response.body?.string()
                taskCallback?.onTaskComplete(response)
                // LogSystem.e("URL : $url\nResult : $responseData")
            }

        })
    }

    fun getZones(): List<HueZone> {
        LogSystem.e(TAG, "Fetch Zone List $bridgeIp Invoked")
        val url = HueOkHttpClient.getZoneURL(bridgeIp)
        val request: Request =
            Request.Builder().url(url).addHeader("hue-application-key", userHueApiToken).build()

        var response = client.newCall(request).execute()
        var responseData = response.body?.string()
        LogSystem.e(TAG, "URL : $url\nResult : $responseData")
        if (response.isSuccessful) {
            var result = Gson().fromJson(responseData, HueZoneListResult::class.java)
            return result.data
        }
        return emptyList()
    }

    fun createZone(name: String, lights: List<Light>): HueZone? {
        LogSystem.e(TAG, "Create Zone Invoked Name: $name LightCount: ${lights.size}")
        val url = HueOkHttpClient.getZoneURL(bridgeIp)
        
        val childrenArray = JSONArray()
        lights.forEach { light ->
            childrenArray.put(JSONObject().put("rid", light.id).put("rtype", "light"))
        }

        val jsonBody = JSONObject().apply {
            put("type", "zone")
            put("metadata", JSONObject().put("name", name).put("archetype", "other"))
            put("children", childrenArray)
        }

        val request: Request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody())
            .addHeader("hue-application-key", userHueApiToken)
            .build()

        var response = client.newCall(request).execute()
        var responseData = response.body?.string()
        LogSystem.e(TAG, "Create Zone Result: $responseData")
        
        if (response.isSuccessful) {
            // response might be a list containing the created resource
             var result = Gson().fromJson(responseData, HueZoneListResult::class.java)
             return result.data.firstOrNull()
        }
        return null
    }

    fun updateZone(zoneId: String, lights: List<Light>): String? {
        LogSystem.e(TAG, "Update Zone Invoked ID: $zoneId LightCount: ${lights.size}")
        val url = HueOkHttpClient.getZoneURL(bridgeIp, zoneId)

        val childrenArray = JSONArray()
        lights.forEach { light ->
             // Note: In V2, we link the 'device' or 'light' resource?
             // Document says: { "rid": "LIGHT_ID", "rtype": "light" }
             childrenArray.put(JSONObject().put("rid", light.id).put("rtype", "light"))
        }

        val jsonBody = JSONObject().apply {
            put("type", "zone")
            put("children", childrenArray)
        }
        
        val request: Request = Request.Builder()
            .url(url)
            .put(jsonBody.toString().toRequestBody())
            .addHeader("hue-application-key", userHueApiToken)
            .build()
            
        var response = client.newCall(request).execute()
        return response.body?.string()
    }

    fun controlGroupedLight(
        groupedLightId: String,
        on: Boolean,
        brightness: Float?,
        x: Double?,
        y: Double?,
        effect: String?
    ) {
        val url = HueOkHttpClient.getGroupedLightURL(bridgeIp, groupedLightId)
        val params = JSONObject()

        params.put("on", JSONObject().put("on", on))
        
        if (brightness != null) {
             params.put("dimming", JSONObject().put("brightness", brightness))
        }

        // Effect Logic
        if (!effect.isNullOrEmpty() && effect != "no_effect") {
            params.put("effects_v2", JSONObject().put("action", "dynamic_palette")) // Wait, doc says "action": { "effect": "fire" } for palette, but check doc again.
            // Client Doc says:
            // "effects": { "effect": "fire" } (Wait, looking at doc snippet)
            // "effects_v2": { "action": { "effect": "cosmos" } }
            
            params.put("effects_v2", JSONObject().put("action", JSONObject().put("effect", effect)))
            
            // Check if color seeded
            val isPaletteLocked = listOf("fire", "candle").contains(effect)
            if (!isPaletteLocked && x != null && y != null) {
                 params.put(
                    "color",
                    JSONObject().put(
                        "xy",
                        JSONObject().put("x", x).put("y", y)
                    )
                )
            }
        } else {
            // Normal Color Mode
            if (effect == "no_effect") {
                 params.put("effects_v2", JSONObject().put("action", JSONObject().put("effect", "no_effect")))
            }
            
            if (x != null && y != null) {
                params.put(
                    "color",
                    JSONObject().put(
                        "xy",
                        JSONObject().put("x", x).put("y", y)
                    )
                )
            }
        }
        
        LogSystem.e(TAG, "Control Grouped Light URL: $url Params: $params")

        val request: Request = Request.Builder()
            .url(url)
            .put(params.toString().toRequestBody())
            .addHeader("hue-application-key", userHueApiToken)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                LogSystem.e(TAG, "Group Control Failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                 // LogSystem.e(TAG, "Group Control Success: ${response.code}")
                 response.close()
            }
        })
    }    
    
    fun changeBrightness(light: Light, level: Float) {
        if (light.systemUseCase == false) return
        if (light?.dimming == null) light.dimming = Dimming()
        light.preBrightness = level
        var params = JSONObject()
            .put("dimming", JSONObject().put("brightness", level))
        processAPI(light, params)
    }
}