package com.zenimmersive.android.apiresponsemodel

import com.google.gson.annotations.SerializedName

data class VipSongsResModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("result")
    var result: List<AlbumMusic?>?,
    @SerializedName("status")
    var status: Int?
){

}