package com.zenimmersive.android.repository

import android.content.Context
import com.zenimmersive.android.apiresponsemodel.UserBasicResModel
import com.zenimmersive.android.base.BaseRepository
import com.zenimmersive.android.base.RemoteDataSource.Companion.getApiServer
import com.zenimmersive.android.base.Resource
import com.zenimmersive.android.base.ServerApi
import com.zenimmersive.android.base.Status
import com.zenimmersive.android.base.Success
import com.zenimmersive.android.helper.CommonUtils
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.ui.payment.PurchaseHelper
import okhttp3.ResponseBody
import retrofit2.Call
import java.util.Date

class LoginRepository(context: Context) : BaseRepository(context) {
    val apiInterface: ServerApi = getApiServer()

    suspend fun userLogin(
        email: String,
        password: String,
        resModel: UserBasicResModel
    ): Resource<UserBasicResModel> {
        val call: Call<ResponseBody> = apiInterface.loginApi(email, password, KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE).ifEmpty { "en" })
        var networkResult =  safeNetworkCall(call, resModel)
        _parseResult(networkResult)
        return networkResult
    }

    suspend fun userSocialLogin(
        email: String,
        registrationType: String,
        socialToken: String,
        socialId: String,
        userName: String,
        resModel: UserBasicResModel
    ): Resource<UserBasicResModel> {
        val call: Call<ResponseBody> = apiInterface.socialRegisterLogin(
            email, registrationType, socialToken, socialId, userName,
            KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE).ifEmpty { "en" })
        var networkResult =  safeNetworkCall(call, resModel)
        _parseResult(networkResult)
        return networkResult
    }
}