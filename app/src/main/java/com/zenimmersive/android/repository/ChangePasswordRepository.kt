package com.zenimmersive.android.repository

import android.content.Context
import com.zenimmersive.android.base.BaseRepository
import com.zenimmersive.android.base.BaseResponse
import com.zenimmersive.android.base.RemoteDataSource.Companion.getApiServer
import com.zenimmersive.android.base.Resource
import com.zenimmersive.android.base.ServerApi
import okhttp3.ResponseBody
import retrofit2.Call

class ChangePasswordRepository(context: Context) : BaseRepository(context) {
    val apiInterface: ServerApi = getApiServer()

    suspend fun changePassword(urlUserToken: String, newPassword: String, resModel : BaseResponse): Resource<BaseResponse> {
        val call: Call<ResponseBody> = apiInterface.changePassword(urlUserToken, newPassword)
        return safeNetworkCall(call, resModel)
    }
}