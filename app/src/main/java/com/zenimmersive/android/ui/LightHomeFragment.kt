package com.zenimmersive.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zenimmersive.android.R
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.base.FragmentViewPressEvent
import com.zenimmersive.android.databinding.FragmentLightHomeBinding
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.helper.hide
import com.zenimmersive.android.helper.show
import com.zenimmersive.android.hue.HueLightManager
import com.zenimmersive.android.repository.BlankRepository
import com.zenimmersive.android.viewmodel.BlankViewModel

class LightHomeFragment : BaseFragment<BlankViewModel, FragmentLightHomeBinding, BlankRepository>(),
    FragmentViewPressEvent {

    private lateinit var hueLightManager: HueLightManager
    private val homeFragment: BridgeHomeFragment = BridgeHomeFragment()
    private val bridgeConfigPage: BridgeConfigureFragment = BridgeConfigureFragment()
    private val bridgePairPage: BridgePairFragment = BridgePairFragment()
    private val bridgeLightsFragment: BridgeLightsFragment = BridgeLightsFragment()
    private val bridgeLightsColor: ColorFragment = ColorFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hueLightManager = HueLightManager.getInstance(requireContext())

        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount > 0) {
                viewBinding.ivBack.show()
            } else {
                viewBinding.ivBack.hide()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLoader = viewBinding.invLoader.viewLoader

        viewBinding.ivBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        viewBinding.ivBack.visibility = View.GONE
        viewBinding.root.post {
            addDefaultView()
        }
    }

    private fun addDefaultView() {
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.viewContainer, homeFragment)
        transaction.commit()
    }

    override fun getViewModel(): Class<BlankViewModel> = BlankViewModel::class.java

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLightHomeBinding = FragmentLightHomeBinding.inflate(inflater, container, false)

    override fun getRepository(): BlankRepository = BlankRepository(requireContext())
    override fun registerObservers() {

    }

    override fun unregisterObservers() {

    }


    override fun onViewPressed(viewId: Int) {
        when (viewId) {
            R.id.viewConfigBridge -> {
                showLoader()
                showBridgeConfigureFragment()
                hideLoader()
            }

            R.id.viewBridgeLights -> {
                showLoader()
                showLightsFragment()
                hideLoader()
            }

//            R.id.viewBridgeColor -> {
//                showLoader()
//                showColorFragment()
//                hideLoader()
//            }
        }
    }

    private fun showBridgeConfigureFragment() {
        LogSystem.e("Showing Configure Page")
        val transaction = childFragmentManager.beginTransaction()
        bridgeConfigPage.arguments?.clear()
        transaction.replace(R.id.viewContainer, bridgeConfigPage)
        transaction.addToBackStack("BridgeConfigureFragment")
        transaction.commit()
    }

    override fun showBridgeConfigureFragment(isSuccess: Boolean) {
        LogSystem.e("Showing Configure Page By Result")
        removeBackstackPages()
        val transaction = childFragmentManager.beginTransaction()
        bridgeConfigPage.arguments?.clear()
        bridgeConfigPage.arguments = Bundle().apply {
            putBoolean("isSuccess", isSuccess)
        }
        transaction.replace(R.id.viewContainer, bridgeConfigPage)
        transaction.addToBackStack("BridgeConfigureFragment")
        transaction.commit()
    }

    private fun removeBackstackPages() {
        val fm = childFragmentManager
        for (i in 1 until fm.backStackEntryCount) {
            fm.popBackStack()
        }
    }

    override fun showBridgePairFragment() {
        LogSystem.e("Showing Pairing Page")
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.viewContainer, bridgePairPage)
        transaction.addToBackStack("BridgePairFragment")
        transaction.commit()
    }

    override fun showLightsFragment() {
        LogSystem.e("Showing Light Page")
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.viewContainer, bridgeLightsFragment)
        transaction.addToBackStack("LightsFragment")
        transaction.commit()
    }

    private fun showColorFragment() {
        LogSystem.e("Showing Color Page")
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.viewContainer, bridgeLightsColor)
        transaction.addToBackStack("ColorFragment")
        transaction.commit()
    }
}
