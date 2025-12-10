package com.zenimmersive.android.repository

import android.content.Context
import com.zenimmersive.android.apiresponsemodel.UserBasicResModel
import com.zenimmersive.android.base.BaseRepository
import com.zenimmersive.android.base.RemoteDataSource.Companion.getApiServer
import com.zenimmersive.android.base.Resource
import com.zenimmersive.android.base.ServerApi
import okhttp3.ResponseBody
import retrofit2.Call

class VerificationRepository(context: Context) : BaseRepository(context) {
    val apiInterface: ServerApi = getApiServer()

    suspend fun verificationEmail(userId: Int, userToken: String, otp:String, resModel : UserBasicResModel): Resource<UserBasicResModel> {
        val call: Call<ResponseBody> = apiInterface.verifyOtp(userId, userToken, otp)
        return safeNetworkCall(call, resModel)
    }
}