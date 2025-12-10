package com.zenimmersive.android.apiresponsemodel


import com.google.gson.annotations.SerializedName

data class StaticPagesResModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("result")
    var result: Result?,
    @SerializedName("status")
    var status: Int?
) {
    data class Result(
        @SerializedName("StaticId")
        var staticId: String?,
        @SerializedName("description")
        var description: String?,
        @SerializedName("title")
        var title: String?,
        @SerializedName("f_description")
        var frenchDescription: String?,
        @SerializedName("f_title")
        var frenchTitle: String?
    )
}