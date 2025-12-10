package com.zenimmersive.android.repository

import android.content.Context
import com.zenimmersive.android.apiresponsemodel.QuoteMessageResModel
import com.zenimmersive.android.apiresponsemodel.UserDetailsResModel
import com.zenimmersive.android.base.BaseRepository
import com.zenimmersive.android.base.RemoteDataSource.Companion.getApiServer
import com.zenimmersive.android.base.Resource
import com.zenimmersive.android.base.ServerApi
import okhttp3.ResponseBody
import retrofit2.Call

class SplashRepository(context: Context) : BaseRepository(context) {
    val apiInterface: ServerApi = getApiServer()

    suspend fun getQuoteMessage(resModel : QuoteMessageResModel): Resource<QuoteMessageResModel> {
        val call: Call<ResponseBody> = apiInterface.getQuoteMessage()
        return safeNetworkCall(call, resModel)
    }

    suspend fun fetchUserDetailsAsync(userId: Int, userToken: String, resModel : UserDetailsResModel): Resource<UserDetailsResModel> {
        val call: Call<ResponseBody> = apiInterface.getUserDetails(userId, userToken)
        var networkResult = safeNetworkCall(call, resModel)
        _parseResultUserDetailsResModel(networkResult)
        return networkResult
    }

}