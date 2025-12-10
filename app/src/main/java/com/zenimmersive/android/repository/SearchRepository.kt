package com.zenimmersive.android.repository

import android.content.Context
import com.zenimmersive.android.apiresponsemodel.AlbumDetailsResModel
import com.zenimmersive.android.apiresponsemodel.FilterResponseModel
import com.zenimmersive.android.apiresponsemodel.SearchSongsResModel
import com.zenimmersive.android.apiresponsemodel.TagListResModel
import com.zenimmersive.android.base.BaseRepository
import com.zenimmersive.android.base.RemoteDataSource.Companion.getApiServer
import com.zenimmersive.android.base.Resource
import com.zenimmersive.android.base.ServerApi
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_ID
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_TOKEN
import okhttp3.ResponseBody
import retrofit2.Call

class SearchRepository(context: Context) : BaseRepository(context) {
    val apiInterface: ServerApi = getApiServer()

    suspend fun getTags(resModel : TagListResModel): Resource<TagListResModel> {
        var userId = KeyStorage.getInstance(context).getInt(KEY_USER_ID)
        var userToken = KeyStorage.getInstance(context).getString(KEY_USER_TOKEN)
        val call: Call<ResponseBody> = apiInterface.getTagListApi(userId, userToken)
        return safeNetworkCall(call, resModel)
    }

    suspend fun getTagsSongs(tagId: Int, resModel : AlbumDetailsResModel): Resource<AlbumDetailsResModel> {
        var userId = KeyStorage.getInstance(context).getInt(KEY_USER_ID)
        var userToken = KeyStorage.getInstance(context).getString(KEY_USER_TOKEN)
        val call: Call<ResponseBody> = apiInterface.getTagSongApi(userId, userToken, tagId)
        return safeNetworkCall(call, resModel)
    }

    suspend fun searchSongs(searchKeyword: String, resModel : SearchSongsResModel): Resource<SearchSongsResModel> {
        var userId = KeyStorage.getInstance(context).getInt(KEY_USER_ID)
        var userToken = KeyStorage.getInstance(context).getString(KEY_USER_TOKEN)
        val call: Call<ResponseBody> = apiInterface.searchSongs(userId, userToken, searchKeyword)
        return safeNetworkCall(call, resModel)
    }

    suspend fun filterSongs(tagIds: String, fromDuration: String, toDuration: String, resModel : FilterResponseModel): Resource<FilterResponseModel> {
        var userId = KeyStorage.getInstance(context).getInt(KEY_USER_ID)
        var userToken = KeyStorage.getInstance(context).getString(KEY_USER_TOKEN)
        val call: Call<ResponseBody> = apiInterface.filterSongs(userId, userToken, tagIds, fromDuration, toDuration)
        return safeNetworkCall(call, resModel)
    }
}