package com.zenimmersive.android.base

interface FragmentViewPressEvent {
    fun onViewPressed(viewId: Int)
    fun showLoader()
    fun showBridgePairFragment()
    fun hideLoader()
    fun showBridgeConfigureFragment(isSuccess: Boolean)
    fun showLightsFragment()
}