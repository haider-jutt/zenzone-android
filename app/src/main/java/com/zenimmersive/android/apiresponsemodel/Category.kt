package com.zenimmersive.android.apiresponsemodel


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Category(
    @SerializedName("categoryId")
    var categoryId: Int?,
    @SerializedName("categoryImage")
    var categoryImage: String?,
    @SerializedName("categoryImageFrench")
    var categoryImageFrench: String?,
    @SerializedName("categoryName")
    var categoryName: String?,
    @SerializedName("categoryNameFrench")
    var categoryNameFrench: String?
):Serializable