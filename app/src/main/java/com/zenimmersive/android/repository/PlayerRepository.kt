package com.zenimmersive.android.repository

import android.content.Context
import com.zenimmersive.android.apiresponsemodel.AddRemoveFavoriteSongApi
import com.zenimmersive.android.apiresponsemodel.SongDetailsResModel
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

class PlayerRepository(context: Context) : BaseRepository(context) {
    val apiInterface: ServerApi = getApiServer()

    suspend fun getSongData(userId: Int, userToken: String, songId: Int, resModel : BaseResponse): Resource<BaseResponse> {
        val call: Call<ResponseBody> = apiInterface.getSongDetails(userId, userToken, songId)
        return safeNetworkCall(call, resModel)
    }

    suspend fun getSongData(songId: Int, resModel : SongDetailsResModel): Resource<SongDetailsResModel> {
        var userId = KeyStorage.getInstance(context).getInt(KEY_USER_ID)
        var userToken = KeyStorage.getInstance(context).getString(KEY_USER_TOKEN)
        val call: Call<ResponseBody> = apiInterface.getSongDetails(userId, userToken, songId)
        return safeNetworkCall(call, resModel)
    }

    suspend fun addRemoveFavorite(songId: Int, isFavorite: Int, resModel : AddRemoveFavoriteSongApi): Resource<AddRemoveFavoriteSongApi> {
        var userId = KeyStorage.getInstance(context).getInt(KEY_USER_ID)
        var userToken = KeyStorage.getInstance(context).getString(KEY_USER_TOKEN)
        val call: Call<ResponseBody> = apiInterface.addRemoveFavoriteApi(userId, userToken, songId, isFavorite)
        return safeNetworkCall(call, resModel)
    }
}