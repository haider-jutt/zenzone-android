package com.zenimmersive.android.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.base.BaseActivity
import com.zenimmersive.android.databinding.ActivityDashboardBinding
import com.zenimmersive.android.helper.Constants.NOTIFICATION_ALBUM_ID
import com.zenimmersive.android.helper.Constants.NOTIFICATION_SONG_ID
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.helper.hide
import com.zenimmersive.android.helper.show
import com.zenimmersive.android.repository.DashboardRepository
import com.zenimmersive.android.ui.home.HomeFragment
import com.zenimmersive.android.ui.home.HomePlaylistFragment
import com.zenimmersive.android.ui.payment.PurchaseHelper
import com.zenimmersive.android.ui.payment.SubscriptionBSFragment
import com.zenimmersive.android.ui.player.PlayerFragment
import com.zenimmersive.android.ui.player.PlayerManager
import com.zenimmersive.android.ui.setting.SettingFragment
import com.zenimmersive.android.viewmodel.DashboardViewModel
import java.util.Arrays

@SuppressLint("UnsafeOptInUsageError")
class DashboardActivity :
    BaseActivity<DashboardViewModel, ActivityDashboardBinding, DashboardRepository>() {

    private lateinit var navPositions: FloatArray
    private var notificationSongId: String? = null
    private var notificationAlbumId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.hasExtra(NOTIFICATION_SONG_ID)) {
            notificationSongId = intent.getStringExtra(NOTIFICATION_SONG_ID)
            playerScreenRedirection()
        }
        if (intent.hasExtra(NOTIFICATION_ALBUM_ID)) {
            notificationAlbumId = intent.getStringExtra(NOTIFICATION_ALBUM_ID)
            playlistScreenRedirection()
        }

    }

    override fun getViewModel(): Class<DashboardViewModel> = DashboardViewModel::class.java

    override fun getActivityBinding(inflater: LayoutInflater): ActivityDashboardBinding =
        ActivityDashboardBinding.inflate(inflater)

    override fun getRepository(): DashboardRepository = DashboardRepository(this)

    override fun bindViewModel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this@DashboardActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@DashboardActivity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2
                )
            }
        }

        getFcmToken()
        setupViews()
        if (!notificationSongId.isNullOrBlank()) {
            playerScreenRedirection()
        }
        if (!notificationAlbumId.isNullOrBlank()) {
            playlistScreenRedirection()
        }
    }

    override fun removeViewModelCallbacks() {

    }

    fun getFcmToken() {
        /**
         *  get fcm token
         */
        FirebaseMessaging.getInstance().token.addOnCompleteListener(object :
            OnCompleteListener<String> {
            override fun onComplete(task: Task<String>) {

                if (!task.isSuccessful) {
                    Log.e("Logger", "token failed to received")
                    return
                }
                val token = task.result
                Log.d("Logger", "Firebase Notification every time token :- $token")
                viewModel.updateDeviceToken(token)
            }

        })
    }

    private fun playlistScreenRedirection() {
        val bundle = Bundle()
        bundle.putInt(NOTIFICATION_ALBUM_ID, notificationAlbumId!!.toInt())
//        bundle.putString(ALBUM_IMAGE, albumImage)
//        bundle.putString(ALBUM_TITLE, albumTitle)
//        bundle.putInt(ALBUM_SESSION, albumSession)
//        bundle.putString(ALBUM_DETAILS, details)

        val playlistFragment = HomePlaylistFragment()
        playlistFragment.arguments = bundle
        supportFragmentManager?.beginTransaction()
            ?.replace(R.id.viewDashboardFragmentContainer, playlistFragment,"HomePlaylistFragment")
            ?.addToBackStack("HomePlaylistFragment")
            ?.commit()
    }

    private fun playerScreenRedirection() {
        val bundle = Bundle()
        bundle.putInt(NOTIFICATION_SONG_ID, notificationSongId!!.toInt())
//        bundle.putSerializable(SONG_DATA, songData)
//        bundle.putSerializable(All_SONGS, songsList)
        val playerFragment = PlayerFragment()
        currentPlayerFragment = playerFragment
        playerFragment.arguments = bundle
        supportFragmentManager.beginTransaction()
            .replace(R.id.viewDashboardFragmentContainer, playerFragment)
            .addToBackStack(null)  // ✅ VERY IMPORTANT
            .commit()
    }

    private fun setupViews() {

        viewBinding.ivBack.setOnClickListener { onBackPressed() }

        // Initialize navPositions array to store translationX values
        navPositions = FloatArray(5)

        // Set OnClickListener for each nav item
        viewBinding.navHome.setOnClickListener(navItemClickListener)
        viewBinding.navHeart.setOnClickListener(navItemClickListener)
        viewBinding.navPlayer.setOnClickListener(navItemClickListener)
        viewBinding.navDownload.setOnClickListener(navItemClickListener)
        viewBinding.navUser.setOnClickListener(navItemClickListener)

        setupIndicator(KeyStorage.getInstance(this).getCachedMusic() != null)

        // Register backstack listener immediately, not in post
        supportFragmentManager.addOnBackStackChangedListener {
            viewBinding.root.post {
                // Ensure container is visible when backstack changes
                viewBinding.viewDashboardFragmentContainer.visibility = View.VISIBLE
                
                var fragment = supportFragmentManager.findFragmentById(R.id.viewDashboardFragmentContainer)
                LogSystem.e("NavSide", "BackStack changed, current fragment: ${fragment?.javaClass?.simpleName}")
                when (fragment) {
                    is HomeFragment -> {
                        pageIndex = 0
                        currentHomeFragment = fragment
                        viewBinding.tvPageTitle.show()
                        viewBinding.tvPageTitle.setText("")
                        if (navPositions.isNotEmpty() && navPositions[0] != 0f) {
                            animateIndicationLine(navPositions[0])
                        }
                    }

                    is FavoritesFragment -> {
                        pageIndex = 1
                        viewBinding.tvPageTitle.show()
                        viewBinding.tvPageTitle.setText("")
                        if (navPositions.isNotEmpty() && navPositions[1] != 0f) {
                            animateIndicationLine(navPositions[1])
                        }
                    }

                    is PlayerFragment -> {
                        pageIndex = 2
                        currentPlayerFragment = fragment
                        viewBinding.tvPageTitle.show()
                        viewBinding.tvPageTitle.setText(getString(R.string.playing_now))
                        if (navPositions.isNotEmpty() && navPositions[2] != 0f) {
                            animateIndicationLine(navPositions[2])
                        }
                    }


                    is VipMusicFragment -> {
                        pageIndex = 3
                        viewBinding.tvPageTitle.show()
                        viewBinding.tvPageTitle.setText("")
                        if (navPositions.isNotEmpty() && navPositions[3] != 0f) {
                            animateIndicationLine(navPositions[3])
                        }
                    }

                    is SettingFragment -> {
                        pageIndex = 4
                        viewBinding.tvPageTitle.show()
                        viewBinding.tvPageTitle.setText("")
                        if (navPositions.isNotEmpty() && navPositions[4] != 0f) {
                            animateIndicationLine(navPositions[4])
                        }
                    }
                }
            }
        }

        showPage(pageIndex)
    }

    private fun setupIndicator(displayPlayIcon: Boolean = true) {
        LogSystem.e("NavSide setupIndicator Invoked Page Index : $pageIndex")
        if (displayPlayIcon) viewBinding.navPlayer.show()
        else viewBinding.navPlayer.hide()
        // Calculate the positions for the indication line
        viewBinding.viewBottomControls.post {
            calculateNavPositions()
        }
    }

    // Keep references only for necessary callbacks, but create new instances in showPage()
    private var currentPlayerFragment: PlayerFragment? = null
    private var currentHomeFragment: HomeFragment? = null

    private fun showPage(index: Int) {

        viewBinding.viewDashboardFragmentContainer.visibility = View.VISIBLE

        when (index) {

            // HOME (ROOT PAGE)
            0 -> {
                val fragmentHomePage = HomeFragment()
                currentHomeFragment = fragmentHomePage
                viewBinding.tvPageTitle.show()
                viewBinding.tvPageTitle.text = ""

                // Clear full backstack
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

                // Add Home as root (no backstack)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.viewDashboardFragmentContainer, fragmentHomePage, "HomeFragment")
                    .commit()
            }

            // FAVORITES
            1 -> {
                val fragmentFavoritePage = FavoritesFragment()
                viewBinding.tvPageTitle.show()
                viewBinding.tvPageTitle.text = ""
                supportFragmentManager.beginTransaction()
                    .replace(R.id.viewDashboardFragmentContainer, fragmentFavoritePage, "FavoritesFragment")
                    .addToBackStack("FavoritesFragment")
                    .commit()
            }

            // PLAYER (NO BACKSTACK)
            2 -> {
                val playerFragment = PlayerFragment()
                currentPlayerFragment = playerFragment
                viewBinding.tvPageTitle.show()
                viewBinding.tvPageTitle.text = getString(R.string.playing_now)

                supportFragmentManager.beginTransaction()
                    .replace(R.id.viewDashboardFragmentContainer, playerFragment, "PlayerFragment")
                    .addToBackStack(null)  // ✅ VERY IMPORTANT
                    .commit()
            }

            // VIP
            3 -> {
                val fragmentVipMusicPage = VipMusicFragment()
                viewBinding.tvPageTitle.show()
                viewBinding.tvPageTitle.text = ""
                supportFragmentManager.beginTransaction()
                    .replace(R.id.viewDashboardFragmentContainer, fragmentVipMusicPage, "VipMusicFragment")
                    .addToBackStack("VipMusicFragment")
                    .commit()
            }

            // SETTINGS
            4 -> {
                val fragmentSettingPage = SettingFragment()
                viewBinding.tvPageTitle.show()
                viewBinding.tvPageTitle.text = ""
                supportFragmentManager.beginTransaction()
                    .replace(R.id.viewDashboardFragmentContainer, fragmentSettingPage, "SettingFragment")
                    .addToBackStack("SettingFragment")
                    .commit()
            }

            // DEFAULT
            else -> {
                viewBinding.tvPageTitle.hide()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.viewDashboardFragmentContainer, BlankFragment(), "BlankFragment#$index")
                    .addToBackStack("BlankFragment#$index")
                    .commit()
            }
        }
    }


    private fun calculateNavPositions() {
        LogSystem.e("NavSide calculateNavPositions Invoked")
        //LogSystem.e("Nav Heart X : ${viewBinding.navHeart.x} Nav Heart Width : ${viewBinding.navHeart.width}")
        //LogSystem.e("Nav Player X : ${viewBinding.navPlayer.x} Nav Player Width : ${viewBinding.navPlayer.width}")
        //LogSystem.e("Nav Download X : ${viewBinding.navDownload.x} Nav Download Width : ${viewBinding.navDownload.width}")
        //LogSystem.e("Nav User X : ${viewBinding.navUser.x} Nav User Width : ${viewBinding.navUser.width}")
        viewBinding.navHome.parent.requestLayout()
        navPositions[0] =
            viewBinding.navHome.x + viewBinding.navHome.width / 2f - viewBinding.viewIndicationLine.width / 2f
        navPositions[1] =
            viewBinding.navHeart.x + viewBinding.navHeart.width / 2f - viewBinding.viewIndicationLine.width / 2f
        navPositions[2] =
            viewBinding.navPlayer.x + viewBinding.navPlayer.width / 2f - viewBinding.viewIndicationLine.width / 2f
        navPositions[3] =
            viewBinding.navDownload.x + viewBinding.navDownload.width / 2f - viewBinding.viewIndicationLine.width / 2f
        navPositions[4] =
            viewBinding.navUser.x + viewBinding.navUser.width / 2f - viewBinding.viewIndicationLine.width / 2f

        viewBinding.viewIndicationLine.post {
            viewBinding.viewIndicationLine.clearAnimation()
            viewBinding.viewIndicationLine.translationX = navPositions[pageIndex]
        }

        LogSystem.e("NavSide Positions : ${Arrays.toString(navPositions)}")
    }

    var pageIndex = 0
    private val navItemClickListener = View.OnClickListener { view ->
        val targetPosition = when (view.id) {
            R.id.navHome -> navPositions[0]
            R.id.navHeart -> navPositions[1]
            R.id.navPlayer -> navPositions[2]
            R.id.navDownload -> navPositions[3]
            R.id.navUser -> navPositions[4]
            else -> 0f
        }
        pageIndex = when (view.id) {
            R.id.navHome -> 0
            R.id.navHeart -> 1
            R.id.navPlayer -> 2
            R.id.navDownload -> 3
            R.id.navUser -> 4
            else -> 0
        }
        var currentFragment = supportFragmentManager.findFragmentById(R.id.viewDashboardFragmentContainer)
        if (currentFragment != null) {
            if (currentFragment is HomeFragment && pageIndex == 0) {
                return@OnClickListener
            }
            if (currentFragment is FavoritesFragment && pageIndex == 1) {
                return@OnClickListener
            }
            if (currentFragment is PlayerFragment && pageIndex == 2) {
                return@OnClickListener
            }
            if (currentFragment is VipMusicFragment && pageIndex == 3) {
                return@OnClickListener
            }
            if (currentFragment is SettingFragment && pageIndex == 4) {
                return@OnClickListener
            }
        }
        showPage(pageIndex)
        animateIndicationLine(targetPosition)
    }

    private fun animateIndicationLine(targetPosition: Float) {
        LogSystem.e("NavSide animateIndicationLine Invoked Target Position : $targetPosition")
        ObjectAnimator.ofFloat(viewBinding.viewIndicationLine, "translationX", targetPosition)
            .apply {
                duration = 300 // Animation duration in milliseconds
                start()
            }
    }

    override fun onBackPressed() {
        val fragment: Fragment? =
            supportFragmentManager.findFragmentById(R.id.viewDashboardFragmentContainer)
        if (fragment is BackPressListener) {
            if ((fragment as BackPressListener?)?.onBackPressed() == false) {
                if (supportFragmentManager.backStackEntryCount >= 1) {
                    super.onBackPressed()
                } else {
                    finish()
                }
            }
        } else {
            if (supportFragmentManager.backStackEntryCount >= 1) {
                super.onBackPressed()
            } else {
                finish()
            }
        }
    }

    override fun onDestroy() {
        PlayerManager.getInstance()?.destroy()
        super.onDestroy()
    }

    fun displayPlayerPage(bundle: Bundle, isVipScreen: Boolean = false) {
        pageIndex = 2
        if (viewBinding.navPlayer.visibility == View.GONE) setupIndicator(true)
        else animateIndicationLine(navPositions[2])
//        var fragPlayerPage = PlayerFragment()
//        PlayerManager.getInstance()?.destroy()
        val playerFragment = PlayerFragment()
        currentPlayerFragment = playerFragment
//        fragmetnPlayerPage?.playerManager?.releaseResources()
        playerFragment.arguments = bundle
        supportFragmentManager.beginTransaction()
            .replace(R.id.viewDashboardFragmentContainer, playerFragment,"PlayerFragment")
            .addToBackStack(null)  // ✅ VERY IMPORTANT
            .commit()

    }

    var isBottomSheetLaunched = false
    fun displayPurchasePage(
        bundle: Bundle,
        listener: SubscriptionBSFragment.BottomSheetDismissListener? = null
    ) {
        if (isBottomSheetLaunched) return
        isBottomSheetLaunched = true
        val bottomSheet = SubscriptionBSFragment()
        bottomSheet.bindBottomSheetDismissListener(object :
            SubscriptionBSFragment.BottomSheetDismissListener {

            override fun onBottomSheetDismissed(musicPack: AlbumMusic?) {
                Log.d("TAG", "onBottomSheetDismissed: called")
                listener?.onBottomSheetDismissed(musicPack)
                isBottomSheetLaunched = false
                val shouldShowPurchase = musicPack?.let {
                    KeyStorage.getInstance(this@DashboardActivity)
                        .shouldShowPaidStatus(this@DashboardActivity, it)
                } ?: false
                if (!shouldShowPurchase) {
                    currentHomeFragment?.refreshMusicPacks()
                    currentPlayerFragment?.onBottomSheetDismissed(
                        musicPack
                    )
                }
            }
        })
        bottomSheet.arguments = bundle
        bottomSheet.show(supportFragmentManager!!, "SubscriptionBSFragment")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        viewBinding.viewBottomControls.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    viewBinding.viewBottomControls.viewTreeObserver.removeOnGlobalLayoutListener(
                        this
                    )
                    setupIndicator(
                        KeyStorage.getInstance(applicationContext).getCachedMusic() != null
                    )
                }
            })
    }
}
