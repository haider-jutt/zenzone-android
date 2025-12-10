package com.zenimmersive.android.apiresponsemodel

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Tag(
    @SerializedName("tagId")
    var tagId: Int?,
    @SerializedName("tagName")
    var tageName: String?,
    @SerializedName("tagImage")
    var tageImage: String?,
    @SerializedName("songCount")
    var songCount: String?,
    @SerializedName("tagNameFrench")
    var tageNameFrench: String?,
    @SerializedName("tagImageFrench")
    var tageImageFrench: String?,
):Serializable{
    var selected: Boolean = false
}