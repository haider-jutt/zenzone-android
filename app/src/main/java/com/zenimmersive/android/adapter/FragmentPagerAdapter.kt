package com.zenimmersive.android.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

open class FragmentPagerAdapter(
    fm: FragmentManager,
    private val fragments: List<Fragment>
) : androidx.fragment.app.FragmentStatePagerAdapter(
    fm,
    androidx.fragment.app.FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) {
    override fun getCount(): Int {
        return fragments.size
    }

    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }

    fun getItemOrNull(position: Int): Fragment? {
        if (position < 0 || position >= fragments.size) {
            return null
        }
        return fragments[position]
    }

    fun getItemList() = fragments

}
