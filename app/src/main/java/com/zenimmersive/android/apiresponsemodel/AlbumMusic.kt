package com.zenimmersive.android.apiresponsemodel

import android.content.Context
import androidx.core.text.isDigitsOnly
import com.google.gson.annotations.SerializedName
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import java.io.Serializable

data class AlbumMusic(
    @SerializedName("audioFileMusic")
    var audioFileMusic: String?,
    @SerializedName("audioFileMusicFrench")
    var audioFileMusicFrench: String?,
    @SerializedName("audioFileNarrator")
    var audioFileNarrator: String?,
    @SerializedName("audioFileNarratorFrench")
    var audioFileNarratorFrench: String?,
    @SerializedName("backgroundImageVerticle")
    var _backgroundImageVerticle: String?,
    @SerializedName("backgroundImageVerticleFrench")
    var _backgroundImageVerticleFrench: String?,
    @SerializedName("category")
    var category: List<Category?>?,
    @SerializedName("artist")
    var artist: List<Artist>?,
    @SerializedName("tag")
    var tag: List<Tag>?,
    @SerializedName("cost")
    var cost: String?,
    @SerializedName("created_at")
    var createdAt: String?,
    @SerializedName("guidedFile")
    var guidedFile: String?,
    @SerializedName("guidedFileFrench")
    var guidedFileFrench: String?,
    @SerializedName("hueColorTime")
    var hueColorTime: String?,
    @SerializedName("hueColorTimeFrench")
    var hueColorTimeFrench: String?,
    @SerializedName("isGuided")
    private var _isGuided: Int?,
    @SerializedName("isFGuided")
    private var _isFGuided: Int?,
    @SerializedName("isNew")
    var isNew: Int?,
    @SerializedName("length")
    var length: String?,
    @SerializedName("lyrics")
    var lyrics: String?,
    @SerializedName("lyricsFrench")
    var lyricsFrench: String?,
    @SerializedName("narratorMusicVideoFile")
    var narratorMusicVideoFile: String?,
    @SerializedName("narratorMusicVideoFileFrench")
    var narratorMusicVideoFileFrench: String?,
    @SerializedName("isPurchased")
    var isPurchased: Int?,
    @SerializedName("isFavorite")
    var isFavorite: Int?,
    @SerializedName("songId")
    var songId: Int?,
    @SerializedName("isPaid")
    var isPaid: Int?,
    @SerializedName("isVIP")
    var isVIP: Int?,
    @SerializedName("previewLength")
    var previewLength: String?,
    @SerializedName("productId")
    var productId: String?,
    @SerializedName("songName")
    var songName: String?,
    @SerializedName("songNameFrench")
    var songNameFrench: String?,
    @SerializedName("videoFileStream")
    var videoFileStream: String?,
    @SerializedName("videoFileStreamFrench")
    var videoFileStreamFrench: String?,
    var lastTimeMusicPosition: Long?,
    var isPlayPreview: Boolean = false,
    @SerializedName("musicPackType")
    var musicPackType: Int? = 0,
) : Serializable {
    fun getMusicDurationString(): String {
        try {
            var isDigit = length?.isDigitsOnly() ?: false
            if (isDigit) {
                var length = length?.toInt() ?: 0
                // Convert seconds to hh:mm
                val hours = length / 3600
                val minutes = (length % 3600) / 60
                val seconds = length % 60
                return String.format("%02d:%02d", minutes, seconds)
            }
            if (length.isNullOrEmpty()) return "00:00"
            return "$length"
        } catch (e: Exception) {
            return "00:00"
        }
    }

    fun getDurationInMilliSeconds(): Long {
        try {
            var isDigit = length?.isDigitsOnly() ?: false
            if (isDigit) {
                var length = length?.toInt() ?: 0
                return (length * 1000).toLong()
            } else {
                var parts = length?.split(":")
                if (parts?.size == 2) {
                    var minutes = parts[0]?.toInt() ?: 0
                    var seconds = parts[1]?.toInt() ?: 0
                    return (minutes * 60 * 1000 + seconds * 1000).toLong()
                } else if (parts?.size == 3) {
                    var hours = parts[0]?.toInt() ?: 0
                    var minutes = parts[1]?.toInt() ?: 0
                    var seconds = parts[2]?.toInt() ?: 0
                    return (hours * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000).toLong()
                }
                return 0
            }
        } catch (e: Exception) {
        }
        return 0
    }


    //    val isGuided: Int
//        get() = if (_isGuided==1) 1 else 0
//
//    val isFGuided: Int
//        get() = if (_isFGuided==1) 1 else 0
    val isGuided: Int
        get() = if (!audioFileNarrator.isNullOrEmpty()) 1 else 0

    val isFGuided: Int
        get() = if (!audioFileNarratorFrench.isNullOrEmpty()) 1 else 0

    //Only use for the Display HTML File
    fun hasGuidedFile(): Int {
        return if (_isGuided == 1 || _isFGuided == 1) 1 else 0
    }
    fun hasGuidedFileEnglish(): Boolean {
        return (_isGuided?:0) == 1
    }
    fun hasGuidedFileFrench(): Boolean {
        return (_isFGuided?:0) == 1
    }

    fun hasLyricFile(lan: String): Boolean {
        var hasLyric = false
        var _lang = lan;
        if (lan.isNullOrEmpty()) _lang = "en";
        if (!lyrics.isNullOrEmpty() && _lang == "en") hasLyric = true
        if (!lyricsFrench.isNullOrEmpty() && _lang == "fr") hasLyric = true
        return hasLyric
    }

    fun hasAudioFile(lan: String): Boolean {
        var audioFile = ""
        if (lan.equals("en") || lan.isNullOrBlank()) {
            audioFile = audioFileMusic ?: ""
        } else {
            audioFile = audioFileMusicFrench ?: ""
        }
        return audioFile.isNotEmpty()
    }

    fun getAudioFile(lan: String): String {
        var audioFile = ""
        if (lan.equals("en") || lan.isBlank()) {
            audioFile = audioFileMusic ?: ""
        } else {
            audioFile = audioFileMusicFrench ?: ""
            if (audioFile.isEmpty()) {
                audioFile = audioFileMusic ?: ""
            }
        }
        return audioFile
    }

    fun getNarratorAudioFile(lan: String): String {
        var audioFile = ""
        if (lan.equals("en") || lan.isBlank()) {
            audioFile = audioFileNarrator ?: ""
        } else {
            audioFile = audioFileNarratorFrench ?: ""
        }
        //Default Fallback
        if (audioFile.isEmpty()) {
            audioFile = audioFileNarrator ?: ""
        }
        return audioFile
    }

    //Music Only
    fun getVideoFileStream(lan: String): String {
        var videoFile = ""
        if (lan.equals("en") || lan.isBlank()) {
            videoFile = videoFileStream ?: ""
        } else {
            videoFile = videoFileStreamFrench ?: ""
        }
        //Default Fallback
        if (videoFile.isEmpty()) {
            videoFile = videoFileStream ?: ""
        }
        return videoFile
    }

    //Music + Narrator
    fun getNarratorStreamFile(lan: String): String {
        var videoFile = ""
        if (lan.equals("en") || lan.isBlank()) {
            videoFile = narratorMusicVideoFile ?: ""
        } else {
            videoFile = narratorMusicVideoFileFrench ?: ""
        }
        //Default Fallback to Only Normal Video
        if (videoFile.isEmpty()) {
            videoFile = getVideoFileStream(lan)
        }
        return videoFile
    }

    fun getStreamFile(lan: String, isNarrator: Boolean): String {
        var videoFile = ""
        if (isNarrator) videoFile = getNarratorStreamFile(lan)
        else videoFile = getVideoFileStream(lan)
        //Default Playback
        if (videoFile.isEmpty()) videoFile = getVideoFileStream(lan)

        return videoFile
    }

    fun getStreamFile(context: Context, isNarrator: Boolean): String {
        var lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE, "en")
        return getStreamFile(lan, isNarrator)
    }

    fun hasNarratorMusicFile(lan: String): Boolean {

        var narratorAudioFile = ""
        if (lan.equals("en") || lan.isNullOrBlank()) {
            narratorAudioFile = audioFileNarrator ?: ""
        } else {
            narratorAudioFile = audioFileNarratorFrench ?: ""
        }

        return narratorAudioFile.isNotEmpty()
    }

    fun hasVideoFile(lan: String): Boolean {
        var videoFile = getStreamFile(lan, false)
        if (videoFile.isEmpty()) videoFile = getStreamFile(lan, true)
        return videoFile.isNotEmpty()
    }

    fun getLyricFile(lan: String): String {
        var lyricFile = ""
        if (lan.equals("en") || lan.isNullOrBlank()) {
            lyricFile = lyrics ?: ""
        } else {
            lyricFile = lyricsFrench ?: ""
        }
        //Default Fallback
        if (lyricFile.isEmpty()) {
            lyricFile = lyrics ?: ""
        }
        return lyricFile
    }

    //getBackgroundVertical
    fun getBackgroundVertical(lan: String?): String? {
        var backgroundImage = ""
        if (lan.equals("en") || lan.isNullOrBlank()) {
            backgroundImage = _backgroundImageVerticle ?: ""
        } else {
            backgroundImage = _backgroundImageVerticleFrench ?: _backgroundImageVerticle ?: ""
        }
        return backgroundImage
    }

    fun isMatch(currentSong: AlbumMusic?): Boolean {
        if (songId == null) return false
        if (currentSong == null) return false
        if (currentSong.songId == null) return false
        return songId == currentSong.songId
    }

    fun getHueFile(context: Context): String {
        var lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE, "en")
        return getHueFile(lan)
    }

    fun getHueFile(lan: String): String {
        var hueFile = ""
        if (lan.equals("en") || lan.isNullOrBlank()) {
            hueFile = hueColorTime ?: ""
        } else {
            hueFile = hueColorTimeFrench ?: hueColorTime ?: ""
        }
        return hueFile
    }

}