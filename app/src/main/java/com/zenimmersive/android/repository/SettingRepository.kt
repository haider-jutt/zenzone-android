package com.zenimmersive.android.repository

import android.content.Context
import com.zenimmersive.android.apiresponsemodel.FaqResModel
import com.zenimmersive.android.apiresponsemodel.StaticPagesResModel
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

class SettingRepository(context: Context) : BaseRepository(context) {
    val apiInterface: ServerApi = getApiServer()

    suspend fun logout(userId: Int, userToken: String, resModel : BaseResponse): Resource<BaseResponse> {
        val call: Call<ResponseBody> = apiInterface.logout(userId, userToken)
        return safeNetworkCall(call, resModel)
    }

    suspend fun getFaq(userId: Int, userToken: String, resModel : FaqResModel, lan : String): Resource<FaqResModel> {
        val call: Call<ResponseBody> = apiInterface.getFaq(userId, userToken, lan.ifEmpty { "en" })
        return safeNetworkCall(call, resModel)
    }

    suspend fun staticPages(userId: Int, userToken: String, staticPageId: Int, resModel : StaticPagesResModel): Resource<StaticPagesResModel> {
        val call: Call<ResponseBody> = apiInterface.staticPages(userId, userToken, staticPageId)
        return safeNetworkCall(call, resModel)
    }

    suspend fun deleteAccount(resModel : BaseResponse): Resource<BaseResponse> {
        var userId = KeyStorage.getInstance(context).getInt(KEY_USER_ID)
        var userToken = KeyStorage.getInstance(context).getString(KEY_USER_TOKEN)
        val call: Call<ResponseBody> = apiInterface.deleteAccountApi(userId, userToken)
        return safeNetworkCall(call, resModel)
    }
}