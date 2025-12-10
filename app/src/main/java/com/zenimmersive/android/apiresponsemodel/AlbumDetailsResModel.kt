package com.zenimmersive.android.apiresponsemodel


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AlbumDetailsResModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("result")
    var result: List<Result?>?,
    @SerializedName("status")
    var status: Int?
) {
    data class Result(
        @SerializedName("albumId")
        var albumId: Int?,
        @SerializedName("albumName")
        var albumName: String?,
        @SerializedName("albumNameFrench")
        var albumNameFrench: String?,
        @SerializedName("bigBackgroundImgAlbum")
        var bigBackgroundImgAlbum: String?,
        @SerializedName("bigBackgroundImgAlbumFrench")
        var bigBackgroundImgAlbumFrench: String?,
        @SerializedName("description")
        var description: String?,
        @SerializedName("descriptionFrench")
        var descriptionFrench: String?,
        @SerializedName("smallBackgroundImgAlbum")
        var smallBackgroundImgAlbum: String?,
        @SerializedName("smallBackgroundImgAlbumFrench")
        var smallBackgroundImgAlbumFrench: String?,
        @SerializedName("songAlbums")
        var songAlbums: List<AlbumMusic?>?,
        @SerializedName("subAlbums")
        var subAlbums: List<SubAlbum?>?,
        @SerializedName("tagArray")
        var tagArray: List<Tag?>?,
        @SerializedName("totalSong")
        var totalSong: Int?,
        @SerializedName("tagId")
        var tagId: Int?,
        @SerializedName("tagName")
        var tagName: String?,
    ):Serializable
}