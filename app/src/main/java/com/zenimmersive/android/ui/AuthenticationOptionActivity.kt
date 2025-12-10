package com.zenimmersive.android.ui

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.UserBasicResModel
import com.zenimmersive.android.base.BaseActivity
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.ActivityAuthenticationOptionBinding
import com.zenimmersive.android.helper.CommonUtils.toJson
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.repository.LoginRepository
import com.zenimmersive.android.viewmodel.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthenticationOptionActivity :
    BaseActivity<LoginViewModel, ActivityAuthenticationOptionBinding, LoginRepository>() {

    lateinit var mGoogleSignInClient: GoogleSignInClient
    val Req_Code: Int = 123
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = FirebaseAuth.getInstance()

        initObserver()

        viewBinding.viewGoogleLoginParent.setOnClickListener {
            mGoogleSignInClient.signOut()
            signInGoogle()
        }

        spannedTextPart(viewBinding.viewNotMemberText)
        viewBinding.viewNotMemberText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        viewBinding.viewEmailLoginParent.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    fun spannedTextPart(viewNotMemberText: TextView) {
        var message = getString(R.string.not_a_member_sign_up)
        val spannableString =
            SpannableString(message)

        // Create a ClickableSpan for the Privacy Policy
        val privacyPolicySpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Handle Privacy Policy click
                startActivity(Intent(getContext(), RegisterActivity::class.java))
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // Underline the text
            }
        }

        // Apply the spans to the respective parts of the string
        spannableString.setSpan(
            privacyPolicySpan,
            message.indexOf(message.split("?").last()),
            spannableString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        viewNotMemberText.text = spannableString
        viewNotMemberText.movementMethod = LinkMovementMethod.getInstance()
    }

    fun initObserver(){

        viewModel.userSocialLoginData.observe(this) { resData ->
            when(resData){
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader("Loading...")
                }
                is ViewState.Data -> {
                    hideLoader()

                    val userJson = resData.toJson()
                    KeyStorage.getInstance(this@AuthenticationOptionActivity).setInt(
                        KeyStorage.KEY_USER_ID, resData.data.result?.userId)
                    KeyStorage.getInstance(this@AuthenticationOptionActivity).setString(
                        KeyStorage.KEY_USER_TOKEN, resData.data.result?.userToken)
                    KeyStorage.getInstance(this@AuthenticationOptionActivity).setString(
                        _root_ide_package_.com.zenimmersive.android.helper.Constants.USER_DATA, userJson)
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
                is ViewState.Error -> {
                    hideLoader()
                    showToast(resData.error)
                }
            }
        }
    }

    private fun signInGoogle() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, Req_Code)
    }

    private fun handleResult(completedTask: Task<GoogleSignInAccount>) {
        showLoader("Loading...")
        try {
            val account: GoogleSignInAccount? = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                UpdateUI(account)
            }
        } catch (e: ApiException) {
            e.printStackTrace()
            hideLoader()
            showToast(e.toString())
        }
    }

    private fun UpdateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val regModel = UserBasicResModel(null, null, null)
                viewModel.userSocialLogin(account.email?:"", "Google", account.idToken?:"", account.id?:"", account.displayName?:"", regModel)
            }
            else {
                task.exception?.printStackTrace()
                hideLoader()
                showToast(task.exception?.message ?: getString(R.string.error_something_went_wrong, "158"))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Req_Code) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleResult(task)
        }
    }

    override fun getActivityBinding(inflater: LayoutInflater): ActivityAuthenticationOptionBinding {
        return ActivityAuthenticationOptionBinding.inflate(inflater)
    }

    override fun getRepository(): LoginRepository {
        return LoginRepository(this)
    }


    override fun bindViewModel() {
    }

    override fun removeViewModelCallbacks() {
    }

    override fun getViewModel(): Class<LoginViewModel> = LoginViewModel::class.java
}