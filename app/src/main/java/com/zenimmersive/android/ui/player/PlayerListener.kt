package com.zenimmersive.android.ui.player

import android.view.View
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.google.android.gms.cast.MediaError

interface PlayerListener {
    fun onPlaybackStateChanged(state: Int)
    fun onVideoPlaybackStateChanged(state: Int) {}
    fun onPlayerStateChanged(musicReady: Boolean, voiceReady: Boolean)
    fun onSessionEnded()
    fun hideLoader()
    fun onCastSessionDisconnected()
    fun onCastSessionConnected()
    fun onPlayerError(message: String? = "There is something wrong with network or datasource")
    fun getHueColorView(): View?
    fun songNotPurchased(currentMusicItem: AlbumMusic?, allDirectMusicList: ArrayList<AlbumMusic>)
    fun onVideoPlayerError(message: String?) {}
    fun onRemoteMediaError(mediaError: MediaError)
}
