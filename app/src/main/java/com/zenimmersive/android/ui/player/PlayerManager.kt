package com.zenimmersive.android.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.helper.CustomLyricView.LyricLine

import com.zenimmersive.android.helper.MusicPackDownloader
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.hue.HueColorManager
import com.zenimmersive.android.hue.HueColorManager.clearColor
import com.zenimmersive.android.hue.HueLightManager
import com.zenimmersive.android.ui.player.AppCastManager.castContext
import com.google.android.gms.cast.MediaError
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.TextTrackStyle
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import java.io.File
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.math.roundToInt

@SuppressLint("UnsafeOptInUsageError")
class PlayerManager private constructor(
    private val context: Context, private var playerListener: PlayerListener? = null
) {
    private val TAG = "PlayerManagement"
    var exoPlayerVoice: ExoPlayer? = null
    var exoPlayerMusic: ExoPlayer? = null
    var exoLocalVideoPlayer: ExoPlayer? = null
    var remoteMediaClient: RemoteMediaClient? = null
    var isPlaying = false

    var isNarrator = false
    private var isMusicReady = false
    private var isVoiceReady = false

    init {
        playerManagerInstance = this
        AppCastManager.playerListener = playerListener
        AppCastManager.sessionListener = object : AppCastManager.AppCastSessionListener {
            override fun startCastingContent(caller: String) {
                this@PlayerManager.startCastingContent(caller)
            }
        }
        AppCastManager.setupCastSession(context)
        remoteMediaClient =
            AppCastManager.castContext?.sessionManager?.currentCastSession?.remoteMediaClient
    }


    private fun startCastingContent(caller: String?) {
        LogSystem.e(TAG, "Remote Media Parent-startCastingContent Invoked by $caller")
        val session = castContext?.sessionManager?.currentCastSession
        if (session != null && session.isConnected) {
            startCastingContent(session, "Parent-startCastingContent")
        }
    }

    var playerMusicList = arrayListOf<AlbumMusic>()

    @Synchronized
    fun initializePlayers(
        _music: AlbumMusic?,
        selectMusicIndex: Int,
        directMusicList: ArrayList<AlbumMusic>,
        tempCallback: PlayerListener? = null,
        _isNarrator: Boolean = false
    ) {


        directMusicList.forEach {
            if (it.isPlayPreview) {
                it.lastTimeMusicPosition = 0L
            }
        }

        var music = directMusicList[selectMusicIndex]
        _currentMediaItemIndex = selectMusicIndex
        playerMusicList.clear()
        playerMusicList.addAll(directMusicList)
        // Setup download queue for upcoming songs
        MusicPackDownloader.setupDownloadQueue(
            context,
            music,
            java.util.ArrayList(directMusicList),
            selectMusicIndex
        )

        LogSystem.e(
            TAG,
            "initializePlayers Invoked\nMusic File : ${music?.audioFileMusic}\nVoice File : ${music?.audioFileNarrator}"
        )
        isNarrator = _isNarrator
//        releasePlayers()


        initializePlayersIfNeed(selectMusicIndex, tempCallback)
    }

    private var isVideoReady = false
    private fun setupLocalVideoPlayer(
        loadControl: DefaultLoadControl,
        selectMusicIndex: Int,
        tempCallback: PlayerListener?
    ) {
        isVideoReady = false

        var lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE, "en")
        val musicPack = playerMusicList.getOrNull(selectMusicIndex)
        val localVideoFileURL = MusicPackDownloader.findLocalVideoFileURL(
            context,
            musicPack,
            isNarrator
        )
        if (localVideoFileURL.isNullOrEmpty()) {
            exoLocalVideoPlayer?.release()
            exoLocalVideoPlayer = null
            return
        }

        exoLocalVideoPlayer = (exoLocalVideoPlayer ?: ExoPlayer.Builder(context)
            .setRenderersFactory(DefaultRenderersFactory(context).setEnableDecoderFallback(true))
            .setLoadControl(loadControl).build()).apply {
            setMediaSource(buildSingleLocalVideoPlayerSource(selectMusicIndex))
            prepare()
            setVolume(0f)
            setPlayWhenReady(false)
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    LogSystem.e(TAG, "PlayerLocalVideo onPlaybackStateChanged Invoked")

                    when (state) {
                        Player.STATE_READY, Player.STATE_IDLE -> {
                            isVideoReady = true
                        }

                        Player.STATE_BUFFERING -> {
                            isVideoReady = false
                        }
                    }
                    playerListener?.onVideoPlaybackStateChanged(state)
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    error.printStackTrace()
                    var index = fetchCurrentMediaIndex() ?: 0
                    var musicPack = playerMusicList[index]
                    var streamURL = musicPack.getStreamFile(
                        KeyStorage.getInstance(context)
                            .getString(APP_SELECTED_LANGUAGE, "en"),
                        isNarrator
                    )
                    var localURL =
                        MusicPackDownloader.findLocalVideoFileURL(context, musicPack, isNarrator)

                    // Enhanced error logging with detailed information
                    val errorDetails = buildDetailedErrorMessage(
                        error,
                        "LocalVideoPlayer",
                        index,
                        streamURL,
                        localURL
                    )
                    LogSystem.e(TAG, errorDetails)

                    exoLocalVideoPlayer?.release()
                    exoLocalVideoPlayer = null

                    // Only delete local file if we're actually playing from local URL
                    if (isPlayingFromLocalFile(localURL, streamURL)) {
                        try {
                            val localFile = File(localURL)
                            if (localFile.exists()) {
                                localFile.delete()
                                LogSystem.e(TAG, "Deleted corrupted local video file: $localURL")
                            }
                        } catch (e: Exception) {
                            LogSystem.e(TAG, "Failed to delete local video file: ${e.message}")
                        }
                        setupLocalVideoPlayer(
                            defaultLoadControl(),
                            index,
                            tempCallback
                        )
                    } else {
                        LogSystem.e(
                            TAG,
                            "Playing from server URL, not deleting local file. StreamURL: $streamURL, LocalURL: $localURL"
                        )
                        playerListener?.onVideoPlayerError(error.message)
                    }
                }
            })
        }
    }

    private fun buildSingleLocalVideoPlayerSource(index: Int): ProgressiveMediaSource {
        val musicPack = playerMusicList.getOrNull(index) ?: playerMusicList.first()
        val localVideoFileURL = MusicPackDownloader.findLocalVideoFileURL(
            context,
            musicPack,
            isNarrator
        )
        LogSystem.e(
            TAG,
            "Local Video Player\nURL : " + musicPack.getStreamFile(
                context,
                isNarrator
            ) + "\nLocal File : " + localVideoFileURL
        )
        return getMediaSource(localVideoFileURL, musicPack)
    }

    private fun defaultLoadControl() =
        DefaultLoadControl.Builder().setBackBuffer(5_000, false).build()

    private fun setupNarratorAudioPlayer(
        loadControl: DefaultLoadControl,
        selectMusicIndex: Int,
        tempCallback: PlayerListener?,
        _isNarrator: Boolean = false
    ) {
        var lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE, "en")
        val audioFile = playerMusicList.getOrNull(selectMusicIndex)
        val audioNarratorURL = audioFile?.getNarratorAudioFile(lan) ?: ""
        if (audioNarratorURL.isEmpty()) {
            exoPlayerVoice?.release()
            exoPlayerVoice = null
            isVoiceReady = true
            return
        }


        exoPlayerVoice = (exoPlayerVoice ?: ExoPlayer.Builder(context)
            .setRenderersFactory(DefaultRenderersFactory(context).setEnableDecoderFallback(true))
            .setLoadControl(loadControl).build()).apply {
            setMediaSource(buildSingleNarratorAudioPlayerSource(selectMusicIndex))
            seekTo(0, playerMusicList[selectMusicIndex].lastTimeMusicPosition ?: 0L)
            prepare()
            setPlayWhenReady(false)
            if (_isNarrator) volume = 1f
            else volume = 0f // Mute initially
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    LogSystem.e(
                        TAG, "PlayerVoice onPlaybackStateChanged Invoked State : ${state}"
                    )
                    playerListener?.onPlaybackStateChanged(state)
                    when (state) {
                        Player.STATE_IDLE -> {
                            isVoiceReady = true
                        }

                        Player.STATE_READY -> {
                            isVoiceReady = true
                            playerListener?.onPlayerStateChanged(isMusicReady, isVoiceReady)
                            tempCallback?.onPlayerStateChanged(isMusicReady, isVoiceReady)
                            if (isRemoteClientConnected()) {
                                remoteMediaClient =
                                    castContext?.sessionManager?.currentCastSession?.remoteMediaClient
                                loadMedia("VoiceReady#301")
                            }
                        }

                        Player.STATE_BUFFERING -> {
                            isVoiceReady = false
                            if (isMusicReady) exoPlayerMusic?.pause()
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    var index = fetchCurrentMediaIndex() ?: 0
                    isVoiceReady = true
                    exoPlayerVoice?.release()
                    exoPlayerVoice = null

                    var audioFile = playerMusicList[index]
                    var audioNarratorURL = audioFile?.getNarratorAudioFile(
                        KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE, "en")
                    ) ?: ""
                    var localFile =
                        MusicPackDownloader.findFileByURL(context, audioFile, audioNarratorURL)

                    // Enhanced error logging with detailed information
                    val errorDetails = buildDetailedErrorMessage(
                        error,
                        "NarratorAudioPlayer",
                        index,
                        audioNarratorURL,
                        localFile
                    )
                    LogSystem.e(TAG, errorDetails)

                    playerListener?.onPlayerStateChanged(isMusicReady, isVoiceReady)

                    // Only delete local file if we're actually playing from local URL
                    if (isPlayingFromLocalFile(localFile, audioNarratorURL)) {
                        try {
                            val localFileObj = File(localFile)
                            if (localFileObj.exists()) {
                                localFileObj.delete()
                                LogSystem.e(
                                    TAG,
                                    "Deleted corrupted local narrator audio file: $localFile"
                                )
                            }
                        } catch (e: Exception) {
                            LogSystem.e(
                                TAG,
                                "Failed to delete local narrator audio file: ${e.message}"
                            )
                        }
                        setupNarratorAudioPlayer(
                            defaultLoadControl(),
                            index,
                            tempCallback,
                            _isNarrator
                        )
                    } else {
                        LogSystem.e(
                            TAG,
                            "Playing from server URL, not deleting local file. AudioURL: $audioNarratorURL, LocalFile: $localFile"
                        )
                    }
                }
            })
        }
    }

    private fun buildSingleMusicAudioPlayerSource(index: Int): ProgressiveMediaSource {
        val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE, "en")
        val audioFile = playerMusicList.getOrNull(index) ?: playerMusicList.first()
        val audioFileUrl = audioFile?.getAudioFile(lan) ?: ""
        var localFile = MusicPackDownloader.findFileByURL(
            context,
            audioFile,
            audioFileUrl
        )
        LogSystem.e(
            TAG,
            "Normal Audio Player\nURL : " + audioFileUrl + "\nLocal File : " + localFile
        )
        return getMediaSource(localFile, null)
    }

    private fun buildSingleNarratorAudioPlayerSource(index: Int): ProgressiveMediaSource {
        val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE, "en")
        val audioFile = playerMusicList.getOrNull(index) ?: playerMusicList.first()
        var localFile = MusicPackDownloader.findFileByURL(
            context,
            audioFile,
            audioFile?.getNarratorAudioFile(lan) ?: ""
        )
        LogSystem.e(
            TAG,
            "Narrator Audio Player\nURL : " + audioFile + "\nLocal File : " + localFile
        )
        return getMediaSource(
            localFile, audioFile
        )
    }

    private fun setupMusicAudioPlayer(
        loadControl: DefaultLoadControl,
        selectMusicIndex: Int,
        tempCallback: PlayerListener?
    ) {

        val audioFile = playerMusicList.getOrNull(selectMusicIndex) ?: playerMusicList.first()
        HueColorManager.loadColorData(context, audioFile)

        exoPlayerMusic = (exoPlayerMusic ?: ExoPlayer.Builder(context)
            .setRenderersFactory(DefaultRenderersFactory(context).setEnableDecoderFallback(true))
            .setLoadControl(loadControl).build()).apply {
            setMediaSource(buildSingleMusicAudioPlayerSource(selectMusicIndex))
            seekTo(0, playerMusicList[selectMusicIndex].lastTimeMusicPosition ?: 0L)
            prepare()
            setPlayWhenReady(false)
            addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    var position = fetchCurrentMediaIndex()
                    if (position != null) _musicItem = playerMusicList[position]
                }

                override fun onPlaybackStateChanged(state: Int) {
                    LogSystem.e(TAG, "PlayerMusic onPlaybackStateChanged Invoked")
                    playerListener?.onPlaybackStateChanged(state)
                    when (state) {
                        Player.STATE_READY -> {
                            isMusicReady = true
                            playerListener?.onPlayerStateChanged(isMusicReady, isVoiceReady)
                            tempCallback?.onPlayerStateChanged(isMusicReady, isVoiceReady)
                            if (isRemoteClientConnected()) {
                                remoteMediaClient =
                                    castContext?.sessionManager?.currentCastSession?.remoteMediaClient
                                loadMedia("MusicReady#398")
                            }
                        }

                        Player.STATE_BUFFERING -> {
                            isMusicReady = false
                            if (isVoiceReady) exoPlayerVoice?.pause()
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    var index = fetchCurrentMediaIndex() ?: 0
                    var musicPack = playerMusicList[index]
                    var musicUrl = musicPack.getAudioFile(
                        KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE, "en")
                    )
                    var localFile = MusicPackDownloader.findFileByURL(context, musicPack, musicUrl)

                    // Enhanced error logging with detailed information
                    val errorDetails = buildDetailedErrorMessage(
                        error,
                        "MusicAudioPlayer",
                        index,
                        musicUrl,
                        localFile
                    )
                    LogSystem.e(TAG, errorDetails)

                    isMusicReady = false

                    exoPlayerMusic?.release()
                    exoPlayerMusic = null
                    exoPlayerVoice?.release()
                    exoPlayerVoice = null
                    exoLocalVideoPlayer?.release()
                    exoLocalVideoPlayer = null

                    // Only delete local file if we're actually playing from local URL
                    if (isPlayingFromLocalFile(localFile, musicUrl)) {
                        try {
                            val localFileObj = File(localFile)
                            if (localFileObj.exists()) {
                                localFileObj.delete()
                                LogSystem.e(
                                    TAG,
                                    "Deleted corrupted local music audio file: $localFile"
                                )
                            }
                        } catch (e: Exception) {
                            LogSystem.e(
                                TAG,
                                "Failed to delete local music audio file: ${e.message}"
                            )
                        }

                        setupMusicAudioPlayer(defaultLoadControl(), index, tempCallback)
                        setupNarratorAudioPlayer(
                            defaultLoadControl(),
                            index,
                            tempCallback,
                            isNarrator
                        )
                        setupLocalVideoPlayer(defaultLoadControl(), index, tempCallback)
                        return
                    }

                    LogSystem.e(
                        TAG,
                        "Playing from server URL, not deleting local file. MusicURL: $musicUrl, LocalFile: $localFile"
                    )
                    playerListener?.onPlayerError(error.message)
                }
            })
        }
    }


    fun checkedIsPurchasedMusic(musicData: AlbumMusic?): Int {
        return musicData?.isPurchased ?: 0
    }

    fun previousMusicPlay() {
        LogSystem.e(
            TAG,
            "previousMusicPlay Invoked isNarrator : ${isNarrator} Current Index : ${_currentMediaItemIndex}"
        )
        var index = _currentMediaItemIndex - 1
        if (index < 0) {
            index = playerMusicList.size - 1
        }
        _currentMediaItemIndex = index
        releasePlayers()
        setupSources()
        exoPlayerMusic?.seekTo(0, 0)
        exoPlayerVoice?.seekTo(0, 0)
        exoLocalVideoPlayer?.seekTo(0, 0)
        initializePlayersIfNeed(index, null)
        loadMedia("PreviousMusic")
    }

    fun nextMusicPlay() {
        LogSystem.e(
            TAG,
            "nextMusicPlay Invoked isNarrator : ${isNarrator} Current Index : ${_currentMediaItemIndex}"
        )
        var index = _currentMediaItemIndex + 1
        if (index >= playerMusicList.size) {
            index = 0
        }
        _currentMediaItemIndex = index
        releasePlayers()
        setupSources()
        exoPlayerMusic?.seekTo(0, 0)
        exoPlayerVoice?.seekTo(0, 0)
        exoLocalVideoPlayer?.seekTo(0, 0)
        initializePlayersIfNeed(index, null)
        loadMedia("NextMusic")
    }

    @Synchronized
    private fun initializePlayersIfNeed(index: Int, tempCallback: PlayerListener? = null) {
        LogSystem.e(TAG, "initializePlayersIfNeed Invoked Index : ${index}")
        _currentMediaItemIndex = index
        LocalVideoPlayerPropertyManager.isPlayButtonPressed = false

        // Always setup players with current index for single media source approach
        if (exoPlayerMusic == null) {
            setupMusicAudioPlayer(defaultLoadControl(), index, tempCallback)
        } else {
            exoPlayerMusic?.setMediaSource(buildSingleMusicAudioPlayerSource(index))
            exoPlayerMusic?.seekTo(0, playerMusicList[index].lastTimeMusicPosition ?: 0L)
        }

        if (exoPlayerVoice == null) {
            setupNarratorAudioPlayer(
                defaultLoadControl(),
                index,
                tempCallback,
                isNarrator
            )
        } else {
            exoPlayerVoice?.setMediaSource(buildSingleNarratorAudioPlayerSource(index))
            exoPlayerVoice?.seekTo(0, playerMusicList[index].lastTimeMusicPosition ?: 0L)
        }

        if (exoLocalVideoPlayer == null) {
            setupLocalVideoPlayer(defaultLoadControl(), index, tempCallback)
        } else {
            exoLocalVideoPlayer?.setMediaSource(buildSingleLocalVideoPlayerSource(index))
            exoLocalVideoPlayer?.seekTo(0, 0)
        }
    }

    private fun setupSources() {
        val currentIndex = fetchCurrentMediaIndex() ?: 0
        exoPlayerMusic?.setMediaSource(buildSingleMusicAudioPlayerSource(currentIndex))
        exoPlayerVoice?.setMediaSource(buildSingleNarratorAudioPlayerSource(currentIndex))
        exoLocalVideoPlayer?.setMediaSource(buildSingleLocalVideoPlayerSource(currentIndex))
    }


    var _currentMediaItemIndex = 0
    fun fetchCurrentMediaIndex(): Int? {
        return _currentMediaItemIndex
    }

    fun isRemoteClientConnected(): Boolean {
        return remoteMediaClient != null && castContext?.sessionManager?.currentCastSession?.isConnected == true
    }

    fun isRemoteClientPlaying(): Boolean {
        return isRemoteClientConnected() && (remoteMediaClient?.isPlaying == true || remoteMediaClient?.isBuffering == true)
    }

    @Synchronized
    fun play(): Boolean {

        try {
            if (isRemoteClientConnected()) {
                if (!isRemoteClientPlaying()) {
                    remoteMediaClient?.play()
                    isPlaying = true
                    return true
                }
            } else {
                exoPlayerMusic?.play()
                exoPlayerVoice?.play()
                if (LocalVideoPlayerPropertyManager.isPlayButtonPressed) exoLocalVideoPlayer?.play()
                isPlaying = true
                return true
            }
        } finally {
            pauseHueEffect()
            startHueEffect(1)
        }

        return false
    }

    @Synchronized
    fun pause(): Boolean {

        try {
            if (isRemoteClientConnected()) {
                if (isRemoteClientPlaying()) {
                    remoteMediaClient?.pause()
                    isPlaying = false
                    return true
                }
            } else {
                exoPlayerMusic?.pause()
                exoPlayerVoice?.pause()
                exoLocalVideoPlayer?.pause()
                isPlaying = false
                return true
            }
        } finally {
            pauseHueEffect()
        }
        return false
    }

    @Synchronized
    fun pauseForce() {
        remoteMediaClient?.pause()
        exoPlayerMusic?.pause()
        exoPlayerVoice?.pause()
        exoLocalVideoPlayer?.pause()
        isPlaying = false
    }

    fun seekTo(position: Long) {
        LogSystem.e(TAG, "seekTo Invoked position: $position duration: ${exoPlayerMusic?.duration}")
        if (isRemoteClientConnected()) {
            remoteMediaClient?.seek(MediaSeekOptions.Builder().setPosition(position).build())
        } else {
            exoPlayerMusic?.seekTo(position)
            exoPlayerVoice?.seekTo(position)
            exoLocalVideoPlayer?.seekTo(position)
        }
    }

    fun seekToInternal(position: Long) {
        LogSystem.e(
            TAG, "seekToInternal Invoked position: $position duration: ${exoPlayerMusic?.duration}"
        )
        exoPlayerMusic?.seekTo(position)
        exoPlayerVoice?.seekTo(position)
        exoLocalVideoPlayer?.seekTo(position)
    }

    fun startCastingContent(session: CastSession, caller: String?) {
        LogSystem.e(TAG, "RemoteMedia startCastingContent Invoked by $caller")
        playerListener?.hideLoader()
        remoteMediaClient = session.remoteMediaClient
        loadMedia(remoteMediaClient, "RemoteMedia startCastingContent")
    }

    fun loadMedia(caller: String?) {
        if (remoteMediaClient != null) loadMedia(remoteMediaClient, "${caller}>>PlayerManager#590")
    }

    val remoteMediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onMediaError(p0: MediaError) {
            super.onMediaError(p0)
            LogSystem.e(TAG, "RemoteMedia Error : ${p0.toJson()}")
            remoteMediaClient?.stop()
            remoteMediaClient = null
            AppCastManager.castContext?.sessionManager?.endCurrentSession(true)
            playerListener?.onRemoteMediaError(mediaError = p0)
            play()
        }

        override fun onStatusUpdated() {
            super.onStatusUpdated()
            val playerState = remoteMediaClient?.playerState
            var idleReason = remoteMediaClient?.idleReason
            LogSystem.e(TAG, "RemoteMedia State : $playerState")
            when (playerState) {
                MediaStatus.PLAYER_STATE_LOADING, MediaStatus.PLAYER_STATE_BUFFERING -> {
                    // The media is buffering
                    playerListener?.onPlaybackStateChanged(Player.STATE_BUFFERING)
                    pauseHueEffect()
                }

                MediaStatus.PLAYER_STATE_PAUSED -> {
                    isPlaying = false
                    //seekToInternal(remoteMediaClient?.approximateStreamPosition?:0)
                    playerListener?.onPlaybackStateChanged(Player.STATE_READY)
                }

                MediaStatus.PLAYER_STATE_PLAYING -> {
                    isPlaying = true
                    //seekToInternal(remoteMediaClient?.approximateStreamPosition?:0)
                    playerListener?.onPlaybackStateChanged(Player.STATE_READY)
                    startHueEffect()
                }

                MediaStatus.PLAYER_STATE_IDLE -> {
                    if (idleReason == MediaStatus.IDLE_REASON_FINISHED || idleReason == MediaStatus.IDLE_REASON_ERROR) {
                        playerListener?.onPlaybackStateChanged(Player.STATE_ENDED)
                    }
                    playerListener?.onPlaybackStateChanged(Player.STATE_IDLE)
                }

                else -> {}
            }
        }
    }

    fun loadMedia(remoteMediaClient: RemoteMediaClient?, caller: String? = null) {
        LogSystem.e(TAG, "RemoteMedia loadRemoteMedia Invoked By $caller")
        exoPlayerMusic?.pause()
        exoPlayerVoice?.pause()
        exoLocalVideoPlayer?.pause()

        var index = fetchCurrentMediaIndex() ?: 0
        LogSystem.e(TAG, "RemoteMedia loadRemoteMedia Index of Music Pack ${index}")
        var musicItem = playerMusicList[index]
        // That is for current application language
        val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE, "en")

        // "lan" for the language vise
        var lanVideoFileStream: String = musicItem?.getStreamFile(lan, isNarrator) ?: ""
        var lanLyrics: String = musicItem?.getLyricFile(lan) ?: ""
        LogSystem.e(
            TAG,
            "RemoteMedia loadMedia [${musicItem.songName}] lanVideoFileStream : $lanVideoFileStream"
        )

        // Prepare MediaInfo with the video URL and subtitle track (if available)
        val mediaInfoBuilder =
            MediaInfo.Builder(lanVideoFileStream).setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)

        if (!lanLyrics.isNullOrBlank()) {
            //lanLyrics = "https://admin.zenimmersive.com/api/lyrics/1746623588_lyrics.vtt"
            //Add Subtitle Track from Lyric VTT file url
            mediaInfoBuilder.setTextTrackStyle(TextTrackStyle())
            mediaInfoBuilder.setMediaTracks(
                listOf(
                    MediaTrack.Builder(1, MediaTrack.TYPE_TEXT).setName("Lyrics")
                        .setSubtype(MediaTrack.SUBTYPE_SUBTITLES).setContentId(lanLyrics)
                        .setLanguage(lan ?: "en") // specify the language of your subtitles
                        .build()
                )
            )
        }

        val mediaInfo = mediaInfoBuilder.build()

        var position = getPlayerPosition()
        var duration = getPlayerDuration()
        if ((duration - position) <= 5000) {
            LogSystem.e(
                TAG,
                "RemoteMedia Position : $position Duration : $${exoPlayerMusic?.duration ?: 0}"
            )
            position = 0
        }

        LogSystem.e(
            TAG,
            "RemoteMedia Load Remote Media Position : $position Narrative : $isNarrator"
        )
        // Load the media on the remote device
        var mediaOptions =
            MediaLoadRequestData.Builder().setMediaInfo(mediaInfo).setCurrentTime(position)
        if ((!lanLyrics.isNullOrBlank())) mediaOptions =
            mediaOptions.setActiveTrackIds(longArrayOf(1L)) // specify the subtitle track id

        remoteMediaClient?.load(
            mediaOptions.build()
        )
        try {
            remoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
        } catch (e: Exception) {

        }
        remoteMediaClient?.registerCallback(remoteMediaClientCallback)
    }

    private fun getMediaSource(url: String?, music: AlbumMusic?): ProgressiveMediaSource {
        val uri = Uri.parse(url)
        val dataSource = DefaultDataSource.Factory(context).createDataSource()
        return ProgressiveMediaSource.Factory { dataSource }
            .createMediaSource(MediaItem.fromUri(uri))
    }

    /*private fun getMediaSource(audioFileMusic : List<AudioFileNarrator?>): ProgressiveMediaSource {
        val uri = Uri.parse(audioFileMusic.first()?.file?:"")
        val dataSource = DefaultHttpDataSource.Factory().createDataSource()
        return ProgressiveMediaSource.Factory { dataSource }
            .createMediaSource(MediaItem.fromUri(uri))
    }*/

    private fun releasePlayers() {
        exoPlayerVoice?.seekTo(0)
        exoPlayerMusic?.seekTo(0)
        exoLocalVideoPlayer?.seekTo(0)

        exoPlayerMusic?.release()
        exoPlayerVoice?.release()
        exoLocalVideoPlayer?.release()

        exoPlayerMusic = null
        exoPlayerVoice = null
        exoLocalVideoPlayer = null
    }

    @Synchronized
    fun isPlayerReady(): Boolean {
//        return isMusicReady && isVoiceReady
        return isMusicReady && isVoiceReady
    }

    fun changeMusicPlayerVolume(volume: Float) {
        exoPlayerMusic?.let { musicPlayer ->
            musicPlayer.volume = volume / 100f
        }
        if (isRemoteClientConnected()) remoteMediaClient?.setStreamVolume(volume / 100.0)
    }

    fun changeVoicePlayerVolume(volume: Float) {
        exoPlayerVoice?.let { musicPlayer ->
            musicPlayer.volume = volume / 100f
        }
        if (isRemoteClientConnected()) remoteMediaClient?.setStreamVolume(volume / 100.0)
    }

    fun getPlayerDuration(): Long {
        if (isPlayerReady()) {
            val isRemoteClientPlaying = remoteMediaClient?.isPlaying == true
            if (isRemoteClientPlaying) {
                var duration = remoteMediaClient?.streamDuration ?: 0L
                return duration
            }
            val duration = exoPlayerMusic?.duration ?: 0
            return duration
        }
        return 0
    }

    fun getPlayerPosition(): Long {
        if (isPlayerReady()) {
            val isRemoteClientPlaying =
                remoteMediaClient?.isPlaying == true || remoteMediaClient?.isPaused == true
            if (isRemoteClientPlaying) {
                var position = remoteMediaClient?.approximateStreamPosition ?: 0L
//                LogSystem.e(
//                    TAG,
//                    "Remote Player Position: $position Duration : ${remoteMediaClient?.streamDuration}"
//                )
                return position
            }
            val position = exoPlayerMusic?.currentPosition ?: 0
//            LogSystem.e(
//                TAG,
//                "Local Player Position: $position Duration : ${exoPlayerMusic?.duration}"
//            )
            return position
        }
        return 0
    }

    fun endCurrentSession(stopCasting: Boolean) {
        castContext?.sessionManager?.endCurrentSession(stopCasting)
        remoteMediaClient = null
    }

    fun isPlayerBuffering(): Boolean {
        var isRemoteClientBuffering = remoteMediaClient?.isBuffering == true
        var isMusicBuffering = exoPlayerMusic?.playbackState == Player.STATE_BUFFERING
        var isVoiceBuffering = exoPlayerVoice?.playbackState == Player.STATE_BUFFERING
        return isRemoteClientBuffering || isMusicBuffering || isVoiceBuffering
    }

    fun changeNarratorState(checked: Boolean) {
        if (isRemoteClientPlaying()) seekToInternal(getPlayerPosition())
        isNarrator = checked
    }

    fun togglePlayPause() {
        if (isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun destroy() {
        isPlaying = false
        pauseHueEffect()
        releasePlayers()
        releaseResources()
        playerManagerInstance = null
    }

    fun getNarratorState() = isNarrator
    fun getMusicPlayerVolume(): Int = ((exoPlayerMusic?.volume ?: 0f) * 100f).roundToInt()
    fun getVoicePlayerVolume(): Int = ((exoPlayerVoice?.volume ?: 1f) * 100f).roundToInt()

    companion object {
        private var playerManagerInstance: PlayerManager? = null
        fun getInstance(context: Context): PlayerManager? {
            if (playerManagerInstance == null) playerManagerInstance = PlayerManager(context)
            return playerManagerInstance
        }

        fun getInstance(): PlayerManager? {
            return playerManagerInstance
        }
    }

    var staticColorSelected = false
    var staticBrightness: Int? = null
    var hueLightManager = HueLightManager.getInstance(context)
    var isColorChanged = java.util.concurrent.atomic.AtomicBoolean(false)

    fun applyHueEffect(caller : String? = null) {
        LogSystem.e("HueLightClient", "$TAG applyHueEffect Invoked by $caller")
        if (isPlaying) startHueEffect()
        if (isPlayerBuffering()) return
        LogSystem.e("HueLightClient", "$TAG applyHueEffect Processing")


        var position = getPlayerPosition()
        if (!staticColorSelected) {
            if (position == 0L && isColorChanged.get()) {
                clearColor()
                isColorChanged.set(false)
            }
            //Chnage Color Logic
            HueColorManager.changeViewColorDirectly(playerListener?.getHueColorView(), position)

            if (hueLightManager?.isBridgeFunctional() == true) {
                if (position > 10) {
                    hueLightManager?.let { hueLightManager ->
                        HueColorManager.changeColor(
                            hueLightManager, position, playerListener?.getHueColorView()
                        )
                    }
                } else {
                    hueLightManager?.changeColor()
                    playerListener?.getHueColorView()
                        ?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
                isColorChanged.set(true)
            }
        }
    }

    private var handler: Handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable = Runnable {
        applyHueEffect("Runnable")
    }

    fun startHueEffect(delay: Long = 500) {
        handler.postDelayed(runnable, delay)
    }


    private fun pauseHueEffect() {
        LogSystem.e("HueLightClient", "$TAG pauseHueEffect Invoked")
        handler.removeCallbacks(runnable)
        handler.removeCallbacksAndMessages(null)
    }

    fun isSubtitleEnabled(): Boolean {
        // Get the currently active track IDs
        val activeTrackIds = remoteMediaClient?.mediaStatus?.activeTrackIds ?: return false
        // Check if the subtitle track ID (1) is in the list of active track IDs
        return activeTrackIds.contains(1L)
    }

    fun disableSubtitle() {
        remoteMediaClient?.setActiveMediaTracks(longArrayOf())
    }

    fun hasSubtitle(): Boolean {
        val mediaTracks = remoteMediaClient?.mediaInfo?.mediaTracks ?: return false
        return mediaTracks.any { it.type == MediaTrack.TYPE_TEXT }
    }

    fun enableSubtitle() {
        if (hasSubtitle()) {
            //Check if there is subtitles
            remoteMediaClient?.setActiveMediaTracks(longArrayOf(1L))
        }
    }

    fun bindPlayerListener(playerListener: PlayerListener) {
        AppCastManager.playerListener = playerListener
        this.playerListener = playerListener
    }

    fun releaseResources() {
        pauseForce()
        handler.removeCallbacksAndMessages(null)
        playerListener = null
        playerManagerInstance = null
    }

    private var _musicItem: AlbumMusic? = null
    fun getCurrentMusicItem() =
        _musicItem ?: fetchCurrentMediaIndex()?.let { playerMusicList.getOrNull(it) }

    fun reload() {
        var index = fetchCurrentMediaIndex() ?: 0
        var position = getPlayerPosition()
        playerMusicList[index].lastTimeMusicPosition = position
        var musicItem = getCurrentMusicItem()
        val lastPlayVoicePlayerVolume = getVoicePlayerVolume().toFloat()
        disableSubtitle()
        pauseForce()
        releasePlayers()
        initializePlayers(
            musicItem,
            index,
            java.util.ArrayList(playerMusicList),
            tempCallback = object : PlayerListener {
                override fun onPlaybackStateChanged(state: Int) {
                }

                override fun onPlayerStateChanged(musicReady: Boolean, voiceReady: Boolean) {
                    if (isPlayerReady()) {
                        play()
                        changeVoicePlayerVolume(lastPlayVoicePlayerVolume)
                    }
                }

                override fun onSessionEnded() {}

                override fun hideLoader() {}

                override fun onCastSessionDisconnected() {}

                override fun onCastSessionConnected() {}

                override fun onPlayerError(message: String?) {}

                override fun getHueColorView(): View? = null

                override fun songNotPurchased(
                    currentMusicItem: AlbumMusic?, allDirectMusicList: ArrayList<AlbumMusic>
                ) {

                }

                override fun onRemoteMediaError(mediaError: MediaError) {

                }

            },
            true
        )
    }

    fun setupLocalVideoPlayerView(exoPlayerView: PlayerView) {
        LogSystem.e(TAG, "setupLocalVideoPlayerView Invoked")
        exoPlayerView.player = exoLocalVideoPlayer
    }

    fun playLocalVideoPlayer() {
        playLocalPlayerSync()
    }

    fun playLocalPlayerSync() {
        exoLocalVideoPlayer?.seekTo(getPlayerPosition())
        exoLocalVideoPlayer?.play()
        if (!isPlaying) {
            play()
        }
    }

    fun pauseLocalVideoPlayer() {
        exoLocalVideoPlayer?.pause()
    }

    fun isVideoPlayerReady(): Boolean {
        return isVideoReady
    }

    fun isVideoPlayerPlaying(): Boolean {
        return exoLocalVideoPlayer?.isPlaying == true
    }

    var lyricLines: List<LyricLine>? = null

    /**
     * Check if we're currently playing from a local file
     * Returns true only if the local file path is different from the server URL
     * and the local file actually exists
     */
    private fun isPlayingFromLocalFile(localPath: String?, serverUrl: String): Boolean {
        if (localPath.isNullOrEmpty() || serverUrl.isEmpty()) return false

        // If local path equals server URL, we're playing from server
        if (localPath == serverUrl) return false

        // Check if local file actually exists
        val localFile = File(localPath)
        return localFile.exists()
    }

    /**
     * Build detailed error message for better debugging
     */
    private fun buildDetailedErrorMessage(
        error: PlaybackException,
        playerType: String,
        index: Int,
        serverUrl: String,
        localPath: String?
    ): String {
        val errorType = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "NETWORK_CONNECTION_FAILED"
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "NETWORK_CONNECTION_TIMEOUT"
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "FILE_NOT_FOUND"
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "BAD_HTTP_STATUS"
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> "INVALID_HTTP_CONTENT_TYPE"
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE -> "READ_POSITION_OUT_OF_RANGE"
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "CONTAINER_MALFORMED"
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "MANIFEST_MALFORMED"
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "CONTAINER_UNSUPPORTED"
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> "MANIFEST_UNSUPPORTED"
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "DECODER_INIT_FAILED"
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> "DECODER_QUERY_FAILED"
            PlaybackException.ERROR_CODE_DECODING_FAILED -> "DECODING_FAILED"
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> "FORMAT_UNSUPPORTED"
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED -> "AUDIO_TRACK_INIT_FAILED"
            PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> "AUDIO_TRACK_WRITE_FAILED"
            PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR -> "DRM_CONTENT_ERROR"
            PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> "DRM_LICENSE_ACQUISITION_FAILED"
            PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED -> "DRM_PROVISIONING_FAILED"
            PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED -> "DRM_SCHEME_UNSUPPORTED"
            PlaybackException.ERROR_CODE_REMOTE_ERROR -> "REMOTE_ERROR"
            PlaybackException.ERROR_CODE_TIMEOUT -> "TIMEOUT"
            PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK -> "FAILED_RUNTIME_CHECK"
            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> "BEHIND_LIVE_WINDOW"
            PlaybackException.ERROR_CODE_UNSPECIFIED -> "UNSPECIFIED"
            else -> "UNKNOWN_ERROR_CODE_${error.errorCode}"
        }

        val isLocalFile = isPlayingFromLocalFile(localPath, serverUrl)
        val fileInfo = if (isLocalFile) {
            val file = File(localPath!!)
            "Local file exists: ${file.exists()}, Size: ${file.length()} bytes, Modified: ${file.lastModified()}"
        } else {
            "Playing from server URL"
        }

        return """
            |=== $playerType ERROR DETAILS ===
            |Index: $index
            |Error Code: ${error.errorCode} ($errorType)
            |Error Message: ${error.message}
            |Server URL: $serverUrl
            |Local Path: $localPath
            |$fileInfo
            |Is Playing From Local: $isLocalFile
            |Music Pack: ${playerMusicList.getOrNull(index)?.songName} (ID: ${
            playerMusicList.getOrNull(
                index
            )?.songId
        })
            |Narrator Mode: $isNarrator
            |===============================
        """.trimMargin()
    }
}
