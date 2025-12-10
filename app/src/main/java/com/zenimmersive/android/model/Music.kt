package com.zenimmersive.android.model

import android.os.Parcel
import android.os.Parcelable

class Music() : Parcelable {
    var id: Int = 0
    var musicTitle: String = "SIRENA"
    var musicArtist: String = "Relex &#8226; Aqua, ELAVASIN"

    var voiceFileUrl: String = ""
    var audioFileUrl: String = ""
    var lyricFileUrl: String = ""
    var bannerImageURL: String = "https://placehold.co/150x150"
    var backgroundImage: String = ""
    var colorAutomationFileUrl: String =
        "https://keshavinfotechdemo2.com/keshav/KG1/sym/ColorFiles/colorautofile.txt"

    var castMusicFileUrl: String = ""
    var castVoiceFileUrl: String = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readInt()
        musicTitle = parcel.readString() ?: ""
        musicArtist = parcel.readString() ?: ""
        voiceFileUrl = parcel.readString() ?: ""
        audioFileUrl = parcel.readString() ?: ""
        lyricFileUrl = parcel.readString() ?: ""
        bannerImageURL = parcel.readString() ?: ""
        backgroundImage = parcel.readString() ?: ""
        colorAutomationFileUrl = parcel.readString() ?: ""
        castMusicFileUrl = parcel.readString() ?: ""
        castVoiceFileUrl = parcel.readString() ?: ""
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(musicTitle)
        parcel.writeString(musicArtist)
        parcel.writeString(voiceFileUrl)
        parcel.writeString(audioFileUrl)
        parcel.writeString(lyricFileUrl)
        parcel.writeString(bannerImageURL)
        parcel.writeString(backgroundImage)
        parcel.writeString(colorAutomationFileUrl)
        parcel.writeString(castMusicFileUrl)
        parcel.writeString(castVoiceFileUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Music> {
        override fun createFromParcel(parcel: Parcel): Music {
            return Music(parcel)
        }

        override fun newArray(size: Int): Array<Music?> {
            return arrayOfNulls(size)
        }
    }
}
