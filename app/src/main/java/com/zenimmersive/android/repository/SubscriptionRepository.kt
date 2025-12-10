package com.zenimmersive.android.repository

import android.content.Context
import com.zenimmersive.android.base.BaseRepository
import com.zenimmersive.android.base.BaseResponse
import com.zenimmersive.android.base.RemoteDataSource.Companion.getApiServer
import com.zenimmersive.android.base.Resource
import com.zenimmersive.android.base.ServerApi
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_ID
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_TOKEN
import okhttp3.ResponseBody
import retrofit2.Call

class SubscriptionRepository(context: Context) : BaseRepository(context) {
    private val apiInterface: ServerApi = getApiServer()

    suspend fun handleUserPurchase(purchasedPackId: String,
                                   purchasedPackType: Int,
                                   purchaseTime: String,
                                   purchasePackDuration: Int,
                                   songId: Int,
                                   purchaseToken: String,
                                   orderId: String,price : String, resModel : BaseResponse): Resource<BaseResponse> {
        val userId = KeyStorage.getInstance(context).getInt(KEY_USER_ID)
        val userToken = KeyStorage.getInstance(context).getString(KEY_USER_TOKEN)
        val call: Call<ResponseBody> = apiInterface.handleUserPurchase(userId, userToken,  purchasedPackId, purchasedPackType, purchaseTime, purchasePackDuration, songId, purchaseToken,price,orderId)
        return safeNetworkCall(call, resModel)
    }

} 