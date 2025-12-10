package com.zenimmersive.android.base

import com.google.gson.annotations.SerializedName

public open class BaseResponse {
    @SerializedName("status")
    var status = 0

    @SerializedName("message")
    var message: String? = null

    constructor(status: Int, message: String?) {
        this.status = status
        this.message = message
    }

    constructor()

    fun isSuccessful(): Boolean {
        return status != null && status == 1
    }

}