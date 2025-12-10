package com.zenimmersive.android.apiresponsemodel


import com.google.gson.annotations.SerializedName

data class FilterResponseModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("result")
    var result: List<Result?>?,
    @SerializedName("status")
    var status: Int?
) {
    data class Result(
        @SerializedName("albumId")
        var albumId: Int?,
        @SerializedName("albumName")
        var albumName: String?,
        @SerializedName("albumMusic")
        var albumMusic: List<AlbumMusic?>?
    )
}