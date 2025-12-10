package com.zenimmersive.android.apiresponsemodel


import com.google.gson.annotations.SerializedName

data class UserDetailsResModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("result")
    var result: Result?,
    @SerializedName("status")
    var status: Int?
) {
    data class Result(
        @SerializedName("age")
        var age: Int?,
        @SerializedName("country")
        var country: String?,
        @SerializedName("email")
        var email: String?,
        @SerializedName("birthdate")
        var birthdate: String?,
        @SerializedName("gender")
        var gender: Int?,
        @SerializedName("userId")
        var userId: Int?,
        @SerializedName("userName")
        var userName: String?,
        @SerializedName("userToken")
        var userToken: String?,
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