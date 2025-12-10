package com.zenimmersive.android.repository

import android.content.Context
import com.zenimmersive.android.apiresponsemodel.AlbumDetailsResModel
import com.zenimmersive.android.base.BaseRepository
import com.zenimmersive.android.base.RemoteDataSource.Companion.getApiServer
import com.zenimmersive.android.base.Resource
import com.zenimmersive.android.base.ServerApi
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_ID
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_TOKEN
import okhttp3.ResponseBody
import retrofit2.Call

class ViewAllAlbumRepository(context: Context) : BaseRepository(context) {
    val apiInterface: ServerApi = getApiServer()

    suspend fun getAllAlbumsData(resModel : AlbumDetailsResModel, cache: Boolean = false): Resource<AlbumDetailsResModel> {
        var userId = KeyStorage.getInstance(context).getInt(KEY_USER_ID)
        var userToken = KeyStorage.getInstance(context).getString(KEY_USER_TOKEN)
        val call: Call<ResponseBody> = apiInterface.getAllAlbums(userId, userToken)
        return safeNetworkCall(call, resModel, cache)
    }
}