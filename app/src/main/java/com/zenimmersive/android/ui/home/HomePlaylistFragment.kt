package com.zenimmersive.android.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.zenimmersive.android.R
import com.zenimmersive.android.adapter.HomeSubAlbumAdapter
import com.zenimmersive.android.adapter.PlaylistOnlySongsAdapter
import com.zenimmersive.android.adapter.SubAlbumSongsAdapter
import com.zenimmersive.android.apiresponsemodel.AlbumDetailsResModel
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.apiresponsemodel.SubAlbum
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.FragmentHomePlaylistBinding
import com.zenimmersive.android.helper.Constants.ALBUM_DETAILS
import com.zenimmersive.android.helper.Constants.ALBUM_ID
import com.zenimmersive.android.helper.Constants.ALBUM_IMAGE
import com.zenimmersive.android.helper.Constants.ALBUM_SESSION
import com.zenimmersive.android.helper.Constants.ALBUM_TITLE
import com.zenimmersive.android.helper.Constants.All_SONGS
import com.zenimmersive.android.helper.Constants.NOTIFICATION_ALBUM_ID
import com.zenimmersive.android.helper.Constants.SONG_DATA
import com.zenimmersive.android.helper.Constants.SONG_ID
import com.zenimmersive.android.helper.Constants.TAG_ID
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_ID
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_TOKEN
import com.zenimmersive.android.repository.HomePlaylistRepository
import com.zenimmersive.android.ui.BackPressListener
import com.zenimmersive.android.ui.DashboardActivity
import com.zenimmersive.android.viewmodel.HomePlaylistViewModel

