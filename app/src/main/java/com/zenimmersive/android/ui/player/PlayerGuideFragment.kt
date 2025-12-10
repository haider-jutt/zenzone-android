package com.zenimmersive.android.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.databinding.FragmentPlayerGuideBinding
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import com.zenimmersive.android.repository.BlankRepository
import com.zenimmersive.android.viewmodel.BlankViewModel

class PlayerGuideFragment :
    BaseFragment<BlankViewModel, FragmentPlayerGuideBinding, BlankRepository>(),
    IPlayerSubPage {
    var musicPack: AlbumMusic? = null
    lateinit var lan: String

    override fun getViewModel(): Class<BlankViewModel> = BlankViewModel::class.java

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPlayerGuideBinding = FragmentPlayerGuideBinding.inflate(inflater, container, false)

    override fun getRepository(): BlankRepository = BlankRepository(requireContext())

    override fun registerObservers() {
    }

    override fun unregisterObservers() {

    }

    override fun setPlayerPosition(currentPosition: Long) {

    }

    override fun pageExpanded(localPlayerPageExpanded: Boolean) {
        try {
            var layoutParams = viewBinding.wvGuided.layoutParams as FrameLayout.LayoutParams
            if(localPlayerPageExpanded) {
                layoutParams.topMargin = resources.getDimensionPixelSize(R.dimen.player_child_page_margin)
                layoutParams.bottomMargin = resources.getDimensionPixelSize(R.dimen.player_child_page_margin)
            } else {
                layoutParams.topMargin = 0
                layoutParams.bottomMargin = 0
            }
            viewBinding.wvGuided.layoutParams = layoutParams
        }
        catch (e : Exception){}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicPack = arguments?.getSerializable("musicPack") as AlbumMusic?
    }

    companion object {
        fun newInstance(musicPack: AlbumMusic): PlayerGuideFragment {
            val fragment = PlayerGuideFragment()
            val args = Bundle()
            args.putSerializable("musicPack", musicPack)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lan = KeyStorage.getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE)

        var prefLang = if (lan.isNullOrBlank()) "en" else lan
        if (prefLang == "en") {
            viewBinding.wvGuided.loadUrl(musicPack?.guidedFile ?: "")
        } else {
            viewBinding.wvGuided.loadUrl(musicPack?.guidedFileFrench ?: "")
        }

        viewBinding.wvGuided.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        viewBinding.wvGuided.settings.javaScriptEnabled = true
        viewBinding.wvGuided.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(
                    """document.body.style.background = 'transparent';""".trimIndent(), null
                )
            }
        }


    }
}