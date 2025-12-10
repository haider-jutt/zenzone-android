package com.zenimmersive.android.helper

import com.zenimmersive.android.model.Music
import kotlin.random.Random

object SymMusicList {
    fun buildMusicList(): List<Music> {
        var musicList = arrayListOf<Music>()
        musicList.add(Music().also {
            it.musicTitle = "Eddy Pack"
            it.musicArtist = "Eddy Initial &#8226; Demo, System"

            it.castVoiceFileUrl =
                "https://keshavinfotechdemo.com/KESHAV/UPDATE/14/iphone/SYMVideos/healing_video_narratormusicEN.mov"
            it.castMusicFileUrl =
                "https://keshavinfotechdemo.com/KESHAV/UPDATE/14/iphone/SYMVideos/healing_video_musiconly.mov"

            it.voiceFileUrl =
                "https://keshavinfotechdemo.com/KESHAV/UPDATE/19/android/Ashvin/healing_audio_narratorEN.mp4"
            it.audioFileUrl =
                    "https://keshavinfotechdemo.com/KESHAV/UPDATE/19/android/Ashvin/healing_audio_musiconly.mp4"
            it.lyricFileUrl =
                "https://keshavinfotechdemo2.com/keshav/vtt/healing_video_narratormusicEN.vtt"
            it.bannerImageURL =
                "https://images.pexels.com/photos/1212487/pexels-photo-1212487.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1"
            it.backgroundImage =
                "https://images.pexels.com/photos/1212487/pexels-photo-1212487.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1"
        })
        musicList.add(Music().also {
            it.musicTitle = "Music Video"
            it.musicArtist = "Keshav Infotech &#8226; Aqua, Ashvin"
            it.audioFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/01_Deep Earth-MP3.mp3"
            it.backgroundImage =
                "https://wallpapers.com/images/high/black-and-red-berries-samsung-full-hd-6qvady67l095dglg.webp"
        })
        musicList.add(Music().also {
            it.musicTitle = "Music ${Random.nextInt(100)}"
            it.musicArtist = "Keshav Infotech &#8226; Aqua, Ashvin"
            it.audioFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/02_Isis Energy-MP3.mp3"
            it.backgroundImage =
                "https://wallpapers.com/images/high/blue-and-black-berries-on-samsung-full-hd-f074e06u9yoftqdu.webp"
        })
        musicList.add(Music().also {
            it.musicTitle = "Music ${Random.nextInt(100)}"
            it.musicArtist = "Keshav Infotech &#8226; Aqua, Ashvin"
            it.audioFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/03_Mother Light-MP3.mp3"
            it.backgroundImage =
                "https://wallpapers.com/images/high/gradient-blue-purple-feather-samsung-full-hd-wm2c4ea477ico180.webp"
        })
        musicList.add(Music().also {
            it.musicTitle = "Music ${Random.nextInt(100)}"
            it.musicArtist = "Keshav Infotech &#8226; Aqua, Ashvin"
            it.audioFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/04_Celtic Heart-MP3.mp3"
            it.backgroundImage =
                "https://wallpapers.com/images/high/soft-and-fuzzy-balls-on-samsung-full-hd-807ed4i602bopbsz.webp"
        })
        musicList.add(Music().also {
            it.musicTitle = "Music ${Random.nextInt(100)}"
            it.musicArtist = "Keshav Infotech &#8226; Aqua, Ashvin"
            it.audioFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/05_Open Your Voice-MP3.mp3"
        })
        musicList.add(Music().also {
            it.musicTitle = "Music ${Random.nextInt(100)}"
            it.musicArtist = "Keshav Infotech &#8226; Aqua, Ashvin"
            it.audioFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/07_Atmosphere-MP3.mp3"
        })
        musicList.add(Music().also {
            it.musicTitle = "Music ${Random.nextInt(100)}"
            it.musicArtist = "Keshav Infotech &#8226; Aqua, Ashvin"
            it.audioFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/ARIA-RELAXATION-AVA.mp3"
        })
        musicList.add(Music().also {
            it.musicTitle = "Music ${Random.nextInt(100)}"
            it.musicArtist = "Keshav Infotech &#8226; Aqua, Ashvin"
            it.audioFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/ARIA-RELAXATION-MARI-EVE.mp3"
        })
        musicList.add(Music().also {
            it.musicTitle = "Music ${Random.nextInt(100)}"
            it.musicArtist = "Keshav Infotech &#8226; Aqua, Ashvin"
            it.audioFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/ARIA-RELAXATION-SIRENA.mp3"
        })
        musicList.add(Music().also {
            it.musicTitle = "Music ${Random.nextInt(100)}"
            it.musicArtist = "Keshav Infotech &#8226; Aqua, Ashvin"
            it.audioFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/Soin_audio_confiance_en_soi_en_musiconly.m4a"
            it.voiceFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/Soin_audio_confiance_en_soi_en_voiceonly.m4a"
        })
        musicList.add(Music().also {
            it.musicTitle = "Music ${Random.nextInt(100)}"
            it.musicArtist = "Keshav Infotech &#8226; Aqua, Ashvin"
            it.audioFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/Soin_audio_confiance_en_soi_fr_musiconly.m4a"
            it.voiceFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/Soin_audio_confiance_en_soi_fr_voiceonly.m4a"
        })
        musicList.add(Music().also {
            it.musicTitle = "Music ${Random.nextInt(100)}"
            it.musicArtist = "Keshav Infotech &#8226; Aqua, Ashvin"
            it.audioFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/soin_audio_estime_de_soi_en_musiconly.m4a"
            it.voiceFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/soin_audio_estime_de_soi_en_voiceonly.m4a"
        })
        musicList.add(Music().also {
            it.musicTitle = "Music ${Random.nextInt(100)}"
            it.musicArtist = "Keshav Infotech &#8226; Aqua, Ashvin"
            it.audioFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/soin_audio_estime_de_soi_fr_musiconly.m4a"
            it.voiceFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/soin_audio_estime_de_soi_fr_voiceonly.m4a"
        })
        musicList.add(Music().also {
            it.musicTitle = "Music ${Random.nextInt(100)}"
            it.musicArtist = "Keshav Infotech &#8226; Aqua, Ashvin"
            it.audioFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/soin_audio_regeneration_en_musiconly.m4a"
            it.voiceFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/soin_audio_regeneration_en_voiceonly.m4a"
        })
        musicList.add(Music().also {
            it.musicTitle = "Music ${Random.nextInt(100)}"
            it.musicArtist = "Keshav Infotech &#8226; Aqua, Ashvin"
            it.audioFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/soin_audio_regeneration_fr_musiconly.m4a"
            it.voiceFileUrl =
                "https://keshavinfotechdemo2.com/keshav/KG1/sym/MusicFiles/soin_audio_regeneration_fr_voiceonly.m4a"
        })
        musicList.forEachIndexed { index, music ->
            music.id = index
        }
        return musicList
    }

}
