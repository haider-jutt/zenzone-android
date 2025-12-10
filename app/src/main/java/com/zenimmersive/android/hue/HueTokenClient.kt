package com.zenimmersive.android.hue

import android.content.Context
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.LogSystem
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class HueTokenClient(var context: Context) {

    fun generateHueToken(
        ipAddress: String,
        taskCallback: TaskCallback<String>?,
        caller: String
    ) {
        var client = HueOkHttpClient.getInstance(context, ipAddress)
        val url = HueOkHttpClient.getApiTokenURL(ipAddress)
        val request: Request = Request.Builder()
            .url(url)
            .post(
                JSONObject().put("devicetype", "HomeHueBridge#AndroidPhone").toString()
                    .toRequestBody()
            )
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                LogSystem.e("URL : $url\nError : ${e.message}")
                taskCallback?.onTaskError("Error ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                var responseData = response.body?.string()
                LogSystem.e("URL : $url\nResult : $responseData")
                if (response.isSuccessful) {
                    try {
                        var result = JSONArray(responseData)
                        var accessToken =
                            result.getJSONObject(0).getJSONObject("success")
                                .getString("username")
                        KeyStorage.getInstance(context).setBridgeHueApiToken(accessToken)
                        taskCallback?.onTaskComplete(accessToken)
                    } catch (e: Exception) {
                        taskCallback?.onTaskError("Error ${e.message}")
                    }
                } else {
                    taskCallback?.onTaskError("Error from bridge")
                }
            }

        })
    }
}