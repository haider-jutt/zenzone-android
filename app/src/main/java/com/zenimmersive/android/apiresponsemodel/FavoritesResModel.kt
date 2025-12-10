package com.zenimmersive.android.apiresponsemodel


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class FavoritesResModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("result")
    var result:  List<AlbumMusic?>?,
    @SerializedName("status")
    var status: Int?
): Serializable {
    /*data class Result(
        @SerializedName("tag")
        var tag: List<Tag?>?,
        @SerializedName("albumMusic")
        var albumMusic: List<AlbumMusic?>?,
    )*/
}