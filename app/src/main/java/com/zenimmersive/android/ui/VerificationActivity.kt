package com.zenimmersive.android.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.UserBasicResModel
import com.zenimmersive.android.base.BaseActivity
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.ActivityVerifyPhoneBinding
import com.zenimmersive.android.helper.CommonUtils.toJson
import com.zenimmersive.android.helper.Constants.USER_DATA
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_ID
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_TOKEN
import com.zenimmersive.android.helper.hide
import com.zenimmersive.android.repository.VerificationRepository
import com.zenimmersive.android.viewmodel.VerificationViewModel

class VerificationActivity :
    BaseActivity<VerificationViewModel, ActivityVerifyPhoneBinding, VerificationRepository>() {
    var isOtpVisible = false
    var userId: Int? = null
    var userToken: String? = null
    var email: String? = null

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        var isEmail = intent.getBooleanExtra("isEmail", false)
        if (intent.hasExtra("userId")){
            userId = intent.getIntExtra("userId",0)
        }
        if (intent.hasExtra("userToken")){
            userToken = intent.getStringExtra("userToken")
        }
        if (intent.hasExtra("email")){
            email = intent.getStringExtra("email")
        }

        var isEmail = intent.getBooleanExtra("isEmail", false)

        if (true) {
            viewBinding.viewPhoneInputArea.hide()
            viewBinding.viewEmailInputArea.hide()
            viewBinding.tvRegisterType.text = getString(R.string.email_address)
            viewBinding.tvLoginTypeDesc.text =
                getString(R.string.we_re_going_to_send_you_an_email_with_a_login_link)
        } else {
            viewBinding.viewEmailInputArea.hide()
            viewBinding.tvRegisterType.text = getString(R.string.phone_number)
            viewBinding.tvLoginTypeDesc.text = getString(R.string.we_will_sent_code_message)
        }

        viewBinding.buttonContinue.setOnClickListener {
            if (viewBinding.etOtpView.text.toString().length == 4) {
                val resModel = UserBasicResModel(null, null, null)
                viewModel.emailOtpVerify(userId ?: 0, userToken ?: "", viewBinding.etOtpView.text.toString(), resModel)
            }else{
                showToast(getString(R.string.please_enter_full_otp_code))
            }
        }

        initObserver()
    }

    override fun getActivityBinding(inflater: LayoutInflater): ActivityVerifyPhoneBinding {
        return ActivityVerifyPhoneBinding.inflate(inflater)
    }

    override fun getRepository(): VerificationRepository {
        return VerificationRepository(this)
    }

    override fun bindViewModel() {
    }

    override fun removeViewModelCallbacks() {
    }

    override fun getViewModel(): Class<VerificationViewModel> = VerificationViewModel::class.java

    fun initObserver(){
        viewModel.otpVerifyData.observe(this) { resData ->
            when(resData){
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader("Loading...")
                }
                is ViewState.Data -> {
                    hideLoader()
                    resData.data.message?.let {
                        showToast(resData.data.message!!)
                    }
                    if (resData.data.status == 1) {
                        val userJson = resData.toJson()
                        KeyStorage.getInstance(this@VerificationActivity)
                            .setInt(KEY_USER_ID, resData.data.result?.userId)
                        KeyStorage.getInstance(this@VerificationActivity)
                            .setString(KEY_USER_TOKEN, resData.data.result?.userToken)
                        KeyStorage.getInstance(this@VerificationActivity).setString(USER_DATA, userJson)
                        val intent = Intent(this, DashboardActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                }
                is ViewState.Error -> {
                    hideLoader()
                    showToast(resData.error)
                }
            }

        }
    }
}