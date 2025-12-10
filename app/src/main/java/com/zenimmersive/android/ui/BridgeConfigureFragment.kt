package com.zenimmersive.android.ui

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zenimmersive.android.R
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.databinding.FragmentBridgeConfigureBinding
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.helper.hide
import com.zenimmersive.android.helper.show
import com.zenimmersive.android.hue.Bridge
import com.zenimmersive.android.hue.HueLightManager
import com.zenimmersive.android.hue.TaskCallback
import com.zenimmersive.android.repository.BlankRepository
import com.zenimmersive.android.viewmodel.BlankViewModel

class BridgeConfigureFragment :
    BaseFragment<BlankViewModel, FragmentBridgeConfigureBinding, BlankRepository>() {
    override fun getViewModel(): Class<BlankViewModel> = BlankViewModel::class.java

    override fun getActivityBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentBridgeConfigureBinding =
        FragmentBridgeConfigureBinding.inflate(inflater, container, false)

    override fun getRepository(): BlankRepository = BlankRepository(requireContext())

    override fun registerObservers() {
        setupUI()
    }

    private fun setupUI() {
        hueLightManager = HueLightManager.getInstance(requireContext())
        bridgeSyncStateHandler = Handler()

        var extraMessage = arguments?.containsKey("isSuccess") ?: false
        if (extraMessage) {

            if (arguments?.getBoolean("isSuccess") == true) {
                viewBinding.parentMessageSuccessView.show()
                viewBinding.parentMessageErrorView.hide()

                bridgePairedMode(true)
            } else {
                viewBinding.parentMessageSuccessView.hide()
                viewBinding.parentMessageErrorView.show()

                bridgePairedMode(false)
            }
        } else {
            bridgePairedMode(false)
        }


        arguments?.clear()

        viewBinding.viewSearch.setOnClickListener {
            hueLightManager.stopRequests()
            bridgePairedMode(false)
            viewBinding.parentMessageSuccessView.hide()
            viewBinding.parentMessageErrorView.hide()

            viewBinding.viewSearch.animate().alpha(0f).setDuration(300).start()
            viewBinding.viewSearch.isEnabled = false
            getParentHandler()?.showLoader()
            hueLightManager.discoverBridges(object : TaskCallback<List<Bridge>> {
                override fun onTaskComplete(result: List<Bridge>) {

                    viewBinding.viewSearch.isEnabled = true
                    viewBinding.viewSearch.animate().alpha(1f).setDuration(300).start()

                    viewBinding.root.post {
                        getParentHandler()?.showBridgePairFragment()
                        getParentHandler()?.hideLoader()
                    }
                }

                override fun onTaskError(error: String?) {
                    viewBinding.root.post {
                        viewBinding.parentMessageErrorView.show()
                        getParentHandler()?.hideLoader()
                        viewBinding.viewSearch.isEnabled = true
                        viewBinding.viewSearch.animate().alpha(1f).setDuration(300).start()
                    }
                }

            })
        }
        viewBinding.viewConfigureLights.setOnClickListener {
            getParentHandler()?.showLoader()
            viewBinding.root.post {
                getParentHandler()?.showLightsFragment()
                getParentHandler()?.hideLoader()
            }
        }

        viewBinding.viewRemoveBridge.setOnClickListener {
            hueLightManager.clearBridgeDetails()
            bridgePairedMode(false)
        }


        if (hueLightManager.isBridgeDiscovered() && hueLightManager.isBridgeHueApiTokenValid()) {
            viewBinding.bridgeSyncMessage.show()
            bridgePairedMode(true)
            viewBinding.viewConfigureLights.hide()
            postSyncState()
        } else {
            bridgePairedMode(false)
        }

    }

    private fun bridgePairedMode(isPaired: Boolean, caller: String = "") {
        LogSystem.e("BridgeConfigureFragment", "Bridge paired mode: $isPaired, caller: $caller")
        if (isPaired) {
            viewBinding.viewSearch.hide()
            viewBinding.viewConfigureLights.show()
            viewBinding.viewRemoveBridge.show()
        } else {
            viewBinding.viewSearch.show()
            viewBinding.viewConfigureLights.hide()
            viewBinding.viewRemoveBridge.hide()
        }
    }

    override fun unregisterObservers() {
        bridgeSyncStateHandler?.removeCallbacksAndMessages(null)
        bridgeSyncStateHandler = null
    }

    var bridgeSyncStateHandler: Handler? = null
    lateinit var hueLightManager: HueLightManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is HueManagementActivity) {
            (activity as HueManagementActivity).viewBinding.tvPageTitle.text = getString(R.string.configure_bridge)
        }
    }

    var syncStateRunnable = Runnable {
        if(!hueLightManager.isBridgeHueApiTokenValid())
        {
            viewBinding.bridgeSyncMessage.hide()
            return@Runnable
        }
        hueLightManager.refreshBridgeStatus(object : TaskCallback<String> {
            override fun onTaskComplete(result: String) {
                viewBinding.bridgeSyncMessage.hide()
                if (hueLightManager.isBridgeHueApiTokenValid()) bridgePairedMode(true)
                else bridgePairedMode(false)
            }

            override fun onTaskError(error: String?) {
                if (isSafe()) postSyncState()
            }
        })
    }

    private fun postSyncState() {
        viewBinding.bridgeSyncMessage.setMessage(getString(R.string.attempting_to_connect_bridge_message))
        bridgeSyncStateHandler?.postDelayed(syncStateRunnable, 3000)
    }

}