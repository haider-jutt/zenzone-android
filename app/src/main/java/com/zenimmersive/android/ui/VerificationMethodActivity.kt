package com.zenimmersive.android.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.RelativeLayout
import com.zenimmersive.android.R
import com.zenimmersive.android.base.BaseActivity
import com.zenimmersive.android.databinding.ActivityVerifyMethodBinding
import com.zenimmersive.android.repository.BlankRepository
import com.zenimmersive.android.viewmodel.BlankViewModel

class VerificationMethodActivity :
    BaseActivity<BlankViewModel, ActivityVerifyMethodBinding, BlankRepository>() {
    var isEmailSelected = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding.viewEmailVerificationMethod.setOnClickListener {
            switchMethod(viewBinding.viewEmailVerificationMethod)
        }
        viewBinding.viewPhoneVerificationMethod.setOnClickListener {
            switchMethod(viewBinding.viewPhoneVerificationMethod)
        }

        viewBinding.buttonContinue.setOnClickListener {
            startActivity(Intent(this, VerificationActivity::class.java).also {
                it.putExtra("isEmail", isEmailSelected)
            })
        }

    }

    private fun switchMethod(view: RelativeLayout) {
        if (isEmailSelected && view.id == viewBinding.viewEmailVerificationMethod.id) {
            return
        } else if (!isEmailSelected && view.id == viewBinding.viewPhoneVerificationMethod.id) {
            return
        }

        if (!isEmailSelected) {
            isEmailSelected = true
            viewBinding.viewEmailVerificationMethod.setBackgroundResource(R.drawable.button_shape_primary)
            (viewBinding.viewEmailVerificationMethod.getChildAt(0) as ImageView).setImageResource(R.drawable.email_rounded_primary)

            viewBinding.viewPhoneVerificationMethod.setBackgroundResource(R.drawable.button_shape_secoundry)
            (viewBinding.viewPhoneVerificationMethod.getChildAt(0) as ImageView).setImageResource(R.drawable.phone_rounded)
        } else {
            isEmailSelected = false

            viewBinding.viewPhoneVerificationMethod.setBackgroundResource(R.drawable.button_shape_primary)
            (viewBinding.viewPhoneVerificationMethod.getChildAt(0) as ImageView).setImageResource(R.drawable.phone_rounded_primary)

            viewBinding.viewEmailVerificationMethod.setBackgroundResource(R.drawable.button_shape_secoundry)
            (viewBinding.viewEmailVerificationMethod.getChildAt(0) as ImageView).setImageResource(R.drawable.email_rounded)
        }
    }

    override fun getActivityBinding(inflater: LayoutInflater): ActivityVerifyMethodBinding {
        return ActivityVerifyMethodBinding.inflate(inflater)
    }

    override fun getRepository(): BlankRepository {
        return BlankRepository(this)
    }


    override fun bindViewModel() {
    }

    override fun removeViewModelCallbacks() {
    }

    override fun getViewModel(): Class<BlankViewModel> = BlankViewModel::class.java
}