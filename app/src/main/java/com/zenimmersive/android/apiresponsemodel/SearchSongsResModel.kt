package com.zenimmersive.android.apiresponsemodel


import com.google.gson.annotations.SerializedName

data class SearchSongsResModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("result")
    var result: Result?,
    @SerializedName("status")
    var status: Int?
) {
    data class Result(
        @SerializedName("tag")
        var tag: List<Tag?>?,
        @SerializedName("albumMusic")
        var albumMusic: List<AlbumMusic?>?,
    )
}