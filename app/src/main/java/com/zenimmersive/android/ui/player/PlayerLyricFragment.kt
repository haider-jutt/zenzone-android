package com.zenimmersive.android.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.databinding.FragmentPlayerLyricBinding
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import com.zenimmersive.android.repository.BlankRepository
import com.zenimmersive.android.viewmodel.BlankViewModel

class PlayerLyricFragment :
    BaseFragment<BlankViewModel, FragmentPlayerLyricBinding, BlankRepository>(),
    IPlayerSubPage {
    private var musicPack: AlbumMusic? = null
    override fun getViewModel(): Class<BlankViewModel> = BlankViewModel::class.java

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPlayerLyricBinding = FragmentPlayerLyricBinding.inflate(inflater, container, false)

    override fun getRepository(): BlankRepository = BlankRepository(requireContext())

    override fun registerObservers() {
        val lan = KeyStorage.getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE)
        viewBinding.lyricView.loadLyric(musicPack)
    }

    override fun unregisterObservers() {

    }

    override fun onPause() {
        super.onPause()
    }

    override fun setPlayerPosition(currentPosition: Long) {
        viewBinding.lyricView.setPlayerPosition(currentPosition)
    }

    override fun pageExpanded(localPlayerPageExpanded: Boolean) {
        try {
            var layoutParams = viewBinding.lyricView.layoutParams as FrameLayout.LayoutParams
            if(localPlayerPageExpanded) {
                layoutParams.topMargin = resources.getDimensionPixelSize(R.dimen.player_child_page_margin)
                layoutParams.bottomMargin = resources.getDimensionPixelSize(R.dimen.player_child_page_margin)
            } else {
                layoutParams.topMargin = 0
                layoutParams.bottomMargin = 0
            }
            viewBinding.lyricView.layoutParams = layoutParams
        }
        catch (e : Exception){}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicPack = arguments?.getSerializable("musicPack") as AlbumMusic
    }

    companion object {
        fun newInstance(musicPack: AlbumMusic): PlayerLyricFragment {
            val fragment = PlayerLyricFragment()
            val args = Bundle()
            args.putSerializable("musicPack", musicPack)
            fragment.arguments = args
            return fragment
        }
    }
}