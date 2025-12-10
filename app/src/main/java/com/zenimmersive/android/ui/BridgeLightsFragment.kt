package com.zenimmersive.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.zenimmersive.android.RcvLightsAdapter
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.databinding.FragmentBridgeLightsBinding
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.hue.HueLightManager
import com.zenimmersive.android.hue.TaskCallback
import com.zenimmersive.android.model.LightListResult
import com.zenimmersive.android.repository.BlankRepository
import com.zenimmersive.android.viewmodel.BlankViewModel

class BridgeLightsFragment :
    BaseFragment<BlankViewModel, FragmentBridgeLightsBinding, BlankRepository>() {
    override fun getViewModel(): Class<BlankViewModel> = BlankViewModel::class.java

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBridgeLightsBinding = FragmentBridgeLightsBinding.inflate(inflater, container, false)

    override fun getRepository(): BlankRepository = BlankRepository(requireContext())


    var lightListResult: LightListResult? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.rcvLightGroupList.layoutManager = LinearLayoutManager(requireContext())

        viewBinding.viewSwipeRefresh.setOnRefreshListener {
            HueLightManager.getInstance(requireContext())
                ?.fetchLights(object : TaskCallback<LightListResult> {
                    override fun onTaskComplete(result: LightListResult) {
                        bindResult(result)
                        viewBinding.viewSwipeRefresh.isRefreshing = false
                    }

                    override fun onTaskError(error: String?) {
                        viewBinding.viewSwipeRefresh.isRefreshing = false
                    }

                }, false)
        }
    }

    var hueLightManager: HueLightManager? = null
    override fun registerObservers() {
        getParentHandler()?.showLoader()

        hueLightManager = HueLightManager.getInstance(requireContext())
        hueLightManager?.fetchLightListByCache().let {
            if (it == null) {
                fetchLightAsync()
            } else {
                if (it.error.isNullOrEmpty() && it.hasLights()) {
                    //shareLogFile()
                    bindResult(it!!)
                    getParentHandler()?.hideLoader()
                } else {
                    fetchLightAsync()
                }
            }
        }

    }

    private fun fetchLightAsync() {
        hueLightManager?.fetchLights(object : TaskCallback<LightListResult> {
            override fun onTaskComplete(result: LightListResult) {
                bindResult(result)
                viewBinding.root.postDelayed({
                    getParentHandler()?.hideLoader()
                    //shareLogFile()
                }, 500)
            }

            override fun onTaskError(error: String?) {
                getParentHandler()?.hideLoader()
                //shareLogFile()
            }

        },false)
    }

    private fun bindResult(result: LightListResult) {
        LogSystem.e("LightPage", "bindResult")
        lightListResult = result
        hueLightManager?.saveLightListByCache(result)
        var adapter = RcvLightsAdapter(KeyStorage.getInstance(requireContext()).fetchMaxHueLights())
        viewBinding.rcvLightGroupList.adapter = adapter
        adapter.bindResult(result)
    }

    override fun unregisterObservers() {
        LogSystem.e("LightPage", "unregisterObservers $hueLightManager")
        lightListResult?.let { hueLightManager?.saveLightListByCache(it) }
    }

}