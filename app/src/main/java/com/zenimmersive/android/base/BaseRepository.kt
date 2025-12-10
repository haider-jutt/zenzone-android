package com.zenimmersive.android.base

import android.content.Context
import android.content.Intent
import android.util.Log
import com.zenimmersive.android.AppDatabase
import com.zenimmersive.android.helper.CommonUtils
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.ui.AuthenticationOptionActivity
import com.google.gson.GsonBuilder
import com.zenimmersive.android.apiresponsemodel.UserBasicResModel
import com.zenimmersive.android.apiresponsemodel.UserDetailsResModel
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.ui.payment.PurchaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import okio.Buffer
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.StringReader
import java.security.MessageDigest
import java.util.Date
import java.util.TimeZone

abstract class BaseRepository(var context: Context) {
    protected var appDatabase = AppDatabase.getDatabase(context)

    suspend fun <T, R> safeNetworkCall(
        networkCall: Call<T>,
        resModel: R,
        cache: Boolean = false
    ): Resource<R> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, "ApiCache")
                if (!cacheDir.exists()) cacheDir.mkdirs()

                val request = networkCall.request()
                val bodyString = request.body?.let {
                    val buffer = Buffer()
                    it.writeTo(buffer)
                    buffer.readUtf8()
                } ?: ""

                val cacheKey = generateCacheKey(request.url.toString(), request.method, bodyString)
                val cacheFile = File(cacheDir, "$cacheKey.json")

                LogSystem.e(
                    "BaseRepository",
                    "Cache File : " + cacheFile.name + " Exist : ${cacheFile.exists()}"
                )

                // Use cache if exists
                if (cache && cacheFile.exists()) {
                    val cachedResponse = cacheFile.readText()
                    val gson = GsonBuilder().create()
                    val cachedData: R = gson.fromJson(cachedResponse, resModel!!::class.java)
                    return@withContext Success(cachedData)
                }

                val result = networkCall.execute() as Response<T>

                if (result.code() == 401) {
                    val body = CommonUtils.errorBodyMessage(result.errorBody(), context)
                    withContext(Dispatchers.Main) {
                        KeyStorage.getInstance(context).clearAllData()
                        val intent = Intent(context, AuthenticationOptionActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }
                    return@withContext Failure<R>(false, result.code(), errorMessage = body, null)
                } else if (result.code() != 200) {
                    val body = CommonUtils.errorBodyMessage(result.errorBody(), context)
                    return@withContext Failure<R>(false, result.code(), errorMessage = body, null)
                } else {
                    val resp = (result.body() as ResponseBody).string()
                    val gson = GsonBuilder().create()
                    val response: R = gson.fromJson(StringReader(resp), resModel!!::class.java)
                    cacheFile.writeText(gson.toJson(response))
                    return@withContext Success(response)
                }
            } catch (e: HttpException) {
                e.printStackTrace()
                return@withContext Failure<R>(false, e.code(), e.response()?.errorBody())
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Failure<R>(
                    true,
                    0,
                    context.getString(com.zenimmersive.android.R.string.error_internet_connection),
                    null
                )
            }
        }
    }

    fun clearDatabase() {
        appDatabase.clearAllTables()
    }

    private fun generateCacheKey(url: String, method: String, body: String): String {
        val input = "$method|$url|$body"
        return sha256(input)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    protected fun _parseResult(networkResult: Resource<UserBasicResModel>) {
        if (networkResult.status == Status.SUCCESS) {
            var user = (networkResult as Success<UserBasicResModel>).value
            try {
                if (user.result != null) {
                    var userSubscriptionType = (user.result?.userSubscriptionType ?: 1)
                    var orderId = user.result?.orderId
                    var purchaseTime = user.result?.purchaseTime
                    var expireTime = user.result?.expireTime
                    var productId = user.result?.purchasedPackId
                    if (userSubscriptionType > 1) {
                        LogSystem.e("User SubType : $userSubscriptionType")
                        LogSystem.e("User Order ID : $orderId")
                        LogSystem.e("User Purchase Time : $purchaseTime")
                        LogSystem.e("User Expire Time : $expireTime")
                        var purchaseDateTime = CommonUtils.parseUtcTime(purchaseTime)
                        var _expireDateTime =
                            Date(purchaseDateTime.time + PurchaseHelper.toDuration(productId))
                        var expireTimeMillis = _expireDateTime.time
                        if (!expireTime.isNullOrEmpty()) {
                            try {
                                var expireDateTime = CommonUtils.parseUtcTime(expireTime)
                                _expireDateTime = expireDateTime
                                val offset =
                                    TimeZone.getDefault().getOffset(System.currentTimeMillis())
                                expireTimeMillis = _expireDateTime.time + offset
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        LogSystem.e("User Purchase Date Time : $purchaseDateTime")
                        LogSystem.e("User Expire Date Time : $_expireDateTime")

                        KeyStorage.getInstance(context).storeSubscriptionInfo(
                            productId,
                            userSubscriptionType,
                            _expireDateTime.time
                        )
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    protected fun _parseResultUserDetailsResModel(networkResult: Resource<UserDetailsResModel>) {
        if (networkResult.status == Status.SUCCESS) {
            var user = (networkResult as Success<UserDetailsResModel>).value
            try {
                if (user.result != null) {
                    var userSubscriptionType = (user.result?.userSubscriptionType ?: 1)
                    var orderId = user.result?.orderId
                    var purchaseTime = user.result?.purchaseTime
                    var expireTime = user.result?.expireTime
                    var productId = user.result?.purchasedPackId

                    if (userSubscriptionType > 1) {
                        LogSystem.e("User SubType : $userSubscriptionType")
                        LogSystem.e("User Order ID : $orderId")
                        LogSystem.e("User Purchase Time : $purchaseTime")
                        LogSystem.e("User Expire Time : $expireTime")
                        var purchaseDateTime = CommonUtils.parseUtcTime(purchaseTime)
                        var _expireDateTime =
                            Date(purchaseDateTime.time + PurchaseHelper.toDuration(productId))
                        var expireTimeMillis = _expireDateTime.time
                        if (!expireTime.isNullOrEmpty()) {
                            try {
                                var expireDateTime = CommonUtils.parseUtcTime(expireTime)
                                _expireDateTime = expireDateTime
                                val offset =
                                    TimeZone.getDefault().getOffset(System.currentTimeMillis())
                                expireTimeMillis = _expireDateTime.time + offset
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        LogSystem.e("User Purchase Date Time : $purchaseDateTime")
                        LogSystem.e("User Expire Date Time : $_expireDateTime")

                        KeyStorage.getInstance(context).storeSubscriptionInfo(
                            productId,
                            userSubscriptionType,
                            _expireDateTime.time
                        )
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