class HomePlaylistFragment :
    BaseFragment<HomePlaylistViewModel, FragmentHomePlaylistBinding, HomePlaylistRepository>(),
    SubAlbumSongsAdapter.SubAlbumSongAdapterListener,
    PlaylistOnlySongsAdapter.PlayListOnlySongsAdapterListener,
    BackPressListener {
    //    lateinit var wanderingAdapter: HomeOtherWanderingAdapter
//    lateinit var atTheBeachAdapter: HomeOtherAtTheBeachAdapter
    private var userID: Int = 0
    lateinit var userToken: String
    private lateinit var subAlbumAdapter: HomeSubAlbumAdapter
    private lateinit var songsAdapter: PlaylistOnlySongsAdapter

    //    var albumData: HomeDataResModel.Result.Album? = null
    private var tagId: Int? = null
    private var albumId: Int? = null
    private var subAlbumList: ArrayList<SubAlbum?> = arrayListOf()
    private var songsList: ArrayList<AlbumMusic?> = arrayListOf()


    override fun getViewModel(): Class<HomePlaylistViewModel> {
        return HomePlaylistViewModel::class.java
    }

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomePlaylistBinding {
        return FragmentHomePlaylistBinding.inflate(inflater, container, false)
    }

    override fun getRepository(): HomePlaylistRepository {
        return HomePlaylistRepository(requireContext())
    }

    override fun registerObservers() {

    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userID = KeyStorage.getInstance(requireContext()).getInt(KEY_USER_ID)
        userToken = KeyStorage.getInstance(requireContext()).getString(KEY_USER_TOKEN)

        if (activity is DashboardActivity) {
            (activity as DashboardActivity).viewBinding.tvPageTitle.text = ""
        }

        initObserver()

        if (arguments?.containsKey(NOTIFICATION_ALBUM_ID) == true) {
            albumId = arguments?.getInt(NOTIFICATION_ALBUM_ID)

            // API call
            val resModel = AlbumDetailsResModel(null, null, null)
            viewModel.getHomeData(userID, userToken, albumId!!, resModel)
        }
        if (arguments?.containsKey(ALBUM_ID) == true) {
            albumId = arguments?.getInt(ALBUM_ID)

            // API call
            val resModel = AlbumDetailsResModel(null, null, null)
            viewModel.getHomeData(userID, userToken, albumId!!, resModel)
        }
        if (arguments?.containsKey(ALBUM_IMAGE) == true) {
            Glide.with(requireContext()).load(arguments?.getString(ALBUM_IMAGE)).placeholder(R.drawable.ic_abstract).error(R.drawable.ic_abstract).into(viewBinding.ivHeader)
        }
        if (arguments?.containsKey(ALBUM_TITLE) == true) {
            viewBinding.tvTitle.text = arguments?.getString(ALBUM_TITLE)
        }
        if (arguments?.containsKey(ALBUM_SESSION) == true) {
            viewBinding.tvSessions.text =
                arguments?.getInt(ALBUM_SESSION).toString() + " " + getString(R.string.sessions)
        }
        if (arguments?.containsKey(ALBUM_DETAILS) == true) {
            viewBinding.tvDetails.text = arguments?.getString(ALBUM_DETAILS)
        }
        // For the meditation tag data
        if (arguments?.containsKey(TAG_ID) == true) {
            tagId = arguments?.getInt(TAG_ID)
            viewModel.getHomeData(tagId!!)
        }

        viewBinding.ivBack.setOnClickListener {
            if (!onBackPressed()) activity?.onBackPressed()
        }

        viewBinding.llSearchView.setOnClickListener {
            if (activity is DashboardActivity) {
                val searchFragment = SearchFragment()
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.viewDashboardFragmentContainer, searchFragment)
                    ?.addToBackStack("SearchFragment")
                    ?.commit()
            }
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        viewBinding.rcvSubAlbum.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        subAlbumAdapter = HomeSubAlbumAdapter(requireContext(), subAlbumList, this)
        viewBinding.rcvSubAlbum.adapter = subAlbumAdapter
//        viewBinding.rcvSubAlbum.visibility = VISIBLE

        viewBinding.rcvMusicResult.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        songsAdapter = PlaylistOnlySongsAdapter(requireContext(), songsList, this)
        viewBinding.rcvMusicResult.adapter = songsAdapter
//        viewBinding.rcvMusicResult.visibility = VISIBLE

//        viewBinding.rcvWanderingNature.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
//        wanderingAdapter = HomeOtherWanderingAdapter(requireContext(), arrayListOf(R.drawable.ic_sample_rain1,R.drawable.ic_sample_rain2,R.drawable.ic_sample_rain3,R.drawable.ic_sample_rain1))
//        viewBinding.rcvWanderingNature.adapter = rainSoundAdapter

//        viewBinding.rcvAtBeach.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
//        atTheBeachAdapter = HomeOtherAtTheBeachAdapter(requireContext(), arrayListOf(R.drawable.ic_sample_meditation1,R.drawable.ic_sample_meditation2,R.drawable.ic_sample_meditation3,R.drawable.ic_sample_meditation1))
//        viewBinding.rcvAtBeach.adapter = atTheBeachAdapter
    }

    fun initObserver() {
        viewModel.albumDetailsData.observe(viewLifecycleOwner) { resData ->
            when (resData) {
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader("Loading...")
                }

                is ViewState.Data -> {
                    hideLoader()
                    if (!resData.data.result?.get(0)?.subAlbums.isNullOrEmpty()) {
                        subAlbumList.clear()
                        subAlbumList.addAll(resData.data.result?.get(0)?.subAlbums!!)
                        subAlbumAdapter.notifyDataSetChanged()

                    } else if (!resData.data.result?.get(0)?.songAlbums.isNullOrEmpty()) {
                        songsList.clear()
                        songsList.addAll(resData.data.result?.get(0)?.songAlbums!!)
                        songsAdapter.notifyDataSetChanged()
                        setRecyclerViewUi()
                    }

                    setRecyclerViewUi()
                }

                is ViewState.Error -> {
                    hideLoader()
                    showToast(resData.error)
                }
            }
        }
        viewModel.tagWiseMusicData.observe(viewLifecycleOwner) { resData ->
            when (resData) {
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader("Loading...")
                }

                is ViewState.Data -> {
                    hideLoader()
                    if (!resData.data.result?.get(0)?.songAlbums.isNullOrEmpty()) {
                        songsList.clear()
                        songsList.addAll(resData.data.result?.get(0)?.songAlbums!!)
                        songsAdapter.notifyDataSetChanged()
                    }
                    setRecyclerViewUi()
                }

                is ViewState.Error -> {
                    hideLoader()
                    showToast(resData.error)
                }
            }
        }
    }

    private fun setRecyclerViewUi() {
        if (!subAlbumList.isNullOrEmpty()) {
            viewBinding.rcvSubAlbum.visibility = VISIBLE
            viewBinding.rcvMusicResult.visibility = GONE
        } else if (!songsList.isNullOrEmpty()) {
            viewBinding.rcvSubAlbum.visibility = GONE
            viewBinding.rcvMusicResult.visibility = VISIBLE
        }
    }

    override fun unregisterObservers() {
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    @OptIn(UnstableApi::class)
    override fun onSubAlbumSongClick(
        subAlbumSongsList: ArrayList<AlbumMusic?>?,
        songData: AlbumMusic
    ) {
        if(activity is DashboardActivity){
            val bundle = Bundle()
            bundle.putInt(SONG_ID, songData.songId!!)
            bundle.putSerializable(SONG_DATA, songData)
            bundle.putSerializable(All_SONGS, subAlbumSongsList)
//            if (songData.isPurchased != 1){
            // The following line is commented out to always open the player fragment regardless of purchase/subscription status
            /*if (songData != null && KeyStorage.getInstance(requireActivity()).shouldShowSubscriptionSheet(requireActivity(), songData)){
                (activity as DashboardActivity).displayPurchasePage(bundle)
            }else{*/
                (activity as DashboardActivity).displayPlayerPage(bundle)
//            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onPlayListOnlySongsAdapterClick(songData: AlbumMusic) {
        if (activity is DashboardActivity) {
            val bundle = Bundle()
            bundle.putInt(SONG_ID, songData.songId!!)
            bundle.putSerializable(SONG_DATA, songData)
            bundle.putSerializable(All_SONGS, songsList)
            (activity as DashboardActivity).displayPlayerPage(bundle)
            /*if (songData.isPurchased != 1)*/
            // The following line is commented out to always open the player fragment regardless of purchase/subscription status
           /* if (songData != null && KeyStorage.getInstance(requireActivity()).shouldShowSubscriptionSheet(requireActivity(), songData)){
                (activity as DashboardActivity).displayPurchasePage(bundle)
            }else{
                (activity as DashboardActivity).displayPlayerPage(bundle)
            }*/
        }
    }

}