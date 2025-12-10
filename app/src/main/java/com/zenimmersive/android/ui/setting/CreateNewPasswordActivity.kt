package com.zenimmersive.android.ui.setting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import com.zenimmersive.android.R
import com.zenimmersive.android.base.BaseActivity
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.ActivityCreateNewPasswordBinding
import com.zenimmersive.android.repository.ChangePasswordRepository
import com.zenimmersive.android.ui.AuthenticationOptionActivity
import com.zenimmersive.android.viewmodel.ChangePasswordViewModel

class CreateNewPasswordActivity : BaseActivity<ChangePasswordViewModel, ActivityCreateNewPasswordBinding, ChangePasswordRepository>() {
    private val TAG = CreateNewPasswordActivity::class.java.simpleName
    private var uri: Uri? = null
    private var paramUserToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        uri = intent.data
        if (uri != null) {
            // getting the path segments and storing it in list.
            paramUserToken = uri!!.getQueryParameter("userToken")
            Log.d(TAG, "deep link param: $paramUserToken")
        }

        initObserver()

        viewBinding.tvBtnReset.setOnClickListener {
            val authSuccessful: Boolean = authenticate(
                viewBinding.etNewPassword.text.toString(),
                viewBinding.etNewConfirmPassword.text.toString()
            )

            if (authSuccessful) {
                viewModel.changePassword(paramUserToken ?: "", viewBinding.etNewPassword.text.toString())
            }
        }

        viewBinding.tvBtnOk.setOnClickListener {
            val intent = Intent(this, AuthenticationOptionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun initObserver() {
        viewModel.changePasswordRes.observe(this) { response ->
            when (response) {
                is ViewState.DefaultState -> {}
                is ViewState.Data -> {
                    hideLoader()
                    response.data.message?.let {
                        showToast(it)
                    }
                    // TODO that is for testing ui.
                    viewBinding.viewHeaderPart.visibility = VISIBLE
                    viewBinding.llPasswordForm.visibility = GONE
                    viewBinding.llSuccessPassword.visibility = VISIBLE
                    viewBinding.tvBtnOk.visibility = VISIBLE
                    viewBinding.tvBtnReset.visibility = GONE
//                    finish()
                }
                is ViewState.Loading -> {
                    showLoader(getString(R.string.loading))
                }
                is ViewState.Error -> {
                    hideLoader()
                    showToast(response.error)
                }
            }
        }
    }

    override fun getViewModel(): Class<ChangePasswordViewModel> {
        return ChangePasswordViewModel::class.java
    }

    override fun getActivityBinding(inflater: LayoutInflater): ActivityCreateNewPasswordBinding {
        return ActivityCreateNewPasswordBinding.inflate(inflater)
    }

    override fun getRepository(): ChangePasswordRepository {
        return ChangePasswordRepository(this)
    }

    override fun bindViewModel() {}

    override fun removeViewModelCallbacks() {}

    fun authenticate(
        newPassword: String,
        confirmNewPassword: String,
    ): Boolean {
        var check = false
        if (newPassword.isEmpty() || newPassword.length < 5) {
            showToast(getString(R.string.please_enter_new_password_at_lease_6_character))
            check = false
        } else if (confirmNewPassword.isEmpty() || newPassword != confirmNewPassword) {
            showToast(getString(R.string.please_enter_same_new_password_and_confirm_new_password))
            check = false
        } else {
            check = true
        }
        return check
    }
}