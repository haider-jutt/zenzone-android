package com.zenimmersive.android.ui

import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zenimmersive.android.R
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.databinding.FragmentBridgeHomeBinding
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import com.zenimmersive.android.helper.hide
import com.zenimmersive.android.helper.show
import com.zenimmersive.android.hue.HueLightManager
import com.zenimmersive.android.hue.TaskCallback
import com.zenimmersive.android.repository.BlankRepository
import com.zenimmersive.android.viewmodel.BlankViewModel


class BridgeHomeFragment :
    BaseFragment<BlankViewModel, FragmentBridgeHomeBinding, BlankRepository>() {
    override fun getViewModel(): Class<BlankViewModel> = BlankViewModel::class.java

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBridgeHomeBinding = FragmentBridgeHomeBinding.inflate(inflater, container, false)

    override fun getRepository(): BlankRepository = BlankRepository(requireContext())

    override fun registerObservers() {

    }

    override fun unregisterObservers() {
    }


    var bridgeSyncStateHandler: Handler? = null
    lateinit var hueLightManager: HueLightManager


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is HueManagementActivity) {
            (activity as HueManagementActivity).viewBinding.tvPageTitle.text = getString(R.string.light_management)
        }

        hueLightManager = HueLightManager.getInstance(requireContext())
        bridgeSyncStateHandler = Handler()

        if(KeyStorage.getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE).ifEmpty { "en" }.equals("en"))
            viewBinding.viewInfo.setText(Html.fromHtml("Using this app, you can sync real <b>Philips HUE lights</b> for a great light show to relax. The benefits are limitless, offering not only light therapy but also music therapy. Start your journey now! Want to buy Philips Lights? <a href=\"https://www.philips-hue.com\">Click here to order.</a>"))
        else
            viewBinding.viewInfo.setText(Html.fromHtml("Avec cette application, vous pouvez synchroniser de vraies lampes <b>Philips Hue</b> pour un superbe spectacle lumineux relaxant. Les bienfaits sont infinis : elle offre non seulement une luminothérapie, mais aussi une musicothérapie. Commencez votre voyage dès maintenant! Vous souhaitez acheter des lampes Philips ? <a href=\"https://www.philips-hue.com\">Cliquez ici pour commander.</a>"))

        viewBinding.viewConfigBridge.setOnClickListener {
            getParentHandler()?.onViewPressed(it.id)
        }

        viewBinding.viewBridgeLights.setOnClickListener {
            getParentHandler()?.onViewPressed(it.id)
        }
//        viewBinding.viewBridgeColor.setOnClickListener {
//            getParentHandler()?.onViewPressed(it.id)
//        }
//
//
//        viewBinding.viewBridgeColor.hide()

        viewBinding.viewBridgeLights.hide()
        if (hueLightManager.isBridgeDiscovered() && hueLightManager.isBridgeHueApiTokenValid()) {
            viewBinding.bridgeSyncMessage.show()
            postSyncState()
        }


        viewBinding.viewShareLogFile.setOnClickListener {
            shareLogFile()
        }
    }


    var syncStateRunnable = Runnable {
        hueLightManager.refreshBridgeStatus(object : TaskCallback<String> {
            override fun onTaskComplete(result: String) {
                viewBinding.bridgeSyncMessage.hide()
                //viewBinding.viewBridgeColor.show()
                viewBinding.viewBridgeLights.show()
            }

            override fun onTaskError(error: String?) {
                if (isSafe()) postSyncState()
            }

        },true)
    }

    private fun postSyncState() {
        viewBinding.bridgeSyncMessage.setMessage(getString(R.string.attempting_to_connect_bridge_message))
        bridgeSyncStateHandler?.postDelayed(syncStateRunnable, 3000)
    }


}