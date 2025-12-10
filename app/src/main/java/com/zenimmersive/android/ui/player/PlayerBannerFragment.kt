package com.zenimmersive.android.ui.player

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.databinding.FragmentPlayerBannerBinding
import com.zenimmersive.android.helper.Constants
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import com.zenimmersive.android.helper.KeyStorage.Companion.getInstance
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.helper.fadeIn
import com.zenimmersive.android.helper.fadeOut
import com.zenimmersive.android.helper.hide
import com.zenimmersive.android.helper.setEnabledState
import com.zenimmersive.android.helper.show
import com.zenimmersive.android.helper.toSeconds
import com.zenimmersive.android.repository.BlankRepository
import com.zenimmersive.android.ui.payment.SubscriptionBSFragment
import com.zenimmersive.android.ui.player.LocalVideoPlayerPropertyManager.isLocalPlayerPageExpanded
import com.zenimmersive.android.ui.player.LocalVideoPlayerPropertyManager.isLyricDisplay
import com.zenimmersive.android.viewmodel.BlankViewModel

@SuppressLint("UnsafeOptInUsageError")
class PlayerBannerFragment :
    BaseFragment<BlankViewModel, FragmentPlayerBannerBinding, BlankRepository>(),
    IPlayerSubPage, SubscriptionBSFragment.BottomSheetDismissListener {
    private val TAG = "PlayerBannerFragment"
    var musicPack: AlbumMusic? = null
    var isPlayButtonPressed = false
    private var isBottomSheetLaunched = false
    
    // Track if dynamic layouts have been inflated
    private var isExoPlayerInflated = false
    private var isPurchaseLayoutInflated = false
    
    // References to dynamically inflated views
    private var exoPlayerContainer: com.zenimmersive.android.helper.RoundedFrameView? = null
    private var exoPlayerViewContainer: FrameLayout? = null
    private var exoPlayerView: androidx.media3.ui.PlayerView? = null
    private var overlayBlocker: View? = null
    private var ivTogglePlayerViewExpand: android.widget.ImageView? = null
    private var ivToggleLocalVideoLyric: android.widget.ImageView? = null
    private var textLocalVideoLyricView: android.widget.TextView? = null
    private var viewVideoPlayerButton: FrameLayout? = null
    private var ivVideoPlayerState: android.widget.ImageView? = null
    private var bufferLoaderVideoView: android.widget.ProgressBar? = null
    
    // Purchase layout views
    private var layoutSongPurchaseOverlay: View? = null
    private var layoutBuy: FrameLayout? = null
    private var txtPrice: android.widget.TextView? = null
    private var txtDuration: android.widget.TextView? = null
    private var ivGuided: android.widget.ImageView? = null
    private var txtGuided: android.widget.TextView? = null

    override fun getViewModel(): Class<BlankViewModel> = BlankViewModel::class.java

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPlayerBannerBinding = FragmentPlayerBannerBinding.inflate(inflater, container, false)

    override fun getRepository(): BlankRepository = BlankRepository(requireContext())

    private val videoUIHandler = Handler(Looper.getMainLooper())

    /**
     * Inflates ExoPlayer layout dynamically when needed
     * Only call when song has video file and is accessible (purchased or free)
     */
    private fun ensureExoPlayerInflated() {
        if (isExoPlayerInflated || !isSafe()) return
        
        try {
            val stub = viewBinding.stubExoPlayerContainer
            val inflatedView = stub.inflate()
            
            // Cache all ExoPlayer view references
            exoPlayerContainer = inflatedView.findViewById(R.id.exoPlayerContainer)
            exoPlayerViewContainer = inflatedView.findViewById(R.id.exoPlayerViewContainer)
            exoPlayerView = inflatedView.findViewById(R.id.exoPlayerView)
            overlayBlocker = inflatedView.findViewById(R.id.overlayBlocker)
            ivTogglePlayerViewExpand = inflatedView.findViewById(R.id.ivTogglePlayerViewExpand)
            ivToggleLocalVideoLyric = inflatedView.findViewById(R.id.ivToggleLocalVideoLyric)
            textLocalVideoLyricView = inflatedView.findViewById(R.id.textLocalVideoLyricView)
            viewVideoPlayerButton = inflatedView.findViewById(R.id.viewVideoPlayerButton)
            ivVideoPlayerState = inflatedView.findViewById(R.id.ivVideoPlayerState)
            bufferLoaderVideoView = inflatedView.findViewById(R.id.bufferLoaderVideoView)
            
            isExoPlayerInflated = true
            setupExoPlayerListeners()
            
            LogSystem.e(TAG, "ExoPlayer layout inflated dynamically")
        } catch (e: Exception) {
            LogSystem.e(TAG, "Error inflating ExoPlayer: ${e.message}")
        }
    }

    /**
     * Inflates Purchase layout dynamically when needed
     * Only call when song is paid and not purchased/subscribed
     */
    private fun ensurePurchaseLayoutInflated() {
        if (isPurchaseLayoutInflated || !isSafe()) return
        
        try {
            val stub = viewBinding.stubPurchaseLayout
            layoutSongPurchaseOverlay = stub.inflate()

            // Cache all purchase layout view references
            layoutBuy = layoutSongPurchaseOverlay?.findViewById(R.id.purchaseLayoutBuy)
            txtPrice = layoutSongPurchaseOverlay?.findViewById(R.id.purchaseLayoutPriceText)
            txtDuration = layoutSongPurchaseOverlay?.findViewById(R.id.purchaseLayoutDurationText)
            ivGuided = layoutSongPurchaseOverlay?.findViewById(R.id.purchaseLayoutGuidedImage)
            txtGuided = layoutSongPurchaseOverlay?.findViewById(R.id.purchaseLayoutGuidedText)

            isPurchaseLayoutInflated = true
            LogSystem.e(TAG, "Purchase layout inflated dynamically")
        } catch (e: Exception) {
            LogSystem.e(TAG, "Error inflating Purchase layout: ${e.message}")
        }
    }

    /**
     * Setup ExoPlayer-specific listeners after inflation
     */
    private fun setupExoPlayerListeners() {
        ivTogglePlayerViewExpand?.setOnClickListener {
            it.fadeOut()
            isLocalPlayerPageExpanded = !isLocalPlayerPageExpanded
            viewVideoPlayerButton?.fadeOut()
            (parentFragment as PlayerFragment?)?.setupLocalPlayerPage()
        }

        overlayBlocker?.setOnClickListener {
            (parentFragment as PlayerFragment?)?.onLocalPlayerViewPressed()
        }

        viewVideoPlayerButton?.setOnClickListener {
            handlePlayButton()
        }

        ivToggleLocalVideoLyric?.setOnClickListener {
            if (musicPack?.hasLyricFile(
                    getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE)
                ) == true) {
                isLyricDisplay = !isLyricDisplay
                setupLyric()
            }
        }
        
        exoPlayerView?.useController = false
        exoPlayerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        exoPlayerView?.setOnTouchListener { _, _ -> false }
        overlayBlocker?.setOnTouchListener { _, _ -> false }
    }


    override fun registerObservers() {
        musicPack = arguments?.getSerializable("musicPack") as AlbumMusic
        viewBinding.ivMusicPackBanner.setImageResource(0)
        viewBinding.ivMusicPackBanner.setImageDrawable(null)
        viewBinding.ivMusicPackBanner.setImageBitmap(null)

        LogSystem.e(
            "PlayerBannerFragment",
            "Name : ${musicPack?.songName} URL : ${
                musicPack?.getBackgroundVertical(
                    KeyStorage.getInstance(
                        requireContext()
                    ).getString(APP_SELECTED_LANGUAGE)
                )
            }"
        )
        var requestOptions = RequestOptions()
        requestOptions.transform(CenterCrop())
        var requestURL = "https://placehold.co/150x150/png"
        if (musicPack != null) {
            if (musicPack?.getBackgroundVertical(
                    KeyStorage.getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE)
                )?.isNullOrBlank() == false
            ) {
                requestURL = musicPack?.getBackgroundVertical(
                    KeyStorage.getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE)
                )!!
            }
        }

        Glide.with(requireContext()).load(requestURL)
            .apply(requestOptions)
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder_error)
            .into(viewBinding.ivMusicPackBanner)

        setupView(musicPack!!, PlayerManager.getInstance())
    }

    private fun setupListener() {
        // Listener setup is now done in setupExoPlayerListeners() after inflation
        // This method is kept for backward compatibility but no longer needed
    }


    @SuppressLint("UnsafeOptInUsageError")
    private fun setupView(music: AlbumMusic, playerManager: PlayerManager?) {
        isLyricDisplay = false
        val lan = getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE)
        
        // Determine if we need to show purchase UI
        val shouldShowPurchase = KeyStorage.getInstance(requireContext())
            .shouldShowPaidStatus(requireContext(), music)
        
        // Determine if song is accessible (purchased or free)
        val isSongAccessible = (music.isPurchased == 1 || music.isPaid == 0)
        
        // Determine if song has video
        val hasVideoFile = music.hasVideoFile(lan)

        // SCENARIO 1: Song needs purchase - inflate only purchase layout
        if (shouldShowPurchase) {
            updatePurchaseUI(music)
            // Don't inflate ExoPlayer if purchase is required
            LogSystem.e(TAG, "Song requires purchase - only showing purchase UI")
            return
        }
        
        // SCENARIO 2: Song is accessible and has video - inflate ExoPlayer
        if (isSongAccessible && hasVideoFile) {
            ensureExoPlayerInflated()
            
            exoPlayerViewContainer?.hide()
            
            viewVideoPlayerButton?.fadeIn()
            viewVideoPlayerButton?.setEnabledState(true)

            viewBinding.root.post {
                (parentFragment as PlayerFragment?)?.setupLocalPlayerPage()
                if (LocalVideoPlayerPropertyManager.isPlayButtonPressed) {
                    exoPlayerView?.let { playerManager?.setupLocalVideoPlayerView(it) }
                    exoPlayerViewContainer?.show()
                    viewVideoPlayerButton?.fadeOut()
                    changeLyricTextPosition(true)
                }
                setupLyric()
            }
            
            LogSystem.e(TAG, "Song accessible with video - ExoPlayer inflated")
            return
        }
        
        // SCENARIO 3: Song is accessible but no video - just show banner
        if (isSongAccessible && !hasVideoFile) {
            LogSystem.e(TAG, "Song accessible but no video - only banner shown")
            return
        }
        
        // SCENARIO 4: Edge case - show banner only
        LogSystem.e(TAG, "Default scenario - only banner shown")
    }

    private fun setupLyric() {
        if (!isExoPlayerInflated) return
        
        if (isLyricDisplay && musicPack?.hasLyricFile(
                getInstance(requireContext()).getString(
                    APP_SELECTED_LANGUAGE
                )
            ) == true
        ) {
            ivToggleLocalVideoLyric?.setImageResource(R.drawable.ic_local_video_lyric_off)
            textLocalVideoLyricView?.fadeIn()
        } else {
            ivToggleLocalVideoLyric?.setImageResource(R.drawable.ic_local_video_lyric)
            textLocalVideoLyricView?.fadeOut()
        }
    }

    private fun handlePlayButton() {
        if (!isExoPlayerInflated) return
        
        var playerManager = PlayerManager.getInstance()
        val lan = getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE)
        if (musicPack!!.hasVideoFile(lan)) {
            LocalVideoPlayerPropertyManager.isPlayButtonPressed = true
            if (playerManager?.isVideoPlayerPlaying() == true) {
                playerManager.pause()
            } else {
                playerManager?.playLocalVideoPlayer()
                exoPlayerView?.let { playerManager?.setupLocalVideoPlayerView(it) }
            }

            exoPlayerViewContainer?.show()
            viewVideoPlayerButton?.fadeOut()
        }
    }

    override fun unregisterObservers() {

    }

    fun updatePlayerUI(currentData: AlbumMusic) {
        if(!isSafe()) return
        
        var hasVideoFile = currentData.hasVideoFile(
            getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE)
        )
        
        if (isExoPlayerInflated) {
            viewVideoPlayerButton?.visibility = if (hasVideoFile) View.VISIBLE else View.GONE
        }
        
        updatePurchaseUI(currentData)
    }

    fun showPlayerButton() {
        if(!isSafe() || !isExoPlayerInflated) return
        viewVideoPlayerButton?.fadeIn()
        viewVideoPlayerButton?.setEnabledState(true)
    }

    fun hidePlayerButton() {
        if(!isSafe() || !isExoPlayerInflated) return
        viewVideoPlayerButton?.fadeOut()
        viewVideoPlayerButton?.setEnabledState(false)
    }

    fun togglePlayerButton() {
        if(!isSafe() || !isExoPlayerInflated) return
        viewVideoPlayerButton?.let {
            it.visibility = if (it.isVisible) View.GONE else View.VISIBLE
        }
    }

    fun updatePlayerButtonState(playerManager: PlayerManager?) {
        if(!isSafe() || !isExoPlayerInflated) return
        ivVideoPlayerState?.setImageResource(
            if (playerManager?.isVideoPlayerPlaying() == true)
                R.drawable.ic_play_cont_pause_huge_video
            else R.drawable.ic_play_cont_play_huge_video
        )
    }

    fun showLocalVideoPlayer() {
        if(!isSafe() || !isExoPlayerInflated) return
        exoPlayerContainer?.show()
    }

    fun hideLocalVideoPlayer() {
        if(!isSafe() || !isExoPlayerInflated) return
        exoPlayerContainer?.hide()
    }

    fun handleVideoPlaybackState(
        playbackState: Int,
        playerManager: PlayerManager?
    ) {
        if(!isSafe() || !isExoPlayerInflated) return
        
        if (exoPlayerView?.player == null) {
            exoPlayerView?.let { playerManager?.setupLocalVideoPlayerView(it) }
        }
        
        when (playbackState) {
            Player.STATE_READY -> {
                if (playerManager?.isVideoPlayerReady() == true) {
                    bufferLoaderVideoView?.hide()
                }
                viewVideoPlayerButton?.setEnabledState(true)
            }

            Player.STATE_BUFFERING -> {
                bufferLoaderVideoView?.show()
                viewVideoPlayerButton?.setEnabledState(false)
            }
        }

        updatePlayerButtonState(playerManager)
    }

    fun handlePlayerError(
        playerManager: PlayerManager?
    ) {
        if(!isSafe() || !isExoPlayerInflated) return
        bufferLoaderVideoView?.hide()
        viewVideoPlayerButton?.setEnabledState(false)
        viewVideoPlayerButton?.hide()
    }

    override fun setPlayerPosition(currentPosition: Long) {
        if(!isSafe() || !isExoPlayerInflated) return
        
        var lyricLines = PlayerManager.getInstance()?.lyricLines
        if ((!isLyricDisplay) || lyricLines.isNullOrEmpty()) return

        for (i in lyricLines!!.indices) {
            val currentLine = lyricLines!![i]
            val nextLine = if (i + 1 < lyricLines!!.size) lyricLines!![i + 1] else null

            if (nextLine != null) {
                // What if lyric text start from 10 seconds but player at the
                if (currentPosition <= currentLine.timestamp && nextLine.timestamp > currentPosition) {
                    textLocalVideoLyricView?.text =
                        Html.fromHtml(currentLine.text, Html.FROM_HTML_MODE_COMPACT)
                    break
                }
            }
            if (nextLine == null || (currentPosition >= currentLine.timestamp && currentPosition < nextLine.timestamp)) {
                textLocalVideoLyricView?.text =
                    Html.fromHtml(currentLine.text, Html.FROM_HTML_MODE_COMPACT)
                break
            }
        }
    }

    override fun pageExpanded(localPlayerPageExpanded: Boolean) {
        if(!isSafe() || !isExoPlayerInflated) return
        
        var defaultMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._15sdp)
        ivTogglePlayerViewExpand?.setImageResource(if (localPlayerPageExpanded) R.drawable.ic_collaps_view else R.drawable.ic_expan_view)
        
        if (localPlayerPageExpanded) {
            var params =
                ivTogglePlayerViewExpand?.layoutParams as? FrameLayout.LayoutParams
            params?.topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._60sdp)
            params?.marginStart = defaultMargin
            params?.marginEnd = defaultMargin
            params?.bottomMargin = defaultMargin
            ivTogglePlayerViewExpand?.layoutParams = params
            exoPlayerContainer?.setRadius(0)
        } else {
            var params =
                ivTogglePlayerViewExpand?.layoutParams as? FrameLayout.LayoutParams
            params?.topMargin = defaultMargin
            params?.marginStart = defaultMargin
            params?.marginEnd = defaultMargin
            params?.bottomMargin = defaultMargin
            ivTogglePlayerViewExpand?.layoutParams = params
            exoPlayerContainer?.setRadius(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._5sdp)
            )
        }

        if ((musicPack?.hasLyricFile(
                getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE)
            ) == true) && localPlayerPageExpanded
        ) {
            ivToggleLocalVideoLyric?.fadeIn()
        } else ivToggleLocalVideoLyric?.fadeOut()

        ivTogglePlayerViewExpand?.fadeIn()
    }

    fun checkAutoStart() {
        if(!isSafe() || !isExoPlayerInflated) return
        
        if (isPlayButtonPressed) {
            PlayerManager.getInstance()?.playLocalVideoPlayer()
            exoPlayerView?.let { PlayerManager.getInstance()?.setupLocalVideoPlayerView(it) }
            exoPlayerViewContainer?.show()
            viewVideoPlayerButton?.fadeOut()
        }
    }

    fun changeLyricTextPosition(isControlVisible: Boolean) {
        if(!isSafe() || !isExoPlayerInflated) return
        
        var defaultMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._50sdp)
        var params = textLocalVideoLyricView?.layoutParams as? FrameLayout.LayoutParams
        params?.bottomMargin =
            if (isControlVisible) resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._200sdp) else defaultMargin
        textLocalVideoLyricView?.layoutParams = params
    }

    fun hidePlayerUI() {
        if(!isSafe() || !isExoPlayerInflated) return
        exoPlayerContainer?.hide()
    }

    fun showPlayerUI() {
        if(!isSafe()) return
        
        if (musicPack?.hasVideoFile(
                getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE)
            ) == true
        ) {
            if (isExoPlayerInflated) {
                exoPlayerContainer?.show()
                viewVideoPlayerButton?.fadeIn()
            }
        }
    }
    fun updatePurchaseUI(currentData: AlbumMusic) {
        if (!isSafe()) return

        val language = getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE)
        val isEnglish = language.equals("en", ignoreCase = true)

        // Show or hide the purchase/subscription layout based on song and user subscription
        val shouldShowPurchase = KeyStorage.getInstance(requireContext())
            .shouldShowPaidStatus(requireContext(), currentData)
        
        if (!shouldShowPurchase) {
            // Hide purchase layout if it's inflated
            layoutSongPurchaseOverlay?.visibility = View.GONE
            return
        }
        
        // Inflate purchase layout if needed
        ensurePurchaseLayoutInflated()
        
        layoutSongPurchaseOverlay?.visibility = View.VISIBLE

        // Fetch and set the song price from Google Play Billing
        if (!currentData.productId.isNullOrEmpty()) {
            txtPrice?.text = productSku?.oneTimePurchaseOfferDetails?.formattedPrice ?: "${currentData?.cost?:"0"} €"
            // Set buy button click listener to launch purchase screen
            layoutBuy?.setOnClickListener {
                launchPurchaseScreen(currentData, arrayListOf())
            }
            
            // Set song duration in the include layout's txtDuration
            txtDuration?.text = currentData.getMusicDurationString()

            // Guided / Unguided visibility
            val guidedValue = if (isEnglish) currentData.isGuided else currentData.isFGuided

            if (guidedValue == 1) {
                ivGuided?.setImageResource(R.drawable.ic_microphone)
                txtGuided?.setText(getString(R.string.guided))
            } else {
                ivGuided?.setImageResource(R.drawable.ic_block_microphone)
                txtGuided?.setText(getString(R.string.unguided))
            }
        } else {
            txtPrice?.text = "--"
        }
    }

    private var productSku : com.android.billingclient.api.ProductDetails? = null
    private fun queryPurchaseHelper(currentData: AlbumMusic?) {
        currentData?.let { currentData->
            layoutSongPurchaseOverlay?.post {
                val purchaseHelper =
                    com.zenimmersive.android.ui.payment.PurchaseHelper.getInstance(requireContext())
                purchaseHelper.fetchProductById(
                    currentData.productId ?: "",
                    object : com.zenimmersive.android.ui.payment.PurchaseHelper.FetchProductListener {
                        override fun onProductFetched(_productSku: com.android.billingclient.api.ProductDetails) {
                            if(!isSafe()) return
                            activity?.runOnUiThread {
                                synchronized(this@PlayerBannerFragment) {
                                    productSku = _productSku
                                    val price = productSku?.oneTimePurchaseOfferDetails?.formattedPrice ?: "--"
                                    LogSystem.e(
                                        TAG,
                                        "onProductFetched: ProductId: ${currentData.productId} Formatted PRICE: $price"
                                    )
                                }
                            }
                        }

                        override fun onProductFetchError(debugMessage: String) {
                            LogSystem.e(TAG, "onProductFetchError: $debugMessage")
                            productSku = null
                        }
                    })
            }
        }
    }

    fun checkIfPaidMusicPack(playerManager: PlayerManager?) {
        if (!isSafe()) return

        musicPack?.let { currentMusic ->
            // Preview time limit for unpurchased/unsubscribed songs
            val prevSeconds = currentMusic.previewLength.toSeconds(45)
            val shouldLimit = (currentMusic.isPurchased ?: 0) == 0
            
            if (shouldLimit) {
                ensurePurchaseLayoutInflated()
                layoutSongPurchaseOverlay?.visibility = View.VISIBLE
                val currentPositionSec = (playerManager?.getPlayerPosition() ?: 0L) / 1000
                
                if (currentPositionSec >= prevSeconds) {
                    if (playerManager?.isPlaying == true) {
                        (parentFragment as? PlayerFragment)?.autoPlayStart = false
                        playerManager.seekTo(prevSeconds * 1000L)
                        playerManager.pauseForce()
                        // Notify parent fragment to update play state
                        (parentFragment as? PlayerFragment)?.updatePlayerUI("checkIfPaidMusicPack")
                    }
                    // Show purchase/subscription UI if not already visible
                    if (layoutSongPurchaseOverlay?.visibility != View.VISIBLE) {
                        layoutSongPurchaseOverlay?.visibility = View.VISIBLE
                    }
                }
            } else {
                layoutSongPurchaseOverlay?.visibility = View.GONE
            }
        }
    }

    fun hidePurchaseLayout() {
        if (isSafe() && isPurchaseLayoutInflated) {
            layoutSongPurchaseOverlay?.visibility = View.GONE
        }
    }

    fun showPurchaseLayout() {
        if (isSafe()) {
            ensurePurchaseLayoutInflated()
            layoutSongPurchaseOverlay?.visibility = View.VISIBLE
        }
    }

    private fun launchPurchaseScreen(
        currentMusicItem: AlbumMusic?,
        allDirectMusicList: ArrayList<AlbumMusic>
    ) {
        if (isBottomSheetLaunched) return
        isBottomSheetLaunched = true
        
        val bundle = Bundle()
        bundle.putInt(Constants.SONG_ID, currentMusicItem?.songId!!)
        bundle.putSerializable(Constants.SONG_DATA, currentMusicItem)
        bundle.putSerializable(Constants.All_SONGS, allDirectMusicList)
        
        val bottomSheet = SubscriptionBSFragment()
        bottomSheet.bindBottomSheetDismissListener(this)
        bottomSheet.arguments = bundle
        bottomSheet.show(childFragmentManager, "SubscriptionBSFragment")
    }

    override fun onBottomSheetDismissed(musicPack: AlbumMusic?) {
        Log.d(TAG, "onBottomSheetDismissed: called")
        isBottomSheetLaunched = false

        // Auto-resume playback if the song is now purchased or covered by subscription
        val currentSong = this@PlayerBannerFragment.musicPack
        if (musicPack?.isMatch(currentSong) == true) {
            val shouldShowPurchase = currentSong?.let {
                KeyStorage.getInstance(requireContext())
                    .shouldShowPaidStatus(requireContext(), it)
            } ?: false

            LogSystem.e(
                TAG,
                "onBottomSheetDismissed: should show subscription purchase sheet? $shouldShowPurchase"
            )

            if (!shouldShowPurchase) {
                PlayerManager.getInstance()?.play()
                hidePurchaseLayout()
                // Notify parent to update UI
                (parentFragment as? PlayerFragment)?.updatePlayerUI("purchaseComplete")
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if(PlayerManager.getInstance()?.getCurrentMusicItem()?.isPaid == 1) queryPurchaseHelper(PlayerManager.getInstance()?.getCurrentMusicItem())
    }

    companion object {
        fun newInstance(
            musicPack: AlbumMusic,
            previousVideoPlayState: Boolean,
            isLocalPlayerPageExpanded: Boolean
        ): PlayerBannerFragment {
            val fragment = PlayerBannerFragment()
            val args = Bundle()
            args.putSerializable("musicPack", musicPack)
            fragment.arguments = args
            fragment.isPlayButtonPressed = previousVideoPlayState
            return fragment
        }
    }
}