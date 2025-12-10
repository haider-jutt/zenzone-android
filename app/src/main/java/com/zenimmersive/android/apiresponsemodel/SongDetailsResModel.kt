package com.zenimmersive.android.apiresponsemodel


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SongDetailsResModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("result")
    var result: AlbumMusic?,
    @SerializedName("status")
    var status: Int?
):Serializable