package com.zenimmersive.android.apiresponsemodel

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SubAlbum(
    @SerializedName("albumId")
    var albumId: Int?,
    @SerializedName("albumMusic")
    var albumMusic: List<AlbumMusic?>?,
    @SerializedName("albumName")
    var albumName: String?,
    @SerializedName("albumNameFrench")
    var albumNameFrench: String?,
    @SerializedName("bigBackgroundImgAlbum")
    var bigBackgroundImgAlbum: String?,
    @SerializedName("smallBackgroundImgAlbum")
    var smallBackgroundImgAlbum: String?
) : Serializable