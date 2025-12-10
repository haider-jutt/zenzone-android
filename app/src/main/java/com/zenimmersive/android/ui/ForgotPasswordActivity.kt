package com.zenimmersive.android.ui

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.UserBasicResModel
import com.zenimmersive.android.base.BaseActivity
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.ActivityForgotPasswordBinding
import com.zenimmersive.android.repository.ForgotPasswordRepository
import com.zenimmersive.android.viewmodel.ForgotPasswordViewModel

class ForgotPasswordActivity :
    BaseActivity<ForgotPasswordViewModel, ActivityForgotPasswordBinding, ForgotPasswordRepository>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding.tvBtnSend.setOnClickListener {
            if (!viewBinding.etEmail.text.toString().isEmpty() || Patterns.EMAIL_ADDRESS.matcher(viewBinding.etEmail.text.toString()).matches()) {
                val resModel = UserBasicResModel(null, null, null)
                viewModel.forgotPassword(viewBinding.etEmail.text.toString(), resModel)
            }else{
                showToast(getString(R.string.error_invalid_email))
            }
        }

        initObserver()
    }

    override fun getActivityBinding(inflater: LayoutInflater): ActivityForgotPasswordBinding {
        return ActivityForgotPasswordBinding.inflate(inflater)
    }

    override fun getRepository(): ForgotPasswordRepository {
        return ForgotPasswordRepository(this)
    }


    override fun bindViewModel() {
    }

    override fun removeViewModelCallbacks() {
    }

    override fun getViewModel(): Class<ForgotPasswordViewModel> = ForgotPasswordViewModel::class.java

    fun initObserver(){
        viewModel.forgotPasswordData.observe(this) { resData ->
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
                    finish()
                }
                is ViewState.Error -> {
                    hideLoader()
                    showToast(resData.error)
                }
            }

        }
    }
}