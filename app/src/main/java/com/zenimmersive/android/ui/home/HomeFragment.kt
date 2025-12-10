package com.zenimmersive.android.ui.home

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zenimmersive.android.R
import com.zenimmersive.android.adapter.AlbumItemPressListener
import com.zenimmersive.android.adapter.AlbumListAdapterSmall
import com.zenimmersive.android.adapter.HomeAlbumAdapter
import com.zenimmersive.android.adapter.HomeMeditationsAdapter
import com.zenimmersive.android.adapter.HomePopularAdapter
import com.zenimmersive.android.adapter.HomeRecentlyPlayedAdapter
import com.zenimmersive.android.adapter.MusicPlackListAdapter
import com.zenimmersive.android.adapter.MusicPackItemListener
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.apiresponsemodel.HomeDataResModel
import com.zenimmersive.android.apiresponsemodel.HomeDataResModel.Result.Album
import com.zenimmersive.android.apiresponsemodel.Tag
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.FragmentHomeBinding
import com.zenimmersive.android.helper.CommonUtils
import com.zenimmersive.android.helper.Constants.ALBUM_DETAILS
import com.zenimmersive.android.helper.Constants.ALBUM_ID
import com.zenimmersive.android.helper.Constants.ALBUM_IMAGE
import com.zenimmersive.android.helper.Constants.ALBUM_SESSION
import com.zenimmersive.android.helper.Constants.ALBUM_TITLE
import com.zenimmersive.android.helper.Constants.All_SONGS
import com.zenimmersive.android.helper.Constants.SONG_DATA
import com.zenimmersive.android.helper.Constants.SONG_ID
import com.zenimmersive.android.helper.Constants.TAG_ID
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_ID
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_TOKEN
import com.zenimmersive.android.helper.LayoutMarginDecoration
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.repository.HomeRepository
import com.zenimmersive.android.ui.BackPressListener
import com.zenimmersive.android.ui.DashboardActivity
import com.zenimmersive.android.ui.ViewAllAlbumsFragment
import com.zenimmersive.android.viewmodel.HomeViewModel
import kotlin.math.roundToInt

