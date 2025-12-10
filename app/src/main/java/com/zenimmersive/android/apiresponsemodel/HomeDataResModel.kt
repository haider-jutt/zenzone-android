package com.zenimmersive.android.apiresponsemodel


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class HomeDataResModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("orderData")
    var orderData: List<String?>?,
    @SerializedName("result")
    var result: Result?,
    @SerializedName("status")
    var status: Int?
):Serializable {
    data class Result(
        @SerializedName("albums")
        var albums: List<Album?>?,
        @SerializedName("meditationType")
        var meditationType: List<Tag?>?,
        @SerializedName("popular")
        var popular: List<AlbumMusic?>?,
        @SerializedName("recentlyPlay")
        var recentlyPlay: List<AlbumMusic?>?,
        @SerializedName("tags")
        var tags: List<Album?>?
    ):Serializable {
        data class Album(
            @SerializedName("albumId")
            var albumId: Int?,
            @SerializedName("albumName")
            var albumName: String?,
            @SerializedName("albumNameFrench")
            var albumNameFrench: String?,
            @SerializedName("bigBackgroundImgAlbum")
            var bigBackgroundImgSubAlbum: String?,
            @SerializedName("bigBackgroundImgAlbumFrench")
            var bigBackgroundImgSubAlbumFrench: String?,
            @SerializedName("description")
            var description: String?,
            @SerializedName("descriptionFrench")
            var descriptionFrench: String?,
            @SerializedName("smallBackgroundImgAlbum")
            var smallBackgroundImgSubAlbum: String?,
            @SerializedName("smallBackgroundImgAlbumFrench")
            var smallBackgroundImgSubAlbumFrench: String?,
            @SerializedName("songAlbums")
            var songAlbums: List<AlbumMusic?>?,
            @SerializedName("subAlbums")
            var subAlbums: List<Album?>?,
            @SerializedName("tagArray")
            var tagArray: List<Tag?>?,
            @SerializedName("totalSong")
            var totalSong: Int?
        ):Serializable
    }
}