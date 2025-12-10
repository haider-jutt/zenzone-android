package com.zenimmersive.android.apiresponsemodel


import com.google.gson.annotations.SerializedName

data class AddRemoveFavoriteSongApi(
    @SerializedName("isFavorite")
    var isFavorite: Int?,
    @SerializedName("message")
    var message: String?,
    @SerializedName("status")
    var status: Int?
)