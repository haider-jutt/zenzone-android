package com.zenimmersive.android.apiresponsemodel


import com.google.gson.annotations.SerializedName

data class FaqResModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("result")
    var result: List<Result?>?,
    @SerializedName("status")
    var status: Int?
) {
    data class Result(
        @SerializedName("description")
        var description: String?,
        @SerializedName("FaqId")
        var faqId: Int?,
        @SerializedName("question")
        var question: String?
    )
}