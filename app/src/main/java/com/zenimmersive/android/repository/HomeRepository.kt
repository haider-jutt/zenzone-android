package com.zenimmersive.android.repository

import android.content.Context
import com.zenimmersive.android.apiresponsemodel.HomeDataResModel
import com.zenimmersive.android.base.BaseRepository
import com.zenimmersive.android.base.RemoteDataSource.Companion.getApiServer
import com.zenimmersive.android.base.Resource
import com.zenimmersive.android.base.ServerApi
import com.zenimmersive.android.helper.CommonUtils
import okhttp3.ResponseBody
import retrofit2.Call

class HomeRepository(context: Context) : BaseRepository(context) {
    val apiInterface: ServerApi = getApiServer()

    suspend fun homeData(userId: Int, userToken: String, resModel : HomeDataResModel,cache: Boolean = false): Resource<HomeDataResModel> {
        val call: Call<ResponseBody> = apiInterface.homeApi(userId, userToken)
        return safeNetworkCall(call, resModel, (if(cache) true else !CommonUtils.isNetworkAvailable(context)))
    }

}