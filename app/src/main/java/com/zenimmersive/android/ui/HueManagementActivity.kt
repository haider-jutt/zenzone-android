package com.zenimmersive.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import com.zenimmersive.android.R
import com.zenimmersive.android.base.BaseActivity
import com.zenimmersive.android.base.FragmentViewPressEvent
import com.zenimmersive.android.databinding.ActivityMainBinding
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.hue.HueLightManager
import com.zenimmersive.android.repository.BlankRepository
import com.zenimmersive.android.viewmodel.BlankViewModel

class HueManagementActivity : BaseActivity<BlankViewModel, ActivityMainBinding, BlankRepository>(),
    FragmentViewPressEvent {

    lateinit var hueLightManager: HueLightManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewLoader = viewBinding.invLoader.viewLoader

        viewBinding.ivBack.setOnClickListener {
            onBackPressed()
        }

        hueLightManager = HueLightManager.getInstance(this)

//        viewBinding.ivBack.visibility = View.GONE

        viewBinding.root.post {
            addDefaultView()
        }

//        supportFragmentManager.addOnBackStackChangedListener {
//            if (supportFragmentManager.backStackEntryCount > 0) {
//                viewBinding.ivBack.show()
//            } else {
//                viewBinding.ivBack.hide()
//            }
//        }

        viewBinding.root.post {
            hideLoader()
        }
    }

    var homeFragment: BridgeHomeFragment = BridgeHomeFragment()
    var bridgeConfigPage: BridgeConfigureFragment = BridgeConfigureFragment()
    var bridgeLightsFragment: BridgeLightsFragment = BridgeLightsFragment()
    var bridgeLightsColor: ColorFragment = ColorFragment()

    private fun addDefaultView() {
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.viewContainer, homeFragment)
        //transaction.addToBackStack("HomeFragment")
        transaction.commit()
    }


    override fun getViewModel(): Class<BlankViewModel> = BlankViewModel::class.java

    override fun getActivityBinding(inflater: LayoutInflater): ActivityMainBinding =
        ActivityMainBinding.inflate(inflater)

    override fun getRepository(): BlankRepository = BlankRepository(this)

    override fun bindViewModel() {

    }

    override fun removeViewModelCallbacks() {

    }

    override fun onViewPressed(viewId: Int) {
        if (viewId == R.id.viewConfigBridge) {
            showLoader()
            showBridgeConfigureFragment()
            hideLoader()
        } else if (viewId == R.id.viewBridgeLights) {
            showLoader()
            showLightsFragment()
            hideLoader()
        }

//        else if (viewId == R.id.viewBridgeColor) {
//            showLoader()
//            showColorFragment()
//            hideLoader()
//        }
    }

    open fun showBridgeConfigureFragment() {
        LogSystem.e("Showing Configure Page")
        var transaction = supportFragmentManager.beginTransaction()
        bridgeConfigPage.arguments?.clear()
        transaction.replace(R.id.viewContainer, bridgeConfigPage)
        transaction.addToBackStack("BridgeConfigureFragment")
        transaction.commit()
    }

    override fun showBridgeConfigureFragment(isSuccess: Boolean) {
        LogSystem.e("HueManagementActivity Showing Configure Page By Result")
        removeBackstackPages();
        var transaction = supportFragmentManager.beginTransaction()
        bridgeConfigPage.arguments?.clear()
        bridgeConfigPage.arguments = Bundle()
        bridgeConfigPage.arguments?.putBoolean("isSuccess", isSuccess)
        transaction.replace(R.id.viewContainer, bridgeConfigPage)
        transaction.addToBackStack("BridgeConfigureFragment")
        transaction.commit()
    }

    private fun removeBackstackPages() {
        val fm = supportFragmentManager
        for (i in 1 until fm.backStackEntryCount) {
            fm.popBackStack()
        }
    }

    override fun showBridgePairFragment() {
        LogSystem.e("HueManagementActivity Showing Pairing Page")
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.viewContainer, BridgePairFragment())
        transaction.addToBackStack("BridgePairFragment")
        transaction.commit()
    }

    override fun showLightsFragment() {
        LogSystem.e("HueManagementActivity Showing Light Page")
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.viewContainer, bridgeLightsFragment)
        transaction.addToBackStack("LightsFragment")
        transaction.commit()
    }

    fun showColorFragment() {
        LogSystem.e("Showing Color Page")
        var transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.viewContainer, bridgeLightsColor)
        transaction.addToBackStack("ColorFragment")
        transaction.commit()
    }
}