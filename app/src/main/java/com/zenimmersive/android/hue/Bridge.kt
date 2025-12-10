package com.zenimmersive.android.hue

import com.google.gson.annotations.SerializedName


data class Bridge(

    @SerializedName("id") var id: String? = null,
    @SerializedName("internalipaddress") var ipAddress: String? = null,
    @SerializedName("port") var port: Int? = null

)