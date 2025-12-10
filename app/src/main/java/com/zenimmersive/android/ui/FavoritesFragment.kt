package com.zenimmersive.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.zenimmersive.android.R
import com.zenimmersive.android.adapter.FavoritesAdapter
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.FragmentFavoritesBinding
import com.zenimmersive.android.helper.Constants.All_SONGS
import com.zenimmersive.android.helper.Constants.SONG_DATA
import com.zenimmersive.android.helper.Constants.SONG_ID
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.repository.FavoritesRepository
import com.zenimmersive.android.ui.payment.SubscriptionBSFragment
import com.zenimmersive.android.viewmodel.FavoritesViewModel

class FavoritesFragment :
    BaseFragment<FavoritesViewModel, FragmentFavoritesBinding, FavoritesRepository>(),
    FavoritesAdapter.FavoriteAdapterLister {
    private val TAG = FavoritesFragment::class.java.simpleName
    private lateinit var favoriteAdapter: FavoritesAdapter
    private var songsList: ArrayList<AlbumMusic?> = arrayListOf()

    override fun getViewModel(): Class<FavoritesViewModel> {
        return FavoritesViewModel::class.java
    }

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentFavoritesBinding {
        return FragmentFavoritesBinding.inflate(inflater, container, false)
    }

    override fun getRepository(): FavoritesRepository {
        return FavoritesRepository(requireContext())
    }

    override fun registerObservers() {}

    override fun unregisterObservers() {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLoader = viewBinding.invLoader.viewLoader

        if (activity is DashboardActivity) {
            (activity as DashboardActivity).viewBinding.tvPageTitle.text = ""
        }

        setAdapter()
        initView()
        initObserver()
    }

    private fun setAdapter() {
        viewBinding.rcvFavoriteSongs.layoutManager = LinearLayoutManager(requireContext())
        favoriteAdapter = FavoritesAdapter(requireContext(), songsList, this)
        viewBinding.rcvFavoriteSongs.adapter = favoriteAdapter
    }

    private fun initView() {
        viewModel.getFavoriteSongs()
    }

    fun initObserver() {
        viewModel.favoriteSongsRes.observe(viewLifecycleOwner) { favListRes ->
            when (favListRes) {
                is ViewState.Data -> {
                    hideLoader()
                    if (favListRes.data.status == 1) {
                        if (!favListRes.data.result.isNullOrEmpty()) {
                            songsList.clear()
                            songsList.addAll(favListRes.data?.result!!)
                        }
                    }
                    updateUi()
                }

                is ViewState.Error -> {
                    hideLoader()
                    showToast(favListRes.error)
                }

                is ViewState.Loading -> {
                    showLoader(getString(R.string.loading))
                }

                is ViewState.DefaultState -> {}
            }
        }
    }

    fun updateUi() {
        if (!songsList.isNullOrEmpty()) {
            viewBinding.rcvFavoriteSongs.visibility = View.VISIBLE
            viewBinding.llNotFoundData.visibility = View.GONE
            favoriteAdapter.notifyDataSetChanged()
        } else {
            viewBinding.rcvFavoriteSongs.visibility = View.GONE
            viewBinding.llNotFoundData.visibility = View.VISIBLE
        }
    }

    @OptIn(UnstableApi::class)
    override fun favoriteSongClick(songData: AlbumMusic) {
        if (activity is DashboardActivity) {
            val bundle = Bundle()
            bundle.putInt(SONG_ID, songData.songId!!)
            bundle.putSerializable(SONG_DATA, songData)
            bundle.putSerializable(All_SONGS, songsList)
//            if (songData.isPurchased != 1){
            if (songData != null && KeyStorage.getInstance(requireActivity())
                    .shouldShowPaidStatus(requireActivity(), songData)
            ) {
                (activity as DashboardActivity).displayPurchasePage(
                    bundle,
                    object : SubscriptionBSFragment.BottomSheetDismissListener {
                        override fun onBottomSheetDismissed(musicPack: AlbumMusic?) {
                            favoriteAdapter?.notifyDataSetChanged()
                        }
                    })
            } else {
                (activity as DashboardActivity).displayPlayerPage(bundle)
            }
        }
    }


}