class HomeFragment : BaseFragment<HomeViewModel, FragmentHomeBinding, HomeRepository>(),
    BackPressListener, AlbumItemPressListener, HomeMeditationsAdapter.HomeMeditationsAdapter,
    HomeRecentlyPlayedAdapter.RecentlyPlayedAdapterLister, MusicPackItemListener {
    val TAG = HomeFragment::class.java.simpleName
    var userID: Int = 0
    lateinit var userToken: String
    private var albumAdapter: HomeAlbumAdapter? = null
    private var popularAdapter: HomePopularAdapter? = null
    private var meditationAdapter: HomeMeditationsAdapter? = null
    private var recentlyPlayedAdapter: HomeRecentlyPlayedAdapter? = null
    private var recentlyPlayedList: ArrayList<AlbumMusic?> = arrayListOf()
    lateinit var lan: String

    // This property is only valid between onCreateView and
    // onDestroyView.
//    private val binding get() = _binding!!
    override fun getViewModel(): Class<HomeViewModel> = HomeViewModel::class.java


    override fun getActivityBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun getRepository(): HomeRepository {
        return HomeRepository(requireContext())
    }

    var isDataLoaded = false

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentViewBinding(FragmentHomeBinding.bind(view))
        viewLoader = viewBinding.invLoader.viewLoader

        if (activity is DashboardActivity) {
            (activity as DashboardActivity).viewBinding.tvPageTitle.text = ""
        }
    }

    val defaultHomeResultModel = HomeDataResModel(null, null, null, null)

    @OptIn(UnstableApi::class)
    private fun initView() {

        viewBinding.ivSearch.setOnClickListener {
            if (activity is DashboardActivity) {
                val searchFragment = SearchFragment()
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.viewDashboardFragmentContainer, searchFragment)
                    ?.addToBackStack("SearchFragment")?.commit()
            }
        }

        viewBinding.viewSwipeRefreshLayout.setOnRefreshListener {
            val resModel = HomeDataResModel(null, null, null, null)
            viewModel.getHomeData(userID, userToken, resModel, false)
        }
    }

    var isServerFetchInvoked = false
    var uiHandler: Handler = Handler()
    fun initObserver() {
        viewModel.homeResData.observe(viewLifecycleOwner) { resData ->
            if (!isSafe()) return@observe
            uiHandler.post { handleHomeResult(resData) }
        }
    }

    private fun handleHomeResult(resData: ViewState<HomeDataResModel>) {
        var popularList: ArrayList<AlbumMusic?> = arrayListOf()
        var albumList: ArrayList<HomeDataResModel.Result.Album?> = arrayListOf()
        var tagsList: ArrayList<HomeDataResModel.Result.Album?> = arrayListOf()
        var meditationList: ArrayList<Tag?> = arrayListOf()

        when (resData) {
            is ViewState.DefaultState -> {}
            is ViewState.Loading -> {
                if (!viewBinding.viewSwipeRefreshLayout.isRefreshing) showLoader("Loading...")
            }

            is ViewState.Data -> {
                Log.e("PageLifeCycle", "Home Page Home API Result Invoked (isPageSafe) ${isSafe()}")
                try {
                    if (!isServerFetchInvoked) {
                        if (CommonUtils.isNetworkAvailable(requireContext()) && KeyStorage.getInstance(
                                requireContext()
                            ).isHomePageServerCallNeed()
                        ) {
                            isServerFetchInvoked = true
                            val resModel = HomeDataResModel(null, null, null, null)
                            viewModel.getHomeData(userID, userToken, resModel, false)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Ensure to clear the existing views in the LinearLayout if needed
                viewBinding.llDynamicView.removeAllViews()
                resData.data.orderData?.forEach { orderViewName ->
                    if (isSafe()) {
                        val inflater =
                            LayoutInflater.from(requireContext()) // Inflate the child layout
                        when (orderViewName) {
                            "popular" -> {
                                popularList.clear()
                                if (!resData.data.result?.popular.isNullOrEmpty()) {
                                    popularList.addAll(resData.data.result?.popular!!)
                                }

                                if (popularList.isNotEmpty()) {

                                    var childView = inflater.inflate(
                                        R.layout.home_popular_view,
                                        viewBinding.llDynamicView,
                                        false
                                    )

                                    val viewDetails =
                                        childView.findViewById<TextView>(R.id.tvPopularViewAll)
                                    viewDetails.setOnClickListener {

                                    }
                                    val rcvPopular =
                                        childView.findViewById<RecyclerView>(R.id.rcvPopular)
                                    rcvPopular.layoutManager = LinearLayoutManager(
                                        requireContext(),
                                        LinearLayoutManager.HORIZONTAL,
                                        false
                                    )
                                    popularAdapter = HomePopularAdapter(
                                        requireContext(), popularList, this
                                    )
                                    rcvPopular.adapter = popularAdapter

                                    // Add the dynamically created view to the LinearLayout
                                    childView?.let { childView ->
                                        viewBinding.llDynamicView.addView(
                                            childView
                                        )
                                    }
                                }
                            }

                            "playlist" -> {
                                albumList.clear()
                                if (!resData.data.result?.albums.isNullOrEmpty()) {
                                    albumList.addAll(resData.data.result?.albums!!)
                                }
                                if (albumList.isNotEmpty()) {
                                    var childView = inflater.inflate(
                                        R.layout.home_album_view,
                                        viewBinding.llDynamicView,
                                        false
                                    )
                                    val tvAlbumsViewAll =
                                        childView.findViewById<TextView>(R.id.tvAlbumsViewAll)
                                    val rcvAlbums =
                                        childView.findViewById<RecyclerView>(R.id.rcvAlbums)
                                    rcvAlbums.layoutManager = LinearLayoutManager(
                                        requireContext(),
                                        LinearLayoutManager.HORIZONTAL,
                                        false
                                    )
                                    albumAdapter = HomeAlbumAdapter(
                                        requireContext(), albumList, this@HomeFragment
                                    )
                                    rcvAlbums.addItemDecoration(
                                        LayoutMarginDecoration(
                                            resources.getDimension(
                                                com.intuit.sdp.R.dimen._15sdp
                                            ).roundToInt(), 0, 0, 0
                                        )
                                    )
                                    rcvAlbums.adapter = albumAdapter

                                    // Add the dynamically created view to the LinearLayout
                                    childView?.let { childView ->
                                        viewBinding.llDynamicView.addView(
                                            childView
                                        )
                                    }

                                    tvAlbumsViewAll.setOnClickListener {
                                        if (activity is DashboardActivity) {
                                            val bundle = Bundle()
                                            val viewAllAlbumsFragment =
                                                ViewAllAlbumsFragment()
                                            viewAllAlbumsFragment.arguments = bundle
                                            activity?.supportFragmentManager?.beginTransaction()
                                                ?.replace(
                                                    R.id.viewDashboardFragmentContainer,
                                                    viewAllAlbumsFragment
                                                )?.addToBackStack("ViewAllAlbumsFragment")
                                                ?.commit()
                                        }
                                    }
                                }
                            }

                            "tags" -> {
                                var prefLang = if (lan.isNullOrBlank()) "en" else lan

                                tagsList.clear()
                                if (!resData.data.result?.tags.isNullOrEmpty()) {
                                    tagsList.addAll(resData.data.result?.tags!!)
                                }

                                if (tagsList.isNotEmpty()) {
                                    tagsList.forEach { tag ->
                                        var childView = inflater.inflate(
                                            R.layout.home_playlist_view,
                                            viewBinding.llDynamicView,
                                            false
                                        )
                                        var tvAlbumName =
                                            childView.findViewById<TextView>(R.id.tvRainStormSounds)
                                        tvAlbumName.setText("${tag?.albumName}")
                                        val tvAlbumTitle =
                                            childView.findViewById<TextView>(R.id.tvRainStormSessions)

                                        tvAlbumTitle.text = "${tag?.totalSong} ${
                                            requireContext().getString(
                                                R.string.sessions
                                            )
                                        }"
                                        val rcvRainStorm =
                                            childView.findViewById<RecyclerView>(R.id.rcvRainStorm)
                                        rcvRainStorm.layoutManager = LinearLayoutManager(
                                            requireContext(),
                                            LinearLayoutManager.HORIZONTAL,
                                            false
                                        )
                                        tvAlbumTitle.setOnClickListener {
                                            val albumTitleLanData: String
                                            val albumDescLanData: String
                                            if (prefLang == "en") {
                                                albumTitleLanData = tag?.albumName!!
                                                albumDescLanData = tag?.description!!
                                            } else {
                                                albumTitleLanData = tag?.albumNameFrench!!
                                                albumDescLanData = tag?.descriptionFrench!!
                                            }
                                            val bundle = Bundle()
                                            bundle.putInt(ALBUM_ID, tag?.albumId!!)
                                            bundle.putString(
                                                ALBUM_IMAGE, tag.bigBackgroundImgSubAlbum
                                            )
                                            bundle.putString(ALBUM_TITLE, albumTitleLanData)
                                            bundle.putInt(ALBUM_SESSION, tag.totalSong!!)
                                            bundle.putString(
                                                ALBUM_DETAILS, albumDescLanData
                                            )

                                            val playlistFragment = HomePlaylistFragment()
                                            playlistFragment.arguments = bundle
                                            activity?.supportFragmentManager?.beginTransaction()
                                                ?.replace(
                                                    R.id.viewDashboardFragmentContainer,
                                                    playlistFragment
                                                )?.addToBackStack("HomePlaylistFragment")
                                                ?.commit()
                                        }
                                        if (tag?.subAlbums?.isEmpty() == true) {
                                            var temp = arrayListOf<AlbumMusic?>()
                                            temp.addAll(tag?.songAlbums ?: emptyList())
                                            var musicPackListAdapter =
                                                MusicPlackListAdapter(
                                                    requireContext(), temp, this
                                                )
                                            rcvRainStorm.addItemDecoration(
                                                LayoutMarginDecoration(
                                                    resources.getDimension(com.intuit.sdp.R.dimen._15sdp)
                                                        .roundToInt(), 0, 0, 0
                                                )
                                            )
                                            rcvRainStorm.adapter = musicPackListAdapter
                                        } else {
                                            tvAlbumTitle.text =
                                                "${requireContext().getString(R.string.view_all)}"
                                            var temp = arrayListOf<Album?>()
                                            temp.addAll(tag?.subAlbums!!)
                                            var albumAdapter = AlbumListAdapterSmall(
                                                requireContext(), temp, this@HomeFragment
                                            )
                                            rcvRainStorm.addItemDecoration(
                                                LayoutMarginDecoration(
                                                    resources.getDimension(com.intuit.sdp.R.dimen._10sdp)
                                                        .roundToInt(), 0, 0, 0
                                                )
                                            )
                                            rcvRainStorm.adapter = albumAdapter
                                        }

                                        // Add the dynamically created view to the LinearLayout
                                        childView?.let { childView ->
                                            viewBinding.llDynamicView.addView(
                                                childView
                                            )
                                        }
                                    }
                                }
                            }

                            "meditationType" -> {
                                meditationList.clear()
                                if (!resData.data.result?.meditationType.isNullOrEmpty()) {
                                    meditationList.addAll(resData.data.result?.meditationType!!)
                                }

                                if (meditationList.isNotEmpty()) {
                                    var childView = inflater.inflate(
                                        R.layout.home_explore_view,
                                        viewBinding.llDynamicView,
                                        false
                                    )
                                    val rcvMeditation =
                                        childView.findViewById<RecyclerView>(R.id.rcvMeditation)
                                    rcvMeditation.layoutManager = LinearLayoutManager(
                                        requireContext(),
                                        LinearLayoutManager.HORIZONTAL,
                                        false
                                    )
                                    meditationAdapter = HomeMeditationsAdapter(
                                        requireContext(), meditationList, this
                                    )
                                    rcvMeditation.adapter = meditationAdapter
                                    // Add the dynamically created view to the LinearLayout
                                    childView?.let { childView ->
                                        viewBinding.llDynamicView.addView(
                                            childView
                                        )
                                    }
                                }
                            }

                            "recentlyPlay" -> {
                                recentlyPlayedList.clear()
                                if (!resData.data.result?.recentlyPlay.isNullOrEmpty()) {
                                    recentlyPlayedList.addAll(resData.data.result?.recentlyPlay!!)
                                }

                                if (recentlyPlayedList.isNotEmpty()) {
                                    var childView = inflater.inflate(
                                        R.layout.home_recently_played_view,
                                        viewBinding.llDynamicView,
                                        false
                                    )
                                    val rcvRecentlyPlayed =
                                        childView.findViewById<RecyclerView>(R.id.rcvRecentlyPlayed)
                                    rcvRecentlyPlayed.layoutManager = LinearLayoutManager(
                                        requireContext(),
                                        LinearLayoutManager.VERTICAL,
                                        false
                                    )
                                    recentlyPlayedAdapter = HomeRecentlyPlayedAdapter(
                                        requireContext(), recentlyPlayedList, this
                                    )
                                    rcvRecentlyPlayed.adapter = recentlyPlayedAdapter
                                    // Add the dynamically created view to the LinearLayout
                                    childView?.let { childView ->
                                        viewBinding.llDynamicView.addView(
                                            childView
                                        )
                                    }
                                }
                            }

                            else -> {
                            }
                        }
                    }
                }

                if (resData.data.result?.albums.isNullOrEmpty() && resData.data.result?.popular.isNullOrEmpty() && resData.data.result?.tags.isNullOrEmpty() && resData.data.result?.meditationType.isNullOrEmpty() && resData.data.result?.recentlyPlay.isNullOrEmpty()) {
                    val inflater =
                        LayoutInflater.from(requireContext()) // Inflate the child layout
                    var childView = inflater.inflate(
                        R.layout.home_no_data_found, viewBinding.llDynamicView, false
                    )
                    childView?.let { childView ->
                        viewBinding.llDynamicView.addView(
                            childView
                        )
                    }
                }

                if (isSafe()) {
                    viewBinding.viewSwipeRefreshLayout.isRefreshing = false
                    hideLoader()
                }
            }

            is ViewState.Error -> {
                viewBinding.viewSwipeRefreshLayout.isRefreshing = false
                hideLoader()
                showToast(resData.error)
            }
        }

        isDataLoaded = true
    }

    override fun registerObservers() {
        LogSystem.e("PageLifeCycle", "HomePage registerObservers Invoked")
        lan = KeyStorage.getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE)
        userID = KeyStorage.getInstance(requireContext()).getInt(KEY_USER_ID)
        userToken = KeyStorage.getInstance(requireContext()).getString(KEY_USER_TOKEN)
        initView()
        initObserver()
    }

    override fun unregisterObservers() {
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    @OptIn(UnstableApi::class)
    override fun albumClickListener(
        albumId: Int, albumImage: String, albumTitle: String, albumSession: Int, details: String
    ) {
        if (activity is DashboardActivity) {
            val bundle = Bundle()
            bundle.putInt(ALBUM_ID, albumId)
            bundle.putString(ALBUM_IMAGE, albumImage)
            bundle.putString(ALBUM_TITLE, albumTitle)
            bundle.putInt(ALBUM_SESSION, albumSession)
            bundle.putString(ALBUM_DETAILS, details)

            val playlistFragment = HomePlaylistFragment()
            playlistFragment.arguments = bundle
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.viewDashboardFragmentContainer, playlistFragment)
                ?.addToBackStack("HomePlaylistFragment")?.commit()
        }
    }

    @OptIn(UnstableApi::class)
    override fun meditationItemClick(position: Int, data: Tag?) {
        if (activity is DashboardActivity) {
            // TODO check this data is correct or not.
            val bundle = Bundle()
            bundle.putString(ALBUM_TITLE, data?.tageName)
            bundle.putInt(ALBUM_SESSION, data?.songCount?.toInt() ?: 0)
            bundle.putString(ALBUM_DETAILS, "")
            bundle.putInt(TAG_ID, data?.tagId!!)
            val playlistFragment = HomePlaylistFragment()
            playlistFragment.arguments = bundle
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.viewDashboardFragmentContainer, playlistFragment)
                ?.addToBackStack("HomePlaylistFragment")?.commit()
        }
    }

    @OptIn(UnstableApi::class)
    override fun recentlyPlayedSongClick(songData: AlbumMusic) {
        if (activity is DashboardActivity) {
            val bundle = Bundle()
            bundle.putInt(SONG_ID, songData.songId!!)
            bundle.putSerializable(SONG_DATA, songData)
            bundle.putSerializable(All_SONGS, recentlyPlayedList)
//            if (songData.isPurchased != 1)
            // The following line is commented out to always open the player fragment regardless of purchase/subscription status
            // if (songData != null && KeyStorage.getInstance(requireActivity()).shouldShowSubscriptionSheet(requireActivity(), songData)){
            //     (activity as DashboardActivity).displayPurchasePage(bundle)
            // }else{
            (activity as DashboardActivity).displayPlayerPage(bundle)
            // }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onMusicPackPressed(songData: AlbumMusic?, popularList: ArrayList<AlbumMusic?>) {
        if (activity is DashboardActivity) {
            val bundle = Bundle()
            bundle.putInt(SONG_ID, songData?.songId!!)
            bundle.putSerializable(SONG_DATA, songData)
            bundle.putSerializable(All_SONGS, popularList)
//            if (songData.isPurchased != 1){
            // The following line is commented out to always open the player fragment regardless of purchase/subscription status
            // if (songData != null && KeyStorage.getInstance(requireActivity()).shouldShowSubscriptionSheet(requireActivity(), songData)){
            //     (activity as DashboardActivity).displayPurchasePage(bundle)
            // }else{
            (activity as DashboardActivity).displayPlayerPage(bundle)
            // }
        }
    }

    override fun onMusicPackPurchaseButtonPressed(musicPack: AlbumMusic?) {
        val bundle = Bundle()
        bundle.putInt(SONG_ID, musicPack?.songId!!)
        bundle.putSerializable(SONG_DATA, musicPack)
        (activity as DashboardActivity).displayPlayerPage(bundle)
        //(activity as DashboardActivity).displayPurchasePage(bundle)
    }

    fun refreshMusicPacks() {
        viewModel.getHomeData(userID, userToken, defaultHomeResultModel)
    }

    override fun onResume() {
        super.onResume()
        showLoader()
        if(!isDataLoaded) {
            viewBinding.root.postDelayed({
                Log.e("PageLifeCycle", "HomePage Fetch Page Invoked")
                viewModel.getHomeData(userID, userToken, defaultHomeResultModel)
            }, 1000)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        LogSystem.e("PageLifeCycle", "HomePage onSaveInstanceState Invoked")
        isDataLoaded = false
    }

    override fun onPause() {
        //Clear load on memory
        releaseResources()
        super.onPause()
    }

    private fun releaseResources() {
        viewBinding.llDynamicView.removeAllViews()
        albumAdapter = null
        popularAdapter = null
        meditationAdapter = null
        recentlyPlayedAdapter = null
        recentlyPlayedList.clear()
    }
}