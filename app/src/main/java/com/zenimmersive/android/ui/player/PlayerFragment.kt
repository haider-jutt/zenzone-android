package com.zenimmersive.android.ui.player

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.mediarouter.app.MediaRouteDialogFactory
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.zenimmersive.android.R
import com.zenimmersive.android.adapter.FragmentPagerAdapter
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.apiresponsemodel.Artist
import com.zenimmersive.android.apiresponsemodel.Tag
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.ActivityPlayerBinding
import com.zenimmersive.android.helper.Configrations
import com.zenimmersive.android.helper.Constants.All_SONGS
import com.zenimmersive.android.helper.Constants.NOTIFICATION_SONG_ID
import com.zenimmersive.android.helper.Constants.SONG_DATA
import com.zenimmersive.android.helper.Constants.SONG_ID
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import com.zenimmersive.android.helper.KeyStorage.Companion.getInstance
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.helper.WhiteAmbiencePalette
import com.zenimmersive.android.helper.*
import com.zenimmersive.android.hue.HueColorManager
import com.zenimmersive.android.hue.HueLightManager
import com.zenimmersive.android.hue.TaskCallback
import com.zenimmersive.android.model.LightListResult
import com.zenimmersive.android.repository.PlayerRepository
import com.zenimmersive.android.ui.BackPressListener
import com.zenimmersive.android.ui.DashboardActivity
import com.zenimmersive.android.ui.HueManagementActivity
import com.zenimmersive.android.ui.payment.SubscriptionBSFragment
import com.zenimmersive.android.ui.player.LocalVideoPlayerPropertyManager.isLocalPlayerPageExpanded
import com.zenimmersive.android.ui.player.LocalVideoPlayerPropertyManager.lastInteractionTime
import com.zenimmersive.android.viewmodel.PlayerViewModel
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaError
import com.skydoves.colorpickerview.listeners.ColorListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@UnstableApi
class PlayerFragment : BaseFragment<PlayerViewModel, ActivityPlayerBinding, PlayerRepository>(),
    BackPressListener {

    private val SEEK_UPDATE_INTERVAL_MS = 200L

    private var musicIndex = 0

    private var songId: Int? = null
    private val TAG = "PlayerManagement"
    private var handler : Handler ? = null
    private var hueLightManager: HueLightManager? = null
    var playerManager: PlayerManager? = null
    private var viewPagerAdapter: FragmentPagerAdapter? = null
    private var musicList = arrayListOf<AlbumMusic>()
    private var lastSelectedMusicPosition: Int? = null
    private var selectedLanguage: String? = "en"
    val viewPagerFargList: ArrayList<Fragment> = arrayListOf()

    private val updateSeekBarRunnable = Runnable {
        postSeekBarRunnable()
        if (!isSafe()) return@Runnable
        try {
            updatePlayerUI("updateSeekBarRunnable")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun postSeekBarRunnable() {
        handler?.postDelayed(updateSeekBarRunnable, SEEK_UPDATE_INTERVAL_MS)
    }

    override fun getViewModel(): Class<PlayerViewModel> = PlayerViewModel::class.java
    override fun getActivityBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): ActivityPlayerBinding = ActivityPlayerBinding.inflate(inflater, container, false)

    override fun getRepository(): PlayerRepository = PlayerRepository(requireContext())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeSongDataFromArguments(arguments)
//        PlayerManager.getInstance(requireContext().applicationContext)?.releaseResources()
        LogSystem.e(TAG, "onCreate Invoked")
        selectedLanguage = KeyStorage.getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE)
        if (selectedLanguage.isNullOrEmpty()) selectedLanguage = "en"
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        songId?.let {
            viewModel.postSongAnalysisUse(songId!!)
        }

        viewLoader = viewBinding.invLoader.viewLoader

        viewBinding.root.post { hideLoader() }

        if (activity is DashboardActivity) {
            (activity as DashboardActivity).viewBinding.tvPageTitle.text =
                getString(R.string.playing_now)
        }
    }

    private fun checkIsAnySongFound() {
        if (songId != null) {
            viewBinding.llNotFoundData.visibility = View.GONE
            viewBinding.viewBackgroundImageParent.visibility = View.VISIBLE
            viewBinding.viewMusicPages.visibility = View.VISIBLE
            viewBinding.viewVolumeHueParent.visibility = View.VISIBLE
            viewBinding.viewPlayerControls.visibility = View.VISIBLE
            viewBinding.viewToggleBrightnessPanel.visibility = View.VISIBLE

        } else {
            viewBinding.llNotFoundData.visibility = View.VISIBLE
            viewBinding.viewBackgroundImageParent.visibility = View.GONE
            viewBinding.viewMusicPages.visibility = View.GONE
            viewBinding.viewVolumeHueParent.visibility = View.GONE
            viewBinding.viewPlayerControls.visibility = View.GONE
            viewBinding.viewToggleBrightnessPanel.visibility = View.GONE
        }
    }

    fun initializeSongDataFromArguments(arguments: Bundle?) {
        LogSystem.e(
            TAG,
            "initializeSongDataFromArguments Invoked ${arguments?.keySet()?.toList().toString()}"
        )

        KeyStorage.getInstance().enforceSubscriptionValidity()

        musicIndex = 0
        musicList.clear()
        songId = (PlayerManager.getInstance()?.getCurrentMusicItem()?.songId ?: -1)
        when {
            arguments?.containsKey(NOTIFICATION_SONG_ID) == true -> {
                songId = arguments?.getInt(NOTIFICATION_SONG_ID)
            }

            arguments?.containsKey(SONG_ID) == true -> {

                autoPlayStart = true
                songId = arguments?.getInt(SONG_ID)
                val albumMusic = arguments?.getSerializable(SONG_DATA) as AlbumMusic?
                //albumMusic?.let { musicList.add(albumMusic) }
                val allSongs = arguments?.getSerializable(All_SONGS) as? ArrayList<AlbumMusic?>
                allSongs?.forEachIndexed { index, music ->
                    if (music?.songId == albumMusic?.songId) musicIndex = index
                }
                if (allSongs.isNullOrEmpty()) {
                    albumMusic?.let { musicList.add(albumMusic) }
                } else musicList.addAll(allSongs ?: arrayListOf<AlbumMusic>())
            }

            else -> {
                KeyStorage.getInstance().getCachedMusic()?.let {
                    songId = it.songId
                    musicList.addAll(PlayerManager.getInstance()?.playerMusicList ?: arrayListOf())
                    if (it !in musicList) {
                        musicList.add(it)
                    }
                    musicIndex = musicList.indexOf(it) ?: 0
                    autoPlayStart = PlayerManager.getInstance()?.isPlaying ?: false
                }
            }
        }

        var userJsonString =
            KeyStorage.getInstance(ContextWrapper.getContext()).getString(Constants.USER_DATA, "")
        musicList?.forEach { pack ->
            if (pack.isPaid == 1 && pack.isPurchased == 0) KeyStorage.getInstance()
                .shouldShowPaidStatus(pack, userJsonString)
        }

        if ((PlayerManager.getInstance()?.getCurrentMusicItem()?.songId ?: -2) != songId) {
            PlayerManager.getInstance()?.releaseResources()
        }
    }

    override fun registerObservers() {
        LogSystem.e(TAG, "registerObservers Invoked")
        hueLightManager = HueLightManager.getInstance(requireContext())
        if ((arguments?.getInt(NOTIFICATION_SONG_ID, -1) ?: -1) == -1) {
            initializeMusicPlayer()
            refreshBridgeStatusAsync()
        }

        viewModel.songData.observe(viewLifecycleOwner) { resData ->
            when (resData) {
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
//                    showLoader("Loading...")
                }

                is ViewState.Data -> {
//                    hideLoader()
                    if ((arguments?.getInt(NOTIFICATION_SONG_ID, -1) ?: -1) != -1) {
                        Log.d(TAG, "initObserver SongData livedata notification songid get it.")
                        musicList.clear()
                        musicList.add(resData.data.result!!)
                        initializeMusicPlayer()
                        refreshBridgeStatusAsync()
                    }
                }

                is ViewState.Error -> {
//                    hideLoader()
                    showToast(resData.error)
                }
            }
        }

        viewModel.addRemoveFavoriteSong.observe(viewLifecycleOwner) { resData ->
            when (resData) {
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
//                    showLoader("Loading...")
                }

                is ViewState.Data -> {
                    hideLoader()
                    lastSelectedMusicPosition?.let {
                        musicList[musicIndex].isFavorite = resData.data.isFavorite
                    }
                    if (resData.data.isFavorite == 1) {
                        viewBinding.ivFavorite.setImageResource(R.drawable.ic_heart_selected)
                    } else {
                        viewBinding.ivFavorite.setImageResource(R.drawable.ic_heart)
                    }

                }

                is ViewState.Error -> {
                    hideLoader()
                    showToast(resData.error)
                }
            }
        }

        checkIsAnySongFound()
    }

    override fun unregisterObservers() {

    }

    private fun refreshBridgeStatusAsync() {
        hueLightManager?.refreshBridgeStatus(object : TaskCallback<String> {
            override fun onTaskComplete(result: String) = fetchLightList()
            override fun onTaskError(error: String?) {}
        })
    }

    private fun fetchLightList() {
        hueLightManager?.fetchLights(object : TaskCallback<LightListResult> {
            override fun onTaskComplete(result: LightListResult) {
                val brightness = result.lights?.minOf { it.dimming?.brightness ?: 100f } ?: 0
                viewBinding.sliderBrightness.progress = brightness.toInt()
                if (playerManager?.isPlaying == false) hueLightManager?.changeColor()
            }

            override fun onTaskError(error: String?) {}
        })
    }

    private fun setupMediaRouter() {
        val mediaRouteSelector = MediaRouteSelector.Builder()
            .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
            .build()

        val mediaRouter = MediaRouter.getInstance(requireContext())
        mediaRouter.addCallback(
            mediaRouteSelector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY
        )

        MediaRouteDialogFactory.getDefault().onCreateChooserDialogFragment()
            .apply { routeSelector = mediaRouteSelector }.show(childFragmentManager, "CastDialog")
    }

    private val mediaRouterCallback = object : MediaRouter.Callback() {
        override fun onRouteSelected(
            router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int
        ) {
            showLoader()
        }
    }

    private fun setupEvents() {
        setupFavoriteButton()
        setupBrightnessPanel()
        setupVolumePanel()
        setupPlaybackControls()
        setupHuePanel()

        viewBinding.hueConfigurePanel.setOnClickListener {
            startActivity(Intent(requireContext(), HueManagementActivity::class.java))
        }
    }

    private var debounceJob: Job? = null
    private var brightnessSlider = object :
        SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            updateBrightness(seekBar?.progress ?: 0)
        }

    }

    private fun setupHuePanel() {
        //Brightness Panel
        viewBinding.viewCloseHuePanel.setOnClickListener {
            if (viewBinding.hueControlPanel.isVisible) {
                toggleVisibility(viewBinding.hueControlPanel)
            }
        }
        viewBinding.sliderBrightness.setOnSeekBarChangeListener(brightnessSlider)
        viewBinding.viewToggleBrightnessPanel.setOnClickListener {
            hidePanels()
            viewBinding.brightnessSeekbarParent.hide()
            viewBinding.hueStaticColorParent.hide()
            if (hueLightManager!!.isBridgeFunctional()) {
                viewBinding.brightnessSeekbarParent.show()
                viewBinding.hueStaticColorParent.show()
            }

            toggleVisibility(viewBinding.hueControlPanel)
        }
        viewBinding.hueConfigurePanel.setOnClickListener {
            startActivity(Intent(requireContext(), HueManagementActivity::class.java))
        }

        //Static Color Panel
        viewBinding.hueStaticColorParent.setOnClickListener {
            if (playerManager?.staticColorSelected == true) {
                playerManager?.staticColorSelected = false
                viewBinding.viewStaticColorText.setText(getString(R.string.pick_static_color_text))
                viewBinding.hueColorView.setBackgroundColor(Color.TRANSPARENT)
                updatePlayerUI("hueStaticColorParent")
                return@setOnClickListener
            }
            hidePanels()
            toggleVisibility(viewBinding.viewColorWheelPanel)
        }
        viewBinding.viewCloseColorWheelPanel.setOnClickListener {
            if (viewBinding.viewColorWheelPanel.isVisible) {
                toggleVisibility(viewBinding.viewColorWheelPanel)
                toggleVisibility(viewBinding.hueControlPanel)
            }
        }
        viewBinding.viewColorWhiteAmbienceWheel.setOnClickListener {
            updateSelectionColorWheelUI(viewBinding.viewColorWhiteAmbienceWheel)
            viewBinding.colorPickerView.post {
                var drawable = WhiteAmbiencePalette(
                    resources, viewBinding.colorPickerView.width, viewBinding.colorPickerView.height
                )
                drawable.setBounds(
                    0, 0, viewBinding.colorPickerView.width, viewBinding.colorPickerView.height
                )
                var bitmap = drawable.createBitmap(
                    viewBinding.colorPickerView.width, viewBinding.colorPickerView.height
                )
                viewBinding.colorPickerView.setPaletteDrawable(BitmapDrawable(resources, bitmap))
            }
        }
        viewBinding.viewColorWheel.setOnClickListener {
            updateSelectionColorWheelUI(viewBinding.viewColorWheel)
            viewBinding.colorPickerView.setHsvPaletteDrawable()
        }
        viewBinding.colorPickerView.setColorListener(object : ColorListener {
            override fun onColorSelected(color: Int, fromUser: Boolean) {
                LogSystem.e("Color Change Fire : $color FromUser : $fromUser")
                if (fromUser) {
                    viewBinding.viewStaticColorText.setText(getString(R.string.remove_static_color_text))
                    playerManager?.staticColorSelected = true
                    viewBinding.hueColorView.setBackgroundColor(color)

                    // Debounce the API call
                    debounceJob?.cancel() // Cancel previous job if still active
                    debounceJob = CoroutineScope(Dispatchers.Main).launch {
                        delay(100) // Adjust delay as per your needs
                        hueLightManager?.changeColor(color)
                    }
                }
            }
        })

    }

    private fun hidePanels() {
        viewBinding.volumePanel.hide()
        viewBinding.viewColorWheelPanel.hide()
        viewBinding.hueControlPanel.hide()
    }

    private fun updateSelectionColorWheelUI(view: FrameLayout) {
        viewBinding.viewColorWheel.getChildAt(0).setBackgroundColor(Color.TRANSPARENT)
        viewBinding.viewColorWhiteAmbienceWheel.getChildAt(0).setBackgroundColor(Color.TRANSPARENT)

        view.getChildAt(0).setBackgroundResource(R.drawable.rounded_circle_white)
    }

    private fun setupFavoriteButton() {
        viewBinding.ivFavorite.setOnClickListener {
            val currentSongPos = playerManager?.fetchCurrentMediaIndex() ?: 0
            val currentMusic = musicList.getOrNull(currentSongPos)
            currentMusic?.let {
                lastSelectedMusicPosition = currentSongPos
                val newFavoriteStatus = if (it.isFavorite == 1) 0 else 1
                viewModel.addRemoveFavoriteSong(it.songId!!, newFavoriteStatus)
            }
        }
    }

    private fun setupBrightnessPanel() {
        viewBinding.viewCloseHuePanel.setOnClickListener {
            toggleVisibility(viewBinding.hueControlPanel)
        }
        viewBinding.sliderBrightness.setOnSeekBarChangeListener(brightnessSlider)
    }

    private fun setupVolumePanel() {
        viewBinding.viewCloseVolumePanel.setOnClickListener {
            toggleVisibility(viewBinding.volumePanel)
        }

        viewBinding.viewSwitchNarrative.setOnCheckedChangeListener { _, isChecked ->
            toggleNarration(
                isChecked
            )
        }

        viewBinding.viewToggleVolumePanel.setOnClickListener {
            if (viewBinding.hueControlPanel.isVisible) viewBinding.hueControlPanel.hide()
            toggleVisibility(viewBinding.volumePanel)
        }
        viewBinding.viewSeekbarMusicPlayerVolume.setOnSeekBarChangeListener(
            createVolumeChangeListener(true)
        )
        viewBinding.viewSeekbarVoiceMusicPlayerVolume.setOnSeekBarChangeListener(
            createVolumeChangeListener(false)
        )

        viewBinding.ivCaptionState.setOnClickListener {
            if (playerManager?.hasSubtitle() == true) {
                if (playerManager?.isSubtitleEnabled() == true) {
                    playerManager?.disableSubtitle()
//                    viewBinding.ivCaptionState.setColorFilter(null)
                } else {
                    playerManager?.enableSubtitle()/*viewBinding.ivCaptionState.setColorFilter(
                        resources.getColor(R.color.colorPrimary),
                        android.graphics.PorterDuff.Mode.SRC_IN
                    )*/
                }
            }
        }
    }

    private fun toggleNarration(isChecked: Boolean) {
        if (isChecked) viewBinding.viewVolumControl2.show() else viewBinding.viewVolumControl2.hide()
        playerManager?.apply {
            changeNarratorState(isChecked)
            if (isChecked) {
                viewBinding.viewSeekbarVoiceMusicPlayerVolume.progress =
                    viewBinding.viewSeekbarMusicPlayerVolume.progress
                changeVoicePlayerVolume(
                    viewBinding.viewSeekbarVoiceMusicPlayerVolume.getProgress().toFloat()
                )
            } else {
                changeVoicePlayerVolume(0f)
            }
            loadMedia("ToggleNarration")
        }
    }

    //defaultPlayer is initial call indication
    private fun handleCastButtonPress(defaultPlayer: Boolean = false) {
        LogSystem.e(TAG, "Cast button pressed")
        val session = AppCastManager.castContext?.sessionManager?.currentCastSession
        if (session != null && session.isConnected) {
            if (playerManager?.isRemoteClientConnected() == true) {
                var position = playerManager?.getPlayerPosition() ?: 0
                playerManager?.endCurrentSession(true)
                playerManager?.seekTo(position)
                playerManager?.play()
            } else {
                playerManager?.startCastingContent(session, "CastButtonPress")
            }
            if (defaultPlayer) {
                updateCastButtonUI(true)
            }
        } else {
            if (!defaultPlayer) setupMediaRouter()
        }
    }

    var playStateBeforeSeek = false
    private fun setupPlaybackControls() {
        viewBinding.viewCastButton.setOnClickListener {
            handleCastButtonPress()
        }
        viewBinding.viewPrevious.setOnClickListener { navigateToPreviousMusic() }
        viewBinding.viewNext.setOnClickListener { navigateToNextMusic() }
        viewBinding.ivPlayerState.setOnClickListener { togglePlayPause() }

        viewBinding.viewSeekbarPlayerDuration.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?, progress: Int, fromUser: Boolean
            ) {
                if (fromUser) {
                    val seekPosition = progress * 500L
                    if (!isOverThePreviewSeconds(
                            seekPosition,
                            false
                        )
                    ) HueColorManager.changeViewColorDirectlyNoThread(
                        viewBinding.hueColorView, seekPosition
                    )
                    viewBinding.viewPlayerTimeText.text = convertMsToTime(seekPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                durationSeekEnabled = false
                playStateBeforeSeek = playerManager?.isPlaying ?: false
                playerManager?.pause()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                var seekPosition = ((seekBar?.progress ?: 0) * 500L)
                if (!isOverThePreviewSeconds(seekPosition)) {
                    playerManager?.seekTo(seekPosition)

                    HueColorManager.changeColor(
                        hueLightManager ?: HueLightManager.getInstance(requireContext()),
                        seekPosition,
                        viewBinding.hueColorView
                    )
                }
                if (playStateBeforeSeek) playerManager?.play()
                durationSeekEnabled = true
            }
        })

    }

    fun isOverThePreviewSeconds(seekPosition: Long, touchPlayer: Boolean = true): Boolean {
        val currentMusic = musicList.getOrNull(musicIndex)
        var prevSeconds = currentMusic?.previewLength.toSeconds(45)
        val shouldLimit = (currentMusic?.isPurchased ?: 0) == 0
        if (shouldLimit && seekPosition > (prevSeconds * 1000L)) {
            HueColorManager.changeViewColorDirectlyNoThread(
                viewBinding.hueColorView, (prevSeconds * 1000L)
            )
            viewBinding.viewPlayerTimeText.text = convertMsToTime(seekPosition)
            if (touchPlayer) playerManager?.seekTo(prevSeconds * 1000L)
            return true
        }

        return false
    }

    private fun createVolumeChangeListener(isAudioPlayer: Boolean) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if (isAudioPlayer) {
                        playerManager?.changeMusicPlayerVolume(progress.toFloat())
                        if (playerManager?.isRemoteClientConnected() == true) {
                            viewBinding.viewSeekbarVoiceMusicPlayerVolume.setProgress(progress)
                        }
                    } else {
                        playerManager?.changeVoicePlayerVolume(progress.toFloat())
                        if (playerManager?.isRemoteClientConnected() == true) {
                            viewBinding.viewSeekbarMusicPlayerVolume.setProgress(progress)
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

    private fun togglePlayPause() {
        playerManager?.togglePlayPause()
        updatePlayerUI("togglePlayPause")
    }

    private fun toggleVisibility(view: View) {
        view.visibility = if (view.isVisible) View.GONE else View.VISIBLE
    }

    private fun updateBrightness(progress: Int) {
        playerManager?.staticBrightness = progress
        if (hueLightManager?.isBridgeActive == true) {
            hueLightManager?.changeBrightness(progress.toFloat())
        } else {
            showToast(getString(R.string.error_bridge_not_active))
        }
    }

    private fun navigateToPreviousMusic() {
        playerManager?.previousMusicPlay()
        updateCurrentMusicUI()
    }

    private fun navigateToNextMusic() {
        playerManager?.nextMusicPlay()
        updateCurrentMusicUI()
    }

    private fun updateCurrentMusicUI() {
        val currentSongPos = playerManager?.fetchCurrentMediaIndex() ?: 0
        musicList.getOrNull(currentSongPos)?.let { currentMusic ->
            updateUiData(currentMusic)
            setupViewPager(currentMusic)
        }
    }

    private fun updateUiData(currentData: AlbumMusic) {
        var language = getInstance(requireContext()).getString(
            APP_SELECTED_LANGUAGE
        )
        val isEnglish = language.equals("en", ignoreCase = true)

        Glide.with(requireContext()).load(currentData.getBackgroundVertical(selectedLanguage))
            .into(viewBinding.ivMusicPackBackground)
        viewBinding.tvMusicPackName.text = getLocalizedSongName(currentData)
        viewBinding.tvMusicPackDetails.text =
            Html.fromHtml(getLocalizedTagsAndArtists(currentData), Html.FROM_HTML_MODE_COMPACT)
        viewBinding.viewSwitchNarrative.isChecked = playerManager?.getNarratorState() ?: false
        updateVolumeControls()
        viewBinding.ivFavorite.setImageResource(getFavoriteIconResource(currentData))

        viewBinding.viewVolumControl1.visibility = if (currentData.hasAudioFile(language)
        ) View.VISIBLE else View.GONE

        var hasNarrator = currentData.hasNarratorMusicFile(language)
        viewBinding.viewVolumControl2.visibility = if (hasNarrator) View.VISIBLE else View.GONE
        viewBinding.viewNarratorSwitchPanel.visibility =
            if (hasNarrator) View.VISIBLE else View.GONE
    }

    private fun getLocalizedSongName(currentData: AlbumMusic): String {
        return (when (selectedLanguage) {
            "en" -> currentData.songName
            else -> currentData.songNameFrench ?: currentData.songName
        }) ?: ""
    }

    private fun getLocalizedTagsAndArtists(currentData: AlbumMusic): String {
        var artists = currentData.artist?.joinToString(", ") { getLocalizedArtistName(it) } ?: ""
        if (currentData.artist.isNullOrEmpty()) artists = ""
        var tags = currentData.tag?.joinToString("&#8226; ") { getLocalizedTagName(it) } ?: ""
        if (currentData.tag.isNullOrEmpty()) return "$artists"
        return "$tags, $artists"
    }

    private fun getLocalizedArtistName(artistData: Artist): String {
        return when (selectedLanguage) {
            "en" -> artistData.artistName ?: ""
            else -> (artistData.artistNameFrench ?: artistData.artistName) ?: ""
        }
    }

    private fun getLocalizedTagName(tagData: Tag): String {
        return (when (selectedLanguage) {
            "en" -> tagData.tageName
            else -> tagData.tageNameFrench ?: tagData.tageName
        }) ?: ""
    }

    private fun getFavoriteIconResource(currentData: AlbumMusic): Int {
        return if (currentData.isFavorite == 1) R.drawable.ic_heart_selected else R.drawable.ic_heart
    }

    private fun updateVolumeControls() {
        val volume = playerManager?.getMusicPlayerVolume() ?: 100
        viewBinding.viewSeekbarMusicPlayerVolume.setProgress(volume)
        viewBinding.viewSeekbarVoiceMusicPlayerVolume.setProgress(volume)
    }


    private val videoUIHandler = Handler(Looper.getMainLooper())
    fun setupLocalPlayerPage() {
        videoUIHandler.removeCallbacksAndMessages(null)
        if (isLocalPlayerPageExpanded) {
            viewBinding.viewToggleBrightnessPanel.hide()
            viewBinding.viewPagerIndicator.hide()
            viewBinding.viewMusicPages.layoutParams = fullPageLayoutParams
            postControlHideByInactivity(videoUIHandler)
        } else {
            viewBinding.viewToggleBrightnessPanel.show()
            viewBinding.viewPagerIndicator.show()
            viewBinding.viewMusicPages.layoutParams = defaultLayoutParams
            togglePlayerControls(true)
        }
        viewPagerAdapter?.getItemList()?.forEach {
            (it as IPlayerSubPage?)?.pageExpanded(isLocalPlayerPageExpanded)
        }
    }

    fun onLocalPlayerViewPressed() {
        videoUIHandler.removeCallbacksAndMessages(null)
        if (isLocalPlayerPageExpanded) {
            togglePlayerControls(!(viewBinding.viewPlayerControls.isVisible && viewBinding.viewPlayerControls.alpha == 1f))
        } else {
            (viewPagerAdapter?.getItemOrNull(0) as PlayerBannerFragment?)?.togglePlayerButton()
        }
    }

    private fun postControlHideByInactivity(
        videoUIHandler: Handler
    ) {

        videoUIHandler.postDelayed({
            if (isLocalPlayerPageExpanded) {
                if (lastInteractionTime != -1L && (System.currentTimeMillis() - lastInteractionTime) < 1000) {
                    postControlHideByInactivity(videoUIHandler)
                } else togglePlayerControls(false)
            }
        }, 3000)
    }

    private fun togglePlayerControls(visible: Boolean) {

        //Carefully this are command control player for the main page, Video view has only one button for play after this controls will manage with main player and this local video player
        val controls = listOf(
            viewBinding.viewPrevious,
            viewBinding.viewNext,
            viewBinding.ivPlayerState,
            viewBinding.viewSeekbarPlayerDuration,
            viewBinding.viewToggleVolumePanel,
            viewBinding.viewToggleBrightnessPanel
        )
        controls.forEach { it.setEnabledState(visible) }

        if (visible) {
            viewBinding.viewPlayerControls.fadeIn()
        } else {
            //ONLY ALPHA WILL BE CHANGED FOR THIS ONE
            viewBinding.viewPlayerControls.fadeOut(hideOnEnd = false)
        }

        viewBinding.viewPager.post {
            (viewPagerAdapter?.getItemOrNull(0) as PlayerBannerFragment?)?.changeLyricTextPosition(
                isControlVisible = visible
            )
        }
    }

    var defaultLayoutParams: ConstraintLayout.LayoutParams? = null
    var fullPageLayoutParams: ConstraintLayout.LayoutParams? = ConstraintLayout.LayoutParams(
        ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT
    )

    private fun setupViewPager(music: AlbumMusic) {

        val lan = getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE)
        val isEnglish = lan.equals("en", ignoreCase = true)

        viewBinding.viewPlayerControls.post {
            if (defaultLayoutParams == null) {
                defaultLayoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT
                )
                var controlsSize = viewBinding.viewPlayerControls.height
                defaultLayoutParams?.topMargin =
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._90sdp)
                defaultLayoutParams?.marginStart =
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._30sdp)
                defaultLayoutParams?.marginEnd =
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._30sdp)
                defaultLayoutParams?.bottomMargin = controlsSize
            }
            if (!isLocalPlayerPageExpanded) {
                viewBinding.viewMusicPages.layoutParams = defaultLayoutParams
            } else {
                viewBinding.viewToggleBrightnessPanel.hide()
                viewBinding.viewPagerIndicator.hide()
                viewBinding.viewMusicPages.layoutParams = fullPageLayoutParams
            }
            var previousVideoPlayState =
                (viewPagerAdapter?.getItemOrNull(0) as PlayerBannerFragment?)?.isPlayButtonPressed
                    ?: false
            viewPagerFargList.clear()
            viewPagerFargList.add(
                PlayerBannerFragment.newInstance(
                    music,
                    previousVideoPlayState,
                    isLocalPlayerPageExpanded
                )
            )
            if ((music.isPurchased == 1) || (music.isPaid == 0)) {
                if (music.hasLyricFile(lan)) {
                    viewPagerFargList.add(PlayerLyricFragment.newInstance(music))
                }

                // Guided / Unguided visibility
                val guidedValue = if (isEnglish) music.hasGuidedFileEnglish() else music.hasGuidedFileFrench()
                if (guidedValue) {
                    viewPagerFargList.add(PlayerGuideFragment.newInstance(music))
                }
            }
            viewPagerAdapter = FragmentPagerAdapter(
                childFragmentManager, viewPagerFargList
            )
            viewBinding.viewPager.adapter = viewPagerAdapter
            viewBinding.viewPager.offscreenPageLimit = 3
            viewBinding.viewPager.addOnPageChangeListener(createPageChangeListener())
            changePageIndication(0)

            viewBinding.viewPager.post {
                val currentSongPos = playerManager?.fetchCurrentMediaIndex() ?: 0
                musicList.getOrNull(currentSongPos)?.let { currentMusic ->
                    updateUiData(currentMusic)
                }
            }
        }
    }

    private fun createPageChangeListener() = object : ViewPager.OnPageChangeListener {
        override fun onPageScrolled(
            position: Int, positionOffset: Float, positionOffsetPixels: Int
        ) {
        }

        override fun onPageSelected(position: Int) {
            changePageIndication(position)
        }

        override fun onPageScrollStateChanged(state: Int) {}
    }

    private fun changePageIndication(position: Int) {
        var indicators = listOf(viewBinding.ivPage1, viewBinding.ivPage2, viewBinding.ivPage3)
        indicators.forEachIndexed { index, item ->
            item.hide()
        }
        if (viewPagerFargList.isEmpty()) return
        indicators = indicators.subList(0, viewPagerFargList.size)
        indicators.forEach { it.show() }
        indicators.forEach { it.setImageResource(R.drawable.ic_dot_default) }
        indicators.getOrNull(position)?.setImageResource(R.drawable.ic_dot_selected)
    }

    private fun releaseResources() {
        handler?.removeCallbacks(updateSeekBarRunnable)
        handler = null
        viewBinding.hueColorView.setBackgroundColor(Color.TRANSPARENT)
    }

    var durationSeekEnabled = true

    internal fun updatePlayerUI(caller: String) {
        if (!isSafe()) {
            return
        }
        if (!(::viewBinding.isInitialized)) {
            return
        }

//        LogSystem.e(
//            TAG,
//            "updatePlayerUI Invoked by $caller isPreview : ${musicList.get(musicIndex).isPlayPreview}"
//        )

        postMusicPackCache()
        postSeekbarPlayerDuration("updatePlayerUI")

        if (viewBinding.viewPager.currentItem == 1) {
            (viewPagerAdapter?.getItemOrNull(1) as IPlayerSubPage?)?.setPlayerPosition(
                playerManager?.getPlayerPosition() ?: 0
            )
        } else if (viewBinding.viewPager.currentItem == 0) {
            (viewPagerAdapter?.getItemOrNull(0) as IPlayerSubPage?)?.setPlayerPosition(
                playerManager?.getPlayerPosition() ?: 0
            )
        }

        updatePlayerButtonState()
        updateCaptionButtonState()
        checkIfPaidMusicPack()
        updateCastButtonUI(playerManager?.isRemoteClientConnected() == true)
    }

    private fun postMusicPackCache() {
        try {
            musicIndex = playerManager?.fetchCurrentMediaIndex() ?: musicIndex
            musicList.get(musicIndex).lastTimeMusicPosition = playerManager?.getPlayerPosition()
            if (musicList.get(musicIndex).isPlayPreview || musicList.get(musicIndex).isPurchased != 1 || (musicList.get(
                    musicIndex
                ).isPaid ?: 0) == 1
            ) {
                musicList.get(musicIndex).lastTimeMusicPosition = 0
            }
            KeyStorage.getInstance(requireContext()).cacheMusic(musicList.get(musicIndex))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updatePlayerButtonState() {
        if (playerManager?.isPlaying == true) {
            viewBinding.hueColorView.show()
            viewBinding.ivPlayerState.setImageResource(R.drawable.ic_play_cont_pause_huge)
        } else {
            if (durationSeekEnabled) viewBinding.hueColorView.hide()
            viewBinding.ivPlayerState.setImageResource(R.drawable.ic_play_cont_play_huge)
        }

        (viewPagerAdapter?.getItemOrNull(0) as PlayerBannerFragment?)?.updatePlayerButtonState(
            playerManager
        )
    }

    private fun updateCaptionButtonState() {
        if (playerManager?.isRemoteClientConnected() == true) {
            if (playerManager?.hasSubtitle() == true) viewBinding.ivCaptionState.show()
            if (playerManager?.isSubtitleEnabled() == true) {
                viewBinding.ivCaptionState.setColorFilter(
                    resources.getColor(R.color.colorPrimary),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            } else {
                viewBinding.ivCaptionState.setColorFilter(null)
            }
        } else {
            viewBinding.ivCaptionState.hide()
        }
    }

    private fun disableLocalPlayerIfNeed(isConnected: Boolean) {
        if ((isLocalPlayerPageExpanded || LocalVideoPlayerPropertyManager.isPlayButtonPressed) && isConnected) {
            showToast(getString(R.string.info_local_player_unavailable_casting))
            isLocalPlayerPageExpanded = false
            LocalVideoPlayerPropertyManager.isPlayButtonPressed = false
            setupLocalPlayerPage()
        }
        if (isConnected) (viewPagerAdapter?.getItemOrNull(0) as PlayerBannerFragment?)?.hidePlayerUI()
        else {
            //Show Local Video UI
            if (!LocalVideoPlayerPropertyManager.isPlayButtonPressed) (viewPagerAdapter?.getItemOrNull(
                0
            ) as PlayerBannerFragment?)?.showPlayerUI()
        }
    }

    private fun checkIfPaidMusicPack() {
        musicIndex = playerManager?.fetchCurrentMediaIndex() ?: musicIndex
        // Delegate to banner fragment to check purchase status and handle preview limits
        (viewPagerAdapter?.getItemOrNull(0) as PlayerBannerFragment?)?.checkIfPaidMusicPack(
            playerManager
        )
    }

    private fun convertMsToTime(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        val hours = milliseconds / (1000 * 60 * 60)
        return if (hours > 0) String.format(
            Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds
        )
        else String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
    }


    override fun onBackPressed(): Boolean {
        if (viewBinding.hueControlPanel.isVisible) {
            toggleVisibility(viewBinding.hueControlPanel)
            return true
        }
        if (viewBinding.volumePanel.isVisible) {
            toggleVisibility(viewBinding.volumePanel)
            return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        viewBinding.brightnessSeekbarParent.hide()
        viewBinding.hueStaticColorParent.hide()
        if (hueLightManager?.isBridgeFunctional() == true) {
            viewBinding.brightnessSeekbarParent.show()
            viewBinding.hueStaticColorParent.show()
            refreshBridgeStatusAsync()
        }
    }

    private fun removeEvents() {
        viewBinding.sliderBrightness.setOnSeekBarChangeListener(null)
        viewBinding.viewToggleBrightnessPanel.setOnClickListener(null)
        viewBinding.hueConfigurePanel.setOnClickListener(null)
        viewBinding.viewToggleVolumePanel.setOnClickListener(null)
        viewBinding.viewSeekbarMusicPlayerVolume.setOnSeekBarChangeListener(null)
        viewBinding.viewSwitchNarrative.setOnCheckedChangeListener(null)
        viewBinding.viewSeekbarVoiceMusicPlayerVolume.setOnSeekBarChangeListener(null)
        viewBinding.viewCastButton.setOnClickListener(null)
        viewBinding.viewPrevious.setOnClickListener(null)
        viewBinding.viewNext.setOnClickListener(null)
        viewBinding.ivPlayerState.setOnClickListener(null)
        viewBinding.viewSeekbarPlayerDuration.setOnSeekBarChangeListener(null)

        viewBinding.ivCaptionState.setOnClickListener(null)
    }

    var autoPlayStart = Configrations.autoPlayStart

    @Synchronized
    private fun playIfReady(caller: String) {
        LogSystem.e(
            TAG,
            "playIfReady : $caller isPlayerReady: ${playerManager?.isPlayerReady()} Auto Start : $autoPlayStart"
        )
        if (playerManager?.isPlayerReady() == true) {
            viewBinding.bufferLoaderView.show()
            updatePlayerUI("playIfReady")
            viewBinding.bufferLoaderView.hide()
            if (autoPlayStart) {
//            if (false) {
                playerManager?.play()
                (viewPagerAdapter?.getItemOrNull(0) as PlayerBannerFragment?)?.checkAutoStart()
                viewBinding.ivPlayerState.setImageResource(R.drawable.ic_play_cont_pause_huge)
            }
            handler?.post(updateSeekBarRunnable)
        }
    }

    private fun postSeekbarPlayerDuration(caller: String) {
        val duration = playerManager?.getPlayerDuration() ?: 0
        val position = playerManager?.getPlayerPosition() ?: 0
//        LogSystem.e(
//            TAG,
//            "postSeekbarPlayerDuration by $caller Music Duration : $position - $duration"
//        )

        viewBinding.viewPlayerDurationText.text = convertMsToTime(duration)
        viewBinding.viewPlayerTimeText.text = convertMsToTime(position)
        if (!(playerManager?.isPlayerReady() ?: false)) {
            var musicPack = playerManager?.getCurrentMusicItem()
            viewBinding.viewPlayerDurationText.text = musicPack?.getMusicDurationString() ?: "--"
        }

        if (durationSeekEnabled && playerManager?.isPlayerReady() == true) {
            viewBinding.viewSeekbarPlayerDuration.apply {
                max = ((playerManager?.getPlayerDuration() ?: 0) / 500).toInt()
                progress = (position / 500).toInt()
            }
        }
    }


    private fun handlePlaybackState(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            if (playerManager?.isPlayerReady() == true) {
                viewBinding.bufferLoaderView.hide()
                if ((viewPagerAdapter?.getItemOrNull(0) as PlayerBannerFragment?)?.isPlayButtonPressed == true) {
                    playerManager?.playLocalPlayerSync()
                }
            }
            viewBinding.ivPlayerState.isEnabled = true
            viewBinding.ivPlayerState.isClickable = true
        } else if (playbackState == Player.STATE_BUFFERING) {
            playerManager?.pauseLocalVideoPlayer()
            viewBinding.bufferLoaderView.show()
            viewBinding.ivPlayerState.isEnabled = false
            viewBinding.ivPlayerState.isClickable = false
        }

        if (playerManager?.isPlaying == true) viewBinding.ivPlayerState.setImageResource(R.drawable.ic_play_cont_pause_huge)
        else viewBinding.ivPlayerState.setImageResource(R.drawable.ic_play_cont_play_huge)

        (viewPagerAdapter?.getItemOrNull(0) as PlayerBannerFragment?)?.updatePlayerButtonState(
            playerManager
        )

        if (playbackState == Player.STATE_ENDED) {
            playerManager?.pauseForce()
            playerManager?.seekTo(0)
            viewBinding.ivPlayerState.setImageResource(R.drawable.ic_play_cont_play_huge)
        }
    }

    private fun initializeMusicPlayer() {
        removeEvents()
        releaseResources()
        handler = Handler(Looper.getMainLooper())

        playerManager = PlayerManager.getInstance(requireContext().applicationContext)
        playerManager?.bindPlayerListener(object : PlayerListener {
            override fun getHueColorView(): View? {
                if (isSafe()) return viewBinding.hueColorView
                else return null
            }

            override fun songNotPurchased(
                currentMusicItem: AlbumMusic?, allDirectMusicList: ArrayList<AlbumMusic>
            ) {

            }

            override fun onVideoPlaybackStateChanged(state: Int) {
                if (!isSafe()) return
                (viewPagerAdapter?.getItemOrNull(0) as PlayerBannerFragment?)?.handleVideoPlaybackState(
                    state, playerManager
                )
            }

            override fun onVideoPlayerError(message: String?) {
                super.onVideoPlayerError(message)
                if (!isSafe()) return
                (viewPagerAdapter?.getItemOrNull(0) as PlayerBannerFragment?)?.handlePlayerError(
                    playerManager
                )
                showToast(getString(R.string.error_video_player, message ?: "Unknown"))
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (!isSafe()) return
                handlePlaybackState(state)
            }

            override fun onPlayerStateChanged(musicReady: Boolean, voiceReady: Boolean) {
                LogSystem.e(TAG, "PlayerStateChanged : $musicReady $voiceReady")
                if (!isSafe()) return
                playIfReady("PlayerStateChanged")
            }

            override fun onCastSessionDisconnected() {
                if (!isSafe()) return
                updateCastButtonUI(false)
            }

            override fun onCastSessionConnected() {
                LogSystem.e(TAG, "onCastSessionConnected Invoked")
                if (!isSafe()) return
                viewBinding.bufferLoaderView.show()
                updateCastButtonUI(true)
            }

            override fun onRemoteMediaError(mediaError: MediaError) {
                if (!isSafe()) return
                handler?.post {
                    showToast(
                        getString(
                            R.string.error_remote_media,
                            mediaError.reason ?: "",
                            mediaError.type ?: "",
                            mediaError.detailedErrorCode
                        )
                    )
                    disableLocalPlayerIfNeed(false)
                }
            }

            override fun onPlayerError(message: String?) {
                if (!isSafe()) return
                showDialog("Error", "Error Message : ${message}")
            }

            override fun hideLoader() {
                if (!isSafe()) return
                this@PlayerFragment.hideLoader()
            }

            override fun onSessionEnded() {
                if (!isSafe()) return
                updateCastButtonUI(false)
            }
        })
        try {
            if (playerManager?.getCurrentMusicItem() != null) {
                musicList = ArrayList(playerManager?.playerMusicList ?: ArrayList())
                musicList.forEachIndexed { index, _music ->
                    if (_music?.songId == playerManager?.getCurrentMusicItem()?.songId) {
                        musicIndex = index
                    }
                }
                viewBinding.bufferLoaderView.hide()
                updatePlayerUI("initializeMusicPlayer")
                postSeekBarRunnable()
            } else {

                playerManager?.initializePlayers(
                    musicList.get(musicIndex), musicIndex, musicList, null, _isNarrator = true
                )
            }


            var userJsonString = KeyStorage.getInstance(ContextWrapper.getContext())
                .getString(Constants.USER_DATA, "")
            musicList?.forEach { pack ->
                if (pack.isPaid == 1 && pack.isPurchased == 0) KeyStorage.getInstance()
                    .shouldShowPaidStatus(pack, userJsonString)
            }

            musicList.get(musicIndex)?.let { pack ->
                if (pack.isPaid == 1 && pack.isPurchased == 0) {
                    LocalVideoPlayerPropertyManager.isLocalPlayerPageExpanded = false
                }
            }

            updateMusicPlayerUI(musicList.get(musicIndex))
            updateUiData(musicList.get(musicIndex))
            setupViewPager(musicList.get(musicIndex))
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(getString(R.string.error_exception, e.message ?: "Unknown"))
        }

        setupEvents()
    }

    var previousCastState = false
    private fun updateCastButtonUI(castState: Boolean) {
        disableLocalPlayerIfNeed(castState)
        if (previousCastState == castState) return
        if (castState) {
            try {
                (viewBinding.viewCastButton.getChildAt(0) as ImageView).setColorFilter(
                    resources.getColor(R.color.colorPrimary),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
                previousCastState = castState
            } catch (e: Exception) {

            }
        } else {
            try {
                (viewBinding.viewCastButton.getChildAt(0) as ImageView).setColorFilter(
                    null
                )
                previousCastState = castState
            } catch (e: Exception) {
            }
        }

    }

    private fun updateMusicPlayerUI(musicData: AlbumMusic) {
        KeyStorage.getInstance(requireContext()).cacheMusic(musicData)
        viewBinding.apply {
            viewPlayerTimeText.text = "--"
            viewPlayerDurationText.text = "--"
            viewSeekbarPlayerDuration.progress = 0
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseResources()
        LogSystem.e(TAG, "onDestroy Player Page")
    }

    fun onBottomSheetDismissed(musicPack: AlbumMusic?) {
        musicPack?.let { pack ->
            musicList.forEach {
                if (it.songId == pack.songId) {
                    it.isPurchased = pack.isPurchased
                }
            }
            // Update PlayerManager's music list as well
            PlayerManager.getInstance()?.playerMusicList?.forEachIndexed { index, music ->
                if (music.songId == pack.songId) {
                    music.isPurchased = pack.isPurchased ?: 0
                    return@forEachIndexed
                }
            }
        }
        updateCurrentMusicUI()
        (viewPagerAdapter?.getItemOrNull(0) as PlayerBannerFragment?)?.onBottomSheetDismissed(
            musicPack
        )
    }


}
