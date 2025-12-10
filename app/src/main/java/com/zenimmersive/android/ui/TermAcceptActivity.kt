package com.zenimmersive.android.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.zenimmersive.android.R
import com.zenimmersive.android.base.BaseActivity
import com.zenimmersive.android.databinding.ActivityTermAcceptBinding
import com.zenimmersive.android.helper.Constants.ACCEPT_TERMS
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.repository.BlankRepository
import com.zenimmersive.android.viewmodel.BlankViewModel

class TermAcceptActivity :
    BaseActivity<BlankViewModel, ActivityTermAcceptBinding, BlankRepository>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Todo Update client actual support email.
        viewBinding.tvContactUs.text = getString(R.string.you_can_withdraw_your_consent_anytime_by_contacting_us) + "support@support.com"

        viewBinding.viewPart1.tvMessage.setText(spannedTextPart1())
        viewBinding.viewPart1.tvMessage.movementMethod = LinkMovementMethod.getInstance()

        viewBinding.viewPart2.tvMessage.setText(spannedTextPart2())
        viewBinding.viewPart2.tvMessage.movementMethod = LinkMovementMethod.getInstance()

        viewBinding.viewPart3.tvMessage.setText(getString(R.string.i_agree_that_may_use_my_personal_data_to_send_me_product_or_service_offerings_via_app_or_email))

        viewBinding.viewPart1.ivCheck.setTag("FALSE")
        viewBinding.viewPart1.ivCheck.setOnClickListener {
            toggleCheck(viewBinding.viewPart1.ivCheck)
        }

        viewBinding.viewPart2.ivCheck.setTag("FALSE")
        viewBinding.viewPart2.ivCheck.setOnClickListener {
            toggleCheck(viewBinding.viewPart2.ivCheck)
        }

        viewBinding.viewPart3.ivCheck.setTag("FALSE")
        viewBinding.viewPart3.ivCheck.setOnClickListener {
            toggleCheck(viewBinding.viewPart3.ivCheck)
        }


        viewBinding.viewAcceptButton.setOnClickListener {
            allToggleAccept(viewBinding.viewPart1.ivCheck)
            allToggleAccept(viewBinding.viewPart2.ivCheck)
            allToggleAccept(viewBinding.viewPart3.ivCheck)
        }
        viewBinding.viewNextButton.setOnClickListener {
            if (viewBinding.viewPart1.ivCheck.tag == "TRUE" &&  viewBinding.viewPart2.ivCheck.tag == "TRUE" && viewBinding.viewPart3.ivCheck.tag == "TRUE"){
                KeyStorage.getInstance(this@TermAcceptActivity).setBoolean(ACCEPT_TERMS, true)
                startActivity(Intent(getContext(), AuthenticationOptionActivity::class.java))
                finish()
            }else{
                showToast(getString(R.string.error_accept_terms))
            }
        }
    }

    private fun allToggleAccept(view: ImageView){
        try {
            view.setTag("TRUE")
            view.setImageResource(R.drawable.ic_checked)
        } catch (e: Exception) {
            view.setTag("FALSE")
            view.setImageResource(R.drawable.rounded_circle_white)
        }
    }

    private fun toggleCheck(view: ImageView) {
        try {
            if (view.getTag().toString().equals("FALSE")) {
                view.setTag("TRUE")
                view.setImageResource(R.drawable.ic_checked)
            } else {
                view.setTag("FALSE")
                view.setImageResource(R.drawable.rounded_circle_white)
            }
        } catch (e: Exception) {
            view.setTag("FALSE")
            view.setImageResource(R.drawable.rounded_circle_white)
        }
    }

    fun spannedTextPart1(): SpannableString {
        val spannableString = SpannableString("I agree to Privacy Policy and Terms of Use.")

        // Create a ClickableSpan for the Privacy Policy
        val privacyPolicySpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Handle Privacy Policy click
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))
                startActivity(browserIntent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // Underline the text
            }
        }

        // Create a ClickableSpan for the Terms of Use
        val termsOfUseSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Handle Terms of Use click
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))
                startActivity(browserIntent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // Underline the text
            }
        }

        // Apply the spans to the respective parts of the string
        spannableString.setSpan(privacyPolicySpan, 9, 25, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(termsOfUseSpan, 28, 42, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableString
    }

    fun spannedTextPart2(): SpannableString {
        val spannableString =
            SpannableString("I agree to processing of my personal health data for providing me Bellabeat app functions. See more in  Privacy Policy.")

        // Create a ClickableSpan for the Privacy Policy
        val privacyPolicySpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Handle Privacy Policy click
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))
                startActivity(browserIntent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // Underline the text
            }
        }

        // Apply the spans to the respective parts of the string
        spannableString.setSpan(
            privacyPolicySpan,
            103,
            spannableString.length - 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannableString
    }

    override fun getActivityBinding(inflater: LayoutInflater): ActivityTermAcceptBinding {
        return ActivityTermAcceptBinding.inflate(inflater)
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