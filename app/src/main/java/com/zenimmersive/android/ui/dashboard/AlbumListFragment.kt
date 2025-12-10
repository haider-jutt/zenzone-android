package com.zenimmersive.android.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.databinding.FragmentAlbumListBinding
import com.zenimmersive.android.repository.HomeRepository
import com.zenimmersive.android.viewmodel.AlbumListViewModel

class AlbumListFragment :
    BaseFragment<AlbumListViewModel, FragmentAlbumListBinding, HomeRepository>() {
    override fun getViewModel(): Class<AlbumListViewModel> = AlbumListViewModel::class.java

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAlbumListBinding = FragmentAlbumListBinding.inflate(inflater, container, false)

    override fun getRepository(): HomeRepository = HomeRepository(requireContext())

    override fun registerObservers() {

    }

    override fun unregisterObservers() {

    }
}