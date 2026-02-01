package com.zenimmersive.android.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.View
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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
import com.zenimmersive.android.helper.toSeconds
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

    private var wakeLock: PowerManager.WakeLock? = null
    private var isCasting = false
    private var isLoadingCastMedia = false
    private var lastCastPosition: Long = 0L
    
    // Video sync controller for A/V synchronization
    private var videoSyncController: VideoSyncController? = null

    var isNarrator = false
    private var isMusicReady = false
    private var isVoiceReady = false
    // Some items may be narration-only (no music file) or music-only (no narration file).
    private var hasMusicSource = true
    private var hasVoiceSource = true

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
        
        // Initialize video sync controller
        videoSyncController = VideoSyncController()
        
        initializeWakeLock()
    }

    // Preview Timer for unpurchased music
    private var previewTimer: Handler = Handler(Looper.getMainLooper())
    private var previewRunnable: Runnable? = null
    private val PREVIEW_DURATION_MS = 60_000L // 1 minute
    private var isPreviewTimerActive = false

    private fun startPreviewTimer(musicItem: AlbumMusic?) {
        // Cancel any existing timer
        stopPreviewTimer()

        if (musicItem == null) return

        // Only start timer for unpurchased music that is paid
        val isPaid = musicItem.isPaid == 1
        val isPurchased = musicItem.isPurchased == 1
        
        if (isPaid && !isPurchased) {
            LogSystem.e(TAG, "Starting preview timer for unpurchased music: ${musicItem.songName}")
            isPreviewTimerActive = true

            previewRunnable = Runnable {
                LogSystem.e(TAG, "Preview time expired for: ${musicItem.songName}")
                handlePreviewExpired(musicItem)
            }
            
            // Calculate remaining preview time if resuming
            // (Simply using fixed 1 minute for now as per requirements "stops after one minute")
            // Ideally we could track accumulated time, but starting fresh 1m from play/resume 
            // or enforcing absolute limit is a design choice. 
            // Based on user "stops after one minute", fixed duration from start of session is safer.
            // But since AlbumMusic has previewLength, let's respect that if available, else default to 60s
            val duration = if (musicItem.previewLength != null) {
                 musicItem.previewLength!!.toSeconds(60) * 1000L
            } else {
                 PREVIEW_DURATION_MS
            }
            
            // If already played beyond preview limit (e.g. seeking), stop immediately
            val currentPos = getPlayerPosition()
            if (currentPos >= duration) {
                 handlePreviewExpired(musicItem)
            } else {
                 // Schedule stop for remaining time
                 val remaining = duration - currentPos
                 if (remaining > 0) {
                     previewTimer.postDelayed(previewRunnable!!, remaining)
                 } else {
                     handlePreviewExpired(musicItem)
                 }
            }
        }
    }

    private fun stopPreviewTimer() {
        if (isPreviewTimerActive) {
            LogSystem.e(TAG, "Stopping preview timer")
            previewRunnable?.let { previewTimer.removeCallbacks(it) }
            previewRunnable = null
            isPreviewTimerActive = false
        }
    }

    private fun handlePreviewExpired(musicItem: AlbumMusic) {
        LogSystem.e(TAG, "Preview expired, stopping playback")
        isPreviewTimerActive = false

        // Pause playback
        pauseForce()
        
        // Seek to preview limit to prevent just hitting play again and continuing
        val duration = if (musicItem.previewLength != null) {
                musicItem.previewLength!!.toSeconds(60) * 1000L
        } else {
                PREVIEW_DURATION_MS
        }
        seekTo(duration)

        // Notify listener to show purchase dialog
        playerListener?.songNotPurchased(musicItem, playerMusicList)
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
        
        // Stop any existing preview timer when initializing new players
        stopPreviewTimer()

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
                            getAdaptiveLoadControl(),
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

    /**
     * Build AudioAttributes for meditation content with proper audio focus handling
     */
    private fun buildAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }

    /**
     * Check if device is connected to WiFi
     */
    private fun isWifiConnected(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (e: Exception) {
            LogSystem.e(TAG, "Error checking WiFi connection: ${e.message}")
            false
        }
    }

    /**
     * Get adaptive load control based on network type
     * WiFi: Larger buffers for smoother playback
     * Cellular: Smaller buffers to save data
     */
    private fun getAdaptiveLoadControl(): DefaultLoadControl {
        val isWifi = isWifiConnected()
        LogSystem.e(TAG, "Creating LoadControl for ${if (isWifi) "WiFi" else "Cellular"} connection")
        
        return if (isWifi) {
            // WiFi: Larger buffers for smoother playback
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    20_000,  // minBufferMs - 20s for WiFi
                    40_000,  // maxBufferMs - 40s for WiFi
                    2_500,   // bufferForPlaybackMs - keep default 2.5s
                    5_000    // bufferForPlaybackAfterRebufferMs - keep default 5s
                )
                .setBackBuffer(10_000, false)  // Keep 10s back buffer on WiFi
                .build()
        } else {
            // Cellular: Smaller buffers to save data
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    10_000,  // minBufferMs - 10s for cellular
                    20_000,  // maxBufferMs - 20s for cellular
                    2_500,   // bufferForPlaybackMs - keep default 2.5s
                    5_000    // bufferForPlaybackAfterRebufferMs - keep default 5s
                )
                .setBackBuffer(5_000, false)  // Keep 5s back buffer on cellular
                .build()
        }
    }

    @Deprecated("Use getAdaptiveLoadControl() instead for network-aware buffering")
    private fun defaultLoadControl() =
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,  // minBufferMs - reduced from 50s default to 15s
                30_000,  // maxBufferMs - reduced from 50s default to 30s
                2_500,   // bufferForPlaybackMs - keep default 2.5s
                5_000    // bufferForPlaybackAfterRebufferMs - keep default 5s
            )
            .setBackBuffer(5_000, false)  // Keep 5s back buffer, don't retain when paused
            .build()


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
            hasVoiceSource = false
            exoPlayerVoice?.release()
            exoPlayerVoice = null
            isVoiceReady = true
            playerListener?.onPlayerStateChanged(isMusicReady, isVoiceReady)
            tempCallback?.onPlayerStateChanged(isMusicReady, isVoiceReady)
            return
        }

        hasVoiceSource = true

        exoPlayerVoice = (exoPlayerVoice ?: ExoPlayer.Builder(context)
            .setRenderersFactory(DefaultRenderersFactory(context).setEnableDecoderFallback(true))
            .setLoadControl(loadControl)
            .setAudioAttributes(buildAudioAttributes(), false)
            .build()).apply {
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
                            // If we paused music due to voice buffering, resume it when voice is ready again.
                            if (isPlaying && hasMusicSource && isMusicReady && exoPlayerMusic?.isPlaying != true) {
                                exoPlayerMusic?.play()
                            }
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
                            getAdaptiveLoadControl(),
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
        val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE, "en")
        val audioFileUrl = audioFile.getAudioFile(lan)
        if (audioFileUrl.isEmpty()) {
            // Narration-only item (or missing music file). Avoid creating a broken ExoPlayer source.
            hasMusicSource = false
            exoPlayerMusic?.release()
            exoPlayerMusic = null
            isMusicReady = true
            playerListener?.onPlayerStateChanged(isMusicReady, isVoiceReady)
            tempCallback?.onPlayerStateChanged(isMusicReady, isVoiceReady)
            return
        } else {
            hasMusicSource = true
        }

        exoPlayerMusic = (exoPlayerMusic ?: ExoPlayer.Builder(context)
            .setRenderersFactory(DefaultRenderersFactory(context).setEnableDecoderFallback(true))
            .setLoadControl(loadControl)
            .setAudioAttributes(buildAudioAttributes(), true)  // Enable audio focus handling
            .build()).apply {
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
                            // If we paused voice due to music buffering, resume it when music is ready again.
                            if (isPlaying && hasVoiceSource && isVoiceReady && exoPlayerVoice?.isPlaying != true) {
                                exoPlayerVoice?.play()
                            }
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

                        setupMusicAudioPlayer(getAdaptiveLoadControl(), index, tempCallback)
                        setupNarratorAudioPlayer(
                            getAdaptiveLoadControl(),
                            index,
                            tempCallback,
                            isNarrator
                        )
                        setupLocalVideoPlayer(getAdaptiveLoadControl(), index, tempCallback)
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
        // Stop timer when changing tracks
        stopPreviewTimer()
        LogSystem.e(
            TAG,
            "previousMusicPlay Invoked isNarrator : ${isNarrator} Current Index : ${_currentMediaItemIndex}"
        )
        var index = _currentMediaItemIndex - 1
        if (index < 0) {
            index = playerMusicList.size - 1
        }
        _currentMediaItemIndex = index

        playerMusicList[index].lastTimeMusicPosition = 0L

        // Clear subtitles when switching tracks during cast
        if (isRemoteClientConnected()) {
            disableSubtitle()
            LogSystem.e(TAG, "Chromecast: Subtitles cleared before switching to previous track")
        }

        releasePlayers()
        setupSources()
        exoPlayerMusic?.seekTo(0, 0)
        exoPlayerVoice?.seekTo(0, 0)
        exoLocalVideoPlayer?.seekTo(0, 0)
        initializePlayersIfNeed(index, null)
        loadMedia("PreviousMusic")
    }

    fun nextMusicPlay() {
        // Stop timer when changing tracks
        stopPreviewTimer()
        LogSystem.e(
            TAG,
            "nextMusicPlay Invoked isNarrator : ${isNarrator} Current Index : ${_currentMediaItemIndex}"
        )
        var index = _currentMediaItemIndex + 1
        if (index >= playerMusicList.size) {
            index = 0
        }
        _currentMediaItemIndex = index

        playerMusicList[index].lastTimeMusicPosition = 0L

        // Clear subtitles when switching tracks during cast
        if (isRemoteClientConnected()) {
            disableSubtitle()
            LogSystem.e(TAG, "Chromecast: Subtitles cleared before switching to next track")
        }

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
        val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE, "en")
        val currentItem = playerMusicList.getOrNull(index)
        val currentMusicUrl = currentItem?.getAudioFile(lan) ?: ""
        val currentNarratorUrl = currentItem?.getNarratorAudioFile(lan) ?: ""
        hasMusicSource = currentMusicUrl.isNotEmpty()
        hasVoiceSource = currentNarratorUrl.isNotEmpty()

        // Always setup players with current index for single media source approach
        if (!hasMusicSource) {
            exoPlayerMusic?.release()
            exoPlayerMusic = null
            isMusicReady = true
        } else if (exoPlayerMusic == null) {
            setupMusicAudioPlayer(getAdaptiveLoadControl(), index, tempCallback)
        } else {
            exoPlayerMusic?.setMediaSource(buildSingleMusicAudioPlayerSource(index))
            exoPlayerMusic?.seekTo(0, playerMusicList[index].lastTimeMusicPosition ?: 0L)
        }

        if (!hasVoiceSource) {
            exoPlayerVoice?.release()
            exoPlayerVoice = null
            isVoiceReady = true
        } else if (exoPlayerVoice == null) {
            setupNarratorAudioPlayer(
                getAdaptiveLoadControl(),
                index,
                tempCallback,
                isNarrator
            )
        } else {
            exoPlayerVoice?.setMediaSource(buildSingleNarratorAudioPlayerSource(index))
            exoPlayerVoice?.seekTo(0, playerMusicList[index].lastTimeMusicPosition ?: 0L)
        }

        if (exoLocalVideoPlayer == null) {
            setupLocalVideoPlayer(getAdaptiveLoadControl(), index, tempCallback)
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
                    releaseWakeLock()
                    return true
                }
            } else {
                exoPlayerMusic?.play()
                exoPlayerVoice?.play()
                if (LocalVideoPlayerPropertyManager.isPlayButtonPressed) {
                    exoLocalVideoPlayer?.play()
                    // Start video sync when video is playing
                    startVideoSync()
                }
                // Always acquire wake lock when playing (audio or video) to keep screen on during meditation
                acquireWakeLock()
                isPlaying = true
                
                // Start preview timer for unpurchased music
                val currentMusic = getCurrentMusicItem()
                startPreviewTimer(currentMusic)
                
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
                
                // Stop video sync when paused
                stopVideoSync()
                
                // Stop preview timer when paused
                stopPreviewTimer()
                
                isPlaying = false
                releaseWakeLock()
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
        
        // Stop video sync when force paused
        stopVideoSync()
        
        // Stop preview timer when force paused
        stopPreviewTimer()
        
        isPlaying = false
        releaseWakeLock()
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
        if (remoteMediaClient != null && !isLoadingCastMedia) {
            loadMedia(remoteMediaClient, "${caller}>>PlayerManager#590")
        } else if (isLoadingCastMedia) {
            LogSystem.e(TAG, "RemoteMedia Skipping loadMedia from $caller - already loading")
        }
    }

    val remoteMediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onMediaError(p0: MediaError) {
            super.onMediaError(p0)
            LogSystem.e(TAG, "RemoteMedia Error : ${p0.toJson()}")
            LogSystem.e(TAG, "Chromecast: Media error occurred, clearing subtitle state")
            remoteMediaClient?.stop()
            remoteMediaClient = null
            isCasting = false
            isLoadingCastMedia = false
            AppCastManager.castContext?.sessionManager?.endCurrentSession(true)
            playerListener?.onRemoteMediaError(mediaError = p0)
            // Reinitialize local players and resume playback
            reinitializeLocalPlayersAfterCast()
        }

        override fun onStatusUpdated() {
            super.onStatusUpdated()
            val playerState = remoteMediaClient?.playerState
            var idleReason = remoteMediaClient?.idleReason
            
            // Log subtitle state for debugging
            val activeTrackIds = remoteMediaClient?.mediaStatus?.activeTrackIds
            val hasActiveSubtitle = activeTrackIds?.contains(1L) ?: false
            LogSystem.e(TAG, "Chromecast: Status update - Player state: $playerState, Active subtitle: $hasActiveSubtitle, Track IDs: ${activeTrackIds?.joinToString()}")
            
            LogSystem.e(TAG, "RemoteMedia State : $playerState")
            
            // Sync position continuously during cast
            val currentPosition = remoteMediaClient?.approximateStreamPosition ?: 0L
            if (currentPosition > 0) {
                lastCastPosition = currentPosition
                val index = fetchCurrentMediaIndex() ?: 0
                playerMusicList.getOrNull(index)?.lastTimeMusicPosition = currentPosition
            }
            
            // Log buffering info for A/V sync debugging
            val mediaStatus = remoteMediaClient?.mediaStatus
            if (mediaStatus != null) {
                LogSystem.e(TAG, "Chromecast A/V Debug: Position=${currentPosition}ms, PlaybackRate=${mediaStatus.playbackRate}, " +
                    "StreamDuration=${remoteMediaClient?.streamDuration}ms")
            }
            
            when (playerState) {
                MediaStatus.PLAYER_STATE_LOADING, MediaStatus.PLAYER_STATE_BUFFERING -> {
                    LogSystem.e(TAG, "Chromecast: Buffering - this may affect A/V sync")
                    playerListener?.onPlaybackStateChanged(Player.STATE_BUFFERING)
                    pauseHueEffect()
                    isLoadingCastMedia = false
                }

                MediaStatus.PLAYER_STATE_PAUSED -> {
                    isPlaying = false
                    playerListener?.onPlaybackStateChanged(Player.STATE_READY)
                    isLoadingCastMedia = false
                }

                MediaStatus.PLAYER_STATE_PLAYING -> {
                    isPlaying = true
                    playerListener?.onPlaybackStateChanged(Player.STATE_READY)
                    startHueEffect()
                    isLoadingCastMedia = false
                }

                MediaStatus.PLAYER_STATE_IDLE -> {
                    if (idleReason == MediaStatus.IDLE_REASON_FINISHED || idleReason == MediaStatus.IDLE_REASON_ERROR) {
                        LogSystem.e(TAG, "Chromecast: Media ended or error, clearing subtitle state")
                        playerListener?.onPlaybackStateChanged(Player.STATE_ENDED)
                    }
                    playerListener?.onPlaybackStateChanged(Player.STATE_IDLE)
                    isLoadingCastMedia = false
                }

                else -> {}
            }
        }
    }

    fun loadMedia(remoteMediaClient: RemoteMediaClient?, caller: String? = null) {
        LogSystem.e(TAG, "RemoteMedia loadRemoteMedia Invoked By $caller")
        
        // Prevent redundant calls
        if (isLoadingCastMedia) {
            LogSystem.e(TAG, "RemoteMedia loadMedia already in progress, skipping call from $caller")
            return
        }
        
        isLoadingCastMedia = true
        isCasting = true
        
        // Clear existing subtitles before loading new media
        try {
            disableSubtitle()
            LogSystem.e(TAG, "Chromecast: Subtitles cleared before loading new media from $caller")
        } catch (e: Exception) {
            LogSystem.e(TAG, "Chromecast: Error clearing subtitles: ${e.message}")
        }
        
        // Store current position before releasing players
        val currentPosition = getPlayerPosition()
        var index = fetchCurrentMediaIndex() ?: 0
        if (currentPosition > 0) {
            playerMusicList.getOrNull(index)?.lastTimeMusicPosition = currentPosition
        }
        
        // Properly stop and release local players to free resources
        LogSystem.e(TAG, "RemoteMedia Releasing local players to free resources")
        exoPlayerMusic?.stop()
        exoPlayerMusic?.release()
        exoPlayerMusic = null
        
        exoPlayerVoice?.stop()
        exoPlayerVoice?.release()
        exoPlayerVoice = null
        
        exoLocalVideoPlayer?.stop()
        exoLocalVideoPlayer?.release()
        exoLocalVideoPlayer = null
        
        // Release wake lock during cast
        releaseWakeLock()

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
        
        // Enhanced logging for debugging URL selection and media loading
        LogSystem.e(TAG, "Chromecast Media Debug Info:")
        LogSystem.e(TAG, "  - Song: ${musicItem.songName} (ID: ${musicItem.songId})")
        LogSystem.e(TAG, "  - Language: $lan")
        LogSystem.e(TAG, "  - Narrator Mode: $isNarrator")
        LogSystem.e(TAG, "  - Video URL: $lanVideoFileStream")
        LogSystem.e(TAG, "  - Lyrics URL: $lanLyrics")
        LogSystem.e(TAG, "  - Has Subtitle: ${!lanLyrics.isNullOrBlank()}")
        
        // Log available media files for debugging
        LogSystem.e(TAG, "Available Media Files:")
        LogSystem.e(TAG, "  - audioFileMusic (EN): ${musicItem.audioFileMusic}")
        LogSystem.e(TAG, "  - audioFileMusicFrench (FR): ${musicItem.audioFileMusicFrench}")
        LogSystem.e(TAG, "  - videoFileStream (EN): ${musicItem.videoFileStream}")
        LogSystem.e(TAG, "  - videoFileStreamFrench (FR): ${musicItem.videoFileStreamFrench}")
        LogSystem.e(TAG, "  - narratorMusicVideoFile (EN): ${musicItem.narratorMusicVideoFile}")
        LogSystem.e(TAG, "  - narratorMusicVideoFileFrench (FR): ${musicItem.narratorMusicVideoFileFrench}")

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

        // Stop video sync before releasing players
        stopVideoSync()

        exoPlayerMusic?.release()
        exoPlayerVoice?.release()
        exoLocalVideoPlayer?.release()

        exoPlayerMusic = null
        exoPlayerVoice = null
        exoLocalVideoPlayer = null
        
        releaseWakeLock()
    }

    @Synchronized
    fun isPlayerReady(): Boolean {
        return (!hasMusicSource || isMusicReady) && (!hasVoiceSource || isVoiceReady)
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
            val musicDuration = if (hasMusicSource) (exoPlayerMusic?.duration ?: 0L) else 0L
            val voiceDuration = if (hasVoiceSource) (exoPlayerVoice?.duration ?: 0L) else 0L
            return maxOf(musicDuration, voiceDuration)
        }
        return 0
    }

    fun getPlayerPosition(): Long {
        if (isPlayerReady()) {
            val isRemoteClientPlaying =
                remoteMediaClient?.isPlaying == true || remoteMediaClient?.isPaused == true
            if (isRemoteClientPlaying) {
                var position = remoteMediaClient?.approximateStreamPosition ?: 0L
                return position
            }
            val musicPos = if (hasMusicSource) (exoPlayerMusic?.currentPosition ?: 0L) else 0L
            val voicePos = if (hasVoiceSource) (exoPlayerVoice?.currentPosition ?: 0L) else 0L
            val videoPos = exoLocalVideoPlayer?.currentPosition ?: 0L
            
            // Log A/V sync info when video is playing
            if (videoPos > 0 && (musicPos > 0 || voicePos > 0)) {
                val audioDrift = videoPos - maxOf(musicPos, voicePos)
                if (kotlin.math.abs(audioDrift) > 100) { // Log only if drift > 100ms
                    LogSystem.e(TAG, "A/V Sync: Drift detected: ${audioDrift}ms (video=$videoPos, music=$musicPos, voice=$voicePos)")
                }
            }
            
            // Use max to avoid "stuck" UI when one of the players is paused/missing.
            return maxOf(musicPos, voicePos, videoPos)
        }
        return 0
    }

    fun endCurrentSession(stopCasting: Boolean) {
        // Clear subtitles when cast session ends
        try {
            disableSubtitle()
            LogSystem.e(TAG, "Chromecast: Subtitles cleared as cast session is ending")
        } catch (e: Exception) {
            LogSystem.e(TAG, "Chromecast: Error clearing subtitles on session end: ${e.message}")
        }
        
        castContext?.sessionManager?.endCurrentSession(stopCasting)
        remoteMediaClient = null
        isCasting = false
        isLoadingCastMedia = false
        // Reinitialize local players when cast ends
        reinitializeLocalPlayersAfterCast()
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
        stopPreviewTimer() // Stop preview timer
        stopVideoSync() // Stop video sync
        releasePlayers()
        releaseResources()
        releaseWakeLock()
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
        val isEnabled = activeTrackIds.contains(1L)
        LogSystem.e(TAG, "Chromecast: Subtitle enabled check - Result: $isEnabled, Active tracks: ${activeTrackIds.joinToString()}")
        return isEnabled
    }

    fun disableSubtitle() {
        LogSystem.e(TAG, "Chromecast: Disabling subtitles - clearing active tracks")
        remoteMediaClient?.setActiveMediaTracks(longArrayOf())
    }

    fun hasSubtitle(): Boolean {
        val mediaTracks = remoteMediaClient?.mediaInfo?.mediaTracks ?: return false
        val hasSubtitle = mediaTracks.any { it.type == MediaTrack.TYPE_TEXT }
        LogSystem.e(TAG, "Chromecast: Has subtitle check - Result: $hasSubtitle, Total tracks: ${mediaTracks.size}")
        return hasSubtitle
    }

    fun enableSubtitle() {
        if (hasSubtitle()) {
            //Check if there is subtitles
            LogSystem.e(TAG, "Chromecast: Enabling subtitle track ID 1")
            remoteMediaClient?.setActiveMediaTracks(longArrayOf(1L))
        } else {
            LogSystem.e(TAG, "Chromecast: Cannot enable subtitle - no subtitle tracks available")
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
        acquireWakeLock()
        
        // Start video sync when video starts playing
        startVideoSync()
        
        if (!isPlaying) {
            play()
        }
    }

    fun pauseLocalVideoPlayer() {
        exoLocalVideoPlayer?.pause()
        
        // Stop video sync when video pauses
        stopVideoSync()
        
        releaseWakeLock()
    }
    
    /**
     * Start continuous A/V synchronization for local video playback
     * Uses VideoSyncController to keep video in sync with audio
     */
    private fun startVideoSync() {
        val videoPlayer = exoLocalVideoPlayer
        if (videoPlayer != null && videoPlayer.isPlaying) {
            videoSyncController?.start(
                videoPlayer = videoPlayer,
                masterClockProvider = { 
                    // Audio position is the master clock
                    val musicPos = if (hasMusicSource) (exoPlayerMusic?.currentPosition ?: 0L) else 0L
                    val voicePos = if (hasVoiceSource) (exoPlayerVoice?.currentPosition ?: 0L) else 0L
                    maxOf(musicPos, voicePos)
                },
                videoPlayerReady = { isVideoReady }
            )
            LogSystem.e(TAG, "A/V Sync: Video synchronization started")
        }
    }
    
    /**
     * Stop A/V synchronization
     */
    private fun stopVideoSync() {
        if (videoSyncController?.isActive() == true) {
            videoSyncController?.stop()
            LogSystem.e(TAG, "A/V Sync: Video synchronization stopped")
        }
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

    /**
     * Initialize wake lock to keep screen on during video playback
     */
    private fun initializeWakeLock() {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                "ZenZone:VideoPlaybackWakeLock"
            )
            wakeLock?.setReferenceCounted(false)
            LogSystem.e(TAG, "Wake lock initialized successfully")
        } catch (e: Exception) {
            LogSystem.e(TAG, "Failed to initialize wake lock: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == false) {
                // 10 hour timeout as safety measure to prevent indefinite battery drain
                wakeLock?.acquire(10 * 60 * 60 * 1000L)
                LogSystem.e(TAG, "Wake lock acquired with 10h timeout - screen will stay on")
            }
        } catch (e: Exception) {
            LogSystem.e(TAG, "Failed to acquire wake lock: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                LogSystem.e(TAG, "Wake lock released - screen can turn off")
            }
        } catch (e: Exception) {
            LogSystem.e(TAG, "Failed to release wake lock: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Reinitialize local players after casting ends
     * Restores playback at the last cast position
     */
    private fun reinitializeLocalPlayersAfterCast() {
        try {
            LogSystem.e(TAG, "Reinitializing local players after cast ended")
            val index = fetchCurrentMediaIndex() ?: 0
            val musicItem = playerMusicList.getOrNull(index) ?: return
            
            // Use last synced position from cast
            val resumePosition = musicItem.lastTimeMusicPosition ?: lastCastPosition
            LogSystem.e(TAG, "Resuming local playback at position: $resumePosition")
            
            // Reinitialize players with stored position
            initializePlayersIfNeed(index, object : PlayerListener {
                override fun onPlaybackStateChanged(state: Int) {}
                override fun onPlayerStateChanged(musicReady: Boolean, voiceReady: Boolean) {
                    if (isPlayerReady()) {
                        // Resume playback automatically
                        play()
                        LogSystem.e(TAG, "Local playback resumed after cast")
                    }
                }
                override fun onSessionEnded() {}
                override fun hideLoader() {}
                override fun onCastSessionDisconnected() {}
                override fun onCastSessionConnected() {}
                override fun onPlayerError(message: String?) {}
                override fun getHueColorView(): View? = null
                override fun songNotPurchased(
                    currentMusicItem: AlbumMusic?,
                    allDirectMusicList: ArrayList<AlbumMusic>
                ) {}
                override fun onRemoteMediaError(mediaError: MediaError) {}
            })
        } catch (e: Exception) {
            LogSystem.e(TAG, "Error reinitializing players after cast: ${e.message}")
            e.printStackTrace()
        }
    }
}
