package com.zenimmersive.android.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.databinding.FragmentBridgeSearchBinding
import com.zenimmersive.android.hue.HueLightManager
import com.zenimmersive.android.hue.TaskCallback
import com.zenimmersive.android.repository.BlankRepository
import com.zenimmersive.android.viewmodel.BlankViewModel
import com.zenimmersive.android.R

class BridgePairFragment :
    BaseFragment<BlankViewModel, FragmentBridgeSearchBinding, BlankRepository>() {
    override fun getViewModel(): Class<BlankViewModel> = BlankViewModel::class.java

    override fun getActivityBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentBridgeSearchBinding = FragmentBridgeSearchBinding.inflate(inflater, container, false)

    override fun getRepository(): BlankRepository = BlankRepository(requireContext())

    override fun registerObservers() {

    }

    override fun unregisterObservers() {
    }


    var handler = Handler()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        //var ipAddress = arguments?.getString("ip_address")
        viewBinding.root.post {
            timerSeconds = 60
            fetchHueApiVersion()
            createUser()
        }

        postTimer(0)
    }

    fun fetchHueApiVersion() {
        var hueLightManager = HueLightManager.getInstance(requireContext())
        hueLightManager.fetchApiConfig(taskCallback = object : TaskCallback<String> {
            override fun onTaskComplete(result: String) {
                createUser()
            }

            override fun onTaskError(error: String?) {
                getParentHandler()?.showBridgeConfigureFragment(false)
            }

        }, caller = "fetchHueApiVersion")
    }

    private fun createUser() {
        if (isSafe()) {
            var hueLightManager = HueLightManager.getInstance(requireContext())
            handler.postDelayed({
                timerSeconds = 60
                hueLightManager.createUser(taskCallback = object : TaskCallback<String> {
                    override fun onTaskComplete(result: String) {
                        removeCallbacks()
                        if (isSafe()) {
                            getParentHandler()?.showBridgeConfigureFragment(true)
                        }
                    }

                    override fun onTaskError(error: String?) {
                        if (timerSeconds > 0) {
                            handler.postDelayed({
                                createUser()
                            }, 2000)
                        } else {
                            removeCallbacks()
                            getParentHandler()?.showBridgeConfigureFragment(false)
                        }
                    }

                }, caller = "Initial Handler")
            }, 3000)
        }
    }

    private fun removeCallbacks() {
        handler.removeCallbacksAndMessages(null)
        handlerMain.removeCallbacksAndMessages(null)
    }

    var handlerMain = Handler(Looper.getMainLooper())
    var updateTask = Runnable {
        viewBinding.viewProcessInformation.setText(getString(R.string.bridge_push_button_timer, timerSeconds))
        timerSeconds--
        if (timerSeconds > 0) {
            postTimer(1000)
        }
    }

    private fun postTimer(time: Int) {
        handlerMain.postDelayed(updateTask, time.toLong())
    }

    var timerSeconds = 60

}