package com.zenimmersive.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.zenimmersive.android.R
import com.zenimmersive.android.adapter.MusicPackItemListener
import com.zenimmersive.android.adapter.VipMusicAdapter
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.FragmentVipMusicBinding
import com.zenimmersive.android.helper.Constants.All_SONGS
import com.zenimmersive.android.helper.Constants.SONG_DATA
import com.zenimmersive.android.helper.Constants.SONG_ID
import com.zenimmersive.android.repository.VipMusicRepository
import com.zenimmersive.android.ui.payment.SubscriptionBSFragment
import com.zenimmersive.android.viewmodel.VipMusicViewModel

class VipMusicFragment :
    BaseFragment<VipMusicViewModel, FragmentVipMusicBinding, VipMusicRepository>(),
    MusicPackItemListener {
    val TAG = VipMusicFragment::class.java.simpleName
    private lateinit var vipMusicAdapter: VipMusicAdapter
    private var songsList: ArrayList<AlbumMusic?> = arrayListOf()

    override fun getViewModel(): Class<VipMusicViewModel> {
        return VipMusicViewModel::class.java
    }

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentVipMusicBinding {
        return FragmentVipMusicBinding.inflate(inflater, container, false)
    }

    override fun getRepository(): VipMusicRepository {
        return VipMusicRepository(requireContext())
    }

    override fun registerObservers() {}

    override fun unregisterObservers() {}


    @OptIn(UnstableApi::class) override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLoader = viewBinding.invLoader.viewLoader

        if (activity is DashboardActivity) {
            (activity as DashboardActivity).viewBinding.tvPageTitle.text = ""
        }

        viewModel.getVipSongs()
        setAdapter()
        initObserver()
    }

    private fun setAdapter() {
        viewBinding.rcvVipSongs.layoutManager = LinearLayoutManager(requireContext())
        vipMusicAdapter = VipMusicAdapter(requireContext(), songsList, this)
        viewBinding.rcvVipSongs.adapter = vipMusicAdapter
    }

    fun initObserver() {
        viewModel.vipSongs.observe(viewLifecycleOwner) { favListRes ->
            when (favListRes) {
                is ViewState.Data -> {
                    hideLoader()
                    if (!favListRes.data.result.isNullOrEmpty()) {
                        songsList.clear()
                        songsList.addAll(favListRes.data.result ?: arrayListOf())
                    }
                    updateUi()
                }

                is ViewState.Error -> {
                    hideLoader()
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
            viewBinding.rcvVipSongs.visibility = View.VISIBLE
            viewBinding.llNotFoundData.visibility = View.GONE
            vipMusicAdapter.notifyDataSetChanged()
        } else {
            viewBinding.rcvVipSongs.visibility = View.GONE
            viewBinding.llNotFoundData.visibility = View.VISIBLE
        }
    }

    override fun onMusicPackPressed(
        songData: AlbumMusic?,
        popularList: ArrayList<AlbumMusic?>
    ) {
        if (activity is DashboardActivity) {
            val bundle = Bundle()
            bundle.putInt(SONG_ID, songData?.songId!!)
            bundle.putSerializable(SONG_DATA, songData)
            bundle.putSerializable(All_SONGS, popularList)
            (activity as DashboardActivity).displayPlayerPage(bundle)
        }
    }

    override fun onMusicPackPurchaseButtonPressed(songData: AlbumMusic?) {
        if (activity is DashboardActivity) {
            val bundle = Bundle()
            bundle.putInt(SONG_ID, songData!!.songId!!)
            bundle.putSerializable(SONG_DATA, songData)
            bundle.putSerializable(All_SONGS, songsList)
            (activity as DashboardActivity).displayPurchasePage(bundle, object : SubscriptionBSFragment.BottomSheetDismissListener {
                override fun onBottomSheetDismissed(musicPack: AlbumMusic?) {
                    vipMusicAdapter?.notifyDataSetChanged()
                }
            })
        }
    }


}