package com.zenimmersive.android.apiresponsemodel

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Artist(
    @SerializedName("artistId")
    var artistId: Int?,
    @SerializedName("artistName")
    var artistName: String?,
    @SerializedName("artistNameFrench")
    var artistNameFrench: String?,
    @SerializedName("artistImage")
    var artistImage: String?,
    @SerializedName("artistImageFrench")
    var artistImageFrench: String?,
):Serializable
