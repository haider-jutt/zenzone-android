package com.zenimmersive.android.apiresponsemodel


import com.google.gson.annotations.SerializedName

data class UserBasicResModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("result")
    var result: Result?,
    @SerializedName("status")
    var status: Int?
) {
    data class Result(
        @SerializedName("email")
        var email: String?,
        @SerializedName("userId")
        var userId: Int?,
        @SerializedName("userName")
        var userName: String?,
        @SerializedName("userToken")
        var userToken: String?,
        @SerializedName("otp")
        var otp: String?,
        @SerializedName("isVerify")
        var isVerify: Int?,
        @SerializedName("userSubscriptionType")
        var userSubscriptionType: Int? = 1, // 1=free, 2=premium, 3=infinite
        @SerializedName("expireTime", alternate = ["purchaseDurationTime"])
        var expireTime: String? = null,
        @SerializedName("purchasedPackId")
        var purchasedPackId: String? = null,
        @SerializedName("purchasedPackType")
        var purchasedPackType: String? = null,
        @SerializedName("purchaseTime")
        var purchaseTime: String? = null,
        @SerializedName("purchaseToken")
        var purchaseToken: String? = null,
        @SerializedName("orderId")
        var orderId: String? = null
    )
}