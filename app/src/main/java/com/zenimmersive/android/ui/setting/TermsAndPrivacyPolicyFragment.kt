package com.zenimmersive.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.StaticPagesResModel
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.FragmentTermsAndPrivacyPolicyBinding
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_ID
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_TOKEN
import com.zenimmersive.android.helper.toHtmlString
import com.zenimmersive.android.repository.SettingRepository
import com.zenimmersive.android.ui.BackPressListener
import com.zenimmersive.android.viewmodel.StaticPageViewModel

class TermsAndPrivacyPolicyFragment : BaseFragment<StaticPageViewModel, FragmentTermsAndPrivacyPolicyBinding, SettingRepository>(),
    BackPressListener {
    var userID: Int = 0
    lateinit var userToken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userID = KeyStorage.getInstance(requireContext()).getInt(KEY_USER_ID)
        userToken = KeyStorage.getInstance(requireContext()).getString(KEY_USER_TOKEN)
    }

    override fun getViewModel(): Class<StaticPageViewModel> {
        return StaticPageViewModel::class.java
    }

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTermsAndPrivacyPolicyBinding {
        return FragmentTermsAndPrivacyPolicyBinding.inflate(inflater, container, false)
    }

    override fun getRepository(): SettingRepository {
        return SettingRepository(requireContext())
    }

    override fun registerObservers() {
    }

    override fun unregisterObservers() {
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val staticId = arguments?.getInt("staticId") // Replace "key" with your actual key

        if (staticId == 1){
            viewBinding.tvHeading.text = getString(R.string.privacy_policy)
        }else{
            viewBinding.tvHeading.text = getString(R.string.terms_and_conditions)
        }

        val resModel = StaticPagesResModel(null, null, null)
        viewModel.staticPageInfo(userID, userToken, staticId!!, resModel)
        viewBinding.ivBack.setOnClickListener {
            if(!onBackPressed()) activity?.onBackPressed()
        }
        initObserver()
    }

    private fun initObserver(){
        viewModel.staticPageData.observe(viewLifecycleOwner){ resModel ->
            when(resModel){
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader("Loading...")
                }
                is ViewState.Error -> {
                    hideLoader()
                    showToast(resModel.error)
                }
                is ViewState.Data -> {
                    hideLoader()
                    resModel.data.message?.let {
                        var data = resModel.data.result?.description
                        var lan = KeyStorage.getInstance(requireContext()).getString(KeyStorage.Companion.APP_SELECTED_LANGUAGE)
                        val isEnglish = lan.equals("en", ignoreCase = true)
                        if (!isEnglish) {
                            data = resModel.data.result?.frenchDescription
                        }
                        viewBinding.wvLongDesc.loadDataWithBaseURL(null,data.toHtmlString() , "text/html", "UTF-8", null)
                    }
                }
            }

        }
    }

    override fun onBackPressed(): Boolean {
        return false
    }
}