package com.zenimmersive.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.GridLayoutManager
import com.zenimmersive.android.R
import com.zenimmersive.android.adapter.AlbumItemPressListener
import com.zenimmersive.android.adapter.ViewAllAlbumsAdapter
import com.zenimmersive.android.apiresponsemodel.AlbumDetailsResModel
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.FragmentViewAllAlbumsBinding
import com.zenimmersive.android.helper.Constants.ALBUM_DETAILS
import com.zenimmersive.android.helper.Constants.ALBUM_ID
import com.zenimmersive.android.helper.Constants.ALBUM_IMAGE
import com.zenimmersive.android.helper.Constants.ALBUM_SESSION
import com.zenimmersive.android.helper.Constants.ALBUM_TITLE
import com.zenimmersive.android.helper.LayoutMarginDecoration
import com.zenimmersive.android.repository.ViewAllAlbumRepository
import com.zenimmersive.android.ui.home.HomePlaylistFragment
import com.zenimmersive.android.viewmodel.ViewAllAlbumViewModel
import kotlin.math.roundToInt

class ViewAllAlbumsFragment : BaseFragment<ViewAllAlbumViewModel, FragmentViewAllAlbumsBinding, ViewAllAlbumRepository>(), AlbumItemPressListener, BackPressListener {
    private val Tag = ViewAllAlbumsFragment::class.java.simpleName
    private lateinit var albumAdapter: ViewAllAlbumsAdapter
    private var albumList: ArrayList<AlbumDetailsResModel.Result?> = arrayListOf()

    override fun getViewModel(): Class<ViewAllAlbumViewModel> {
        return ViewAllAlbumViewModel::class.java
    }

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentViewAllAlbumsBinding {
        return FragmentViewAllAlbumsBinding.inflate(inflater, container, false)
    }

    override fun getRepository(): ViewAllAlbumRepository {
        return ViewAllAlbumRepository(requireContext())
    }

    override fun registerObservers() {
    }

    override fun unregisterObservers() {
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.ivBack.setOnClickListener {
            activity?.onBackPressed()
        }
        viewLoader = viewBinding.invLoader.viewLoader

        viewModel.getAllAlbumsData(false)
        setupRecyclerView()
        initObserver()

        viewBinding.ivBack.setOnClickListener {
            if (!onBackPressed()) activity?.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        viewBinding.rcvAlbums.layoutManager = GridLayoutManager(requireContext(),2)
        albumAdapter = ViewAllAlbumsAdapter(requireContext(), albumList, this@ViewAllAlbumsFragment)
        viewBinding.rcvAlbums.addItemDecoration(
            LayoutMarginDecoration(
                resources.getDimension(
                    com.intuit.sdp.R.dimen._5sdp
                ).roundToInt(), 0, resources.getDimension(
                    com.intuit.sdp.R.dimen._5sdp
                ).roundToInt(), resources.getDimension(
                    com.intuit.sdp.R.dimen._15sdp
                ).roundToInt()
            )
        )
        viewBinding.rcvAlbums.adapter = albumAdapter
    }

    private fun manageRecyclerViewData(newList: ArrayList<AlbumDetailsResModel.Result?>){
        if (!newList.isNullOrEmpty()){
            viewBinding.rcvAlbums.visibility = VISIBLE
            viewBinding.llNotFoundData.visibility = GONE
            albumAdapter.notifyDataSetChanged()
        }else{
            viewBinding.rcvAlbums.visibility = GONE
            viewBinding.llNotFoundData.visibility = VISIBLE
        }
    }

    private fun initObserver(){
        viewModel.homeResData.observe(viewLifecycleOwner) { resData ->
            when (resData) {
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader("Loading...")
                }

                is ViewState.Data -> {
                    hideLoader()
                    if (!resData.data.result.isNullOrEmpty()){
                        albumList.clear()
                        albumList.addAll(resData.data.result!!)
                    }
                    manageRecyclerViewData(albumList)
                }

                is ViewState.Error -> {
                    hideLoader()
                    showToast(resData.error)
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun albumClickListener(
        albumId: Int,
        albumImage: String,
        albumTitle: String,
        albumSession: Int,
        details: String
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
}