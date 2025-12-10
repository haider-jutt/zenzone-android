package com.zenimmersive.android.apiresponsemodel


import com.google.gson.annotations.SerializedName

data class OrderListResModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("result")
    var result: List<Result?>?,
    @SerializedName("status")
    var status: Int?
) {
    data class Result(
        @SerializedName("musicPackId")
        var musicPackId: String?,
        @SerializedName("amount", alternate = ["cost"])
        var amount: String?,
        @SerializedName("orderDate")
        var orderDate: String?,
        @SerializedName("orderNumber")
        var orderNumber: String?,
        @SerializedName("songName")
        var songName: String?,
        @SerializedName("songNameFrench")
        var songNameFrench: String?,
        @SerializedName("backgroundImageVerticle")
        var _backgroundImageVerticle: String?,
        @SerializedName("backgroundImageVerticleFrench")
        var _backgroundImageVerticleFrench: String?,
        @SerializedName("transactionCode")
        var transactionCode: String?,
        @SerializedName("purchasedPackType")
        var purchasedPackType : String?,
        @SerializedName("duration")
        var purchasedPackDuration : String?
    )
    {
        //getBackgroundVertical
        fun getBackgroundVertical(lan: String?): String? {
            var backgroundImage = ""
            if (lan.equals("en") || lan.isNullOrBlank()) {
                backgroundImage = _backgroundImageVerticle ?: ""
            } else {
                backgroundImage = _backgroundImageVerticleFrench ?: _backgroundImageVerticle ?: ""
            }
            return backgroundImage
        }

        fun getSongNameByLan(lan: String?): String {
            if (lan.equals("en") || lan.isNullOrBlank()) {
                return songName ?: ""
            } else {
                return songNameFrench ?: songName ?: ""
            }
        }
    }
}