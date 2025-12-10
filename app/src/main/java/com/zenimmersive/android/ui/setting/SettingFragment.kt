package com.zenimmersive.android.ui.setting

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.zenimmersive.android.R
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.base.BaseResponse
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.FragmentSettingBinding
import com.zenimmersive.android.helper.CommonUtils
import com.zenimmersive.android.helper.CommonUtils.customPositiveNegativeDialog
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_ID
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_TOKEN
import com.zenimmersive.android.repository.SettingRepository
import com.zenimmersive.android.ui.AuthenticationOptionActivity
import com.zenimmersive.android.ui.BackPressListener
import com.zenimmersive.android.ui.DashboardActivity
import com.zenimmersive.android.ui.HueManagementActivity
import com.zenimmersive.android.viewmodel.SettingViewModel

class SettingFragment : BaseFragment<SettingViewModel, FragmentSettingBinding, SettingRepository>(), BackPressListener {
    var userID: Int = 0
    lateinit var userToken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userID = KeyStorage.getInstance(requireContext()).getInt(KEY_USER_ID)
        userToken = KeyStorage.getInstance(requireContext()).getString(KEY_USER_TOKEN)
    }

    override fun getViewModel(): Class<SettingViewModel> {
        return SettingViewModel::class.java
    }

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingBinding {
        return FragmentSettingBinding.inflate(inflater, container, false)
    }

    @OptIn(UnstableApi::class) override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is DashboardActivity) {
            (activity as DashboardActivity).viewBinding.tvPageTitle.text = ""
        }

        initView()
        initObserver()
    }

    @OptIn(UnstableApi::class) private fun initView() {
        viewBinding.rlMyAccount.setOnClickListener {
            if(activity is DashboardActivity){
                val editProfileFragment = EditProfileFragment()
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.viewDashboardFragmentContainer, editProfileFragment)
                    ?.addToBackStack("EditProfileFragment")
                    ?.commit()
            }
        }

        viewBinding.rlMyOrders.setOnClickListener {
            startActivity(Intent(requireContext(), MyOrdersActivity::class.java))
        }

        viewBinding.rlFaq.setOnClickListener {
            if(activity is DashboardActivity){
                val faqFragment = FAQFragment()
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.viewDashboardFragmentContainer, faqFragment)
                    ?.addToBackStack("FAQFragment")
                    ?.commit()
            }
        }

        viewBinding.rlPrivacyPolicy.setOnClickListener {
            if(activity is DashboardActivity){
                val termsPrivacyFragment = TermsAndPrivacyPolicyFragment()
                val bundle = Bundle()
                bundle.putInt("staticId", 1)
                termsPrivacyFragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.viewDashboardFragmentContainer, termsPrivacyFragment)
                    ?.addToBackStack("TermsAndPrivacyPolicyFragment")
                    ?.commit()
            }
        }

        viewBinding.rlTermsConditions.setOnClickListener {
            if(activity is DashboardActivity){
                val termsPrivacyFragment = TermsAndPrivacyPolicyFragment()
                val bundle = Bundle()
                bundle.putInt("staticId", 2)
                termsPrivacyFragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.viewDashboardFragmentContainer, termsPrivacyFragment)
                    ?.addToBackStack("TermsAndPrivacyPolicyFragment")
                    ?.commit()
            }
        }

        viewBinding.rlLightsManagement.setOnClickListener {
            startActivity(Intent(requireContext(), HueManagementActivity::class.java))
        }

        viewBinding.tvLogout.setOnClickListener {
            customPositiveNegativeDialog(requireContext(), getString(R.string.log_out), getString(R.string.logout_message), getString(R.string.log_out), "Cancel", object : CommonUtils.CustomDialogCallback{
                override fun positiveBtnClick(dialog: Dialog) {
                    dialog.dismiss()
                    val resModel = BaseResponse()
                    viewModel.logout(userID, userToken, resModel)
                }

                override fun cancelBtnClick(dialog: Dialog) {
                    dialog.dismiss()
                }

            })
        }

        viewBinding.tvDeleteAccount.setOnClickListener {
            customPositiveNegativeDialog(requireContext(), getString(R.string.delete_account),
                getString(R.string.delete_account_message), getString(R.string.ok), getString(R.string.cancel), object : CommonUtils.CustomDialogCallback{
                override fun positiveBtnClick(dialog: Dialog) {
                    dialog.dismiss()
                    viewModel.deleteAccount()
                }

                override fun cancelBtnClick(dialog: Dialog) {
                    dialog.dismiss()
                }

            })
        }
    }

    override fun getRepository(): SettingRepository {
        return SettingRepository(requireContext())
    }

    override fun registerObservers() {

    }

    override fun unregisterObservers() {

    }

    override fun onBackPressed(): Boolean {
        return false
    }

    private fun initObserver(){
        viewModel.logoutData.observe(viewLifecycleOwner){ resModel ->
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
                    showToast(resModel.data.message ?: "")
                    KeyStorage.getInstance(requireContext()).clearAllData()
                    val intent = Intent(requireContext(), AuthenticationOptionActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }

        }

        viewModel.deleteAccountData.observe(viewLifecycleOwner){ resModel ->
            when(resModel){
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader(getString(R.string.loading))
                }
                is ViewState.Error -> {
                    hideLoader()
                    showToast(resModel.error)
                }
                is ViewState.Data -> {
                    hideLoader()
                    showToast(resModel.data.message ?: "")
                    KeyStorage.getInstance(requireContext()).clearAllData()
                    val intent = Intent(requireContext(), AuthenticationOptionActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }

        }
    }
}