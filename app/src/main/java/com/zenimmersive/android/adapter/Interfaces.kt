package com.zenimmersive.android.adapter

import com.zenimmersive.android.apiresponsemodel.AlbumMusic

interface SongBuyListener {
    fun onMusicPackPurchaseButtonPressed(musicPack: AlbumMusic?)
}
interface MusicPackItemListener : SongBuyListener {
    fun onMusicPackPressed(songData: AlbumMusic?, popularList: ArrayList<AlbumMusic?>)
}

interface AlbumItemPressListener {
    fun albumClickListener(albumId: Int, albumImage: String, albumTitle: String, albumSession: Int, details: String)
}