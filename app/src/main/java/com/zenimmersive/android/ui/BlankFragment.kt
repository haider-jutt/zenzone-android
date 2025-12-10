package com.zenimmersive.android.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.databinding.FragmentBlankBinding
import com.zenimmersive.android.repository.BlankRepository
import com.zenimmersive.android.viewmodel.BlankViewModel

class BlankFragment : BaseFragment<BlankViewModel, FragmentBlankBinding, BlankRepository>() {
    override fun getViewModel(): Class<BlankViewModel> = BlankViewModel::class.java

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBlankBinding = FragmentBlankBinding.inflate(inflater, container, false)

    override fun getRepository(): BlankRepository = BlankRepository(requireContext())

    override fun registerObservers() {
    }

    override fun unregisterObservers() {

    }
}