package com.zenimmersive.android.apiresponsemodel


import com.google.gson.annotations.SerializedName

data class TagListResModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("result")
    var result: List<Tag?>?,
    @SerializedName("status")
    var status: Int?
) {
}