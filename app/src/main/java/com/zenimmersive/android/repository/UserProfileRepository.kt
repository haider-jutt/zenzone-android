package com.zenimmersive.android.repository

import android.content.Context
import com.zenimmersive.android.apiresponsemodel.UserDetailsResModel
import com.zenimmersive.android.base.BaseRepository
import com.zenimmersive.android.base.RemoteDataSource.Companion.getApiServer
import com.zenimmersive.android.base.Resource
import com.zenimmersive.android.base.ServerApi
import okhttp3.ResponseBody
import retrofit2.Call

class UserProfileRepository(context: Context) : BaseRepository(context) {
    val apiInterface: ServerApi = getApiServer()

    suspend fun fetchUserDetailsAsync(userId: Int, userToken: String, resModel : UserDetailsResModel): Resource<UserDetailsResModel> {
        val call: Call<ResponseBody> = apiInterface.getUserDetails(userId, userToken)
        return safeNetworkCall(call, resModel)
    }

    suspend fun editProfile(userId: Int, userToken: String, userName: String, birthdate: String, email: String, gender:Int, country:String,  resModel:UserDetailsResModel): Resource<UserDetailsResModel> {
        val call: Call<ResponseBody> = apiInterface.updateProfileApi(userId, userToken, userName, birthdate, email, gender, country)
        return safeNetworkCall(call, resModel)
    }
}