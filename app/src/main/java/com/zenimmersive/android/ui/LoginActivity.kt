package com.zenimmersive.android.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.UserBasicResModel
import com.zenimmersive.android.base.BaseActivity
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.ActivityLoginBinding
import com.zenimmersive.android.helper.CommonUtils.toJson
import com.zenimmersive.android.helper.Constants.USER_DATA
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_ID
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_TOKEN
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

class LoginActivity :
    BaseActivity<LoginViewModel, ActivityLoginBinding, LoginRepository>() {

    lateinit var mGoogleSignInClient: GoogleSignInClient
    val Req_Code: Int = 123
    private lateinit var firebaseAuth: FirebaseAuth
    private var passwordTextVisibility: Boolean = false


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        spannedTextPart(viewBinding.viewNotMemberText)

        FirebaseApp.initializeApp(this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = FirebaseAuth.getInstance()

        initObserver()

        viewBinding.etPassword.setOnTouchListener { _, event ->
            val drawableRight = viewBinding.etPassword.compoundDrawablesRelative[2] // Index 2 represents the right drawable
            if (event.action == MotionEvent.ACTION_UP && event.rawX >= viewBinding.etPassword.right - drawableRight.bounds.width()) {

                viewBinding.etPassword.requestFocus()

                // Handle the click event on the right drawable
                // Perform your desired action here
                if (passwordTextVisibility) {
                    passwordTextVisibility = false
                    viewBinding.etPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null,
                        null,
                        ContextCompat.getDrawable(this@LoginActivity, R.drawable.ic_eye_block),
                        null
                    )
                    viewBinding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                } else {
                    passwordTextVisibility = true
                    viewBinding.etPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null,
                        null,
                        ContextCompat.getDrawable(this@LoginActivity, R.drawable.ic_eye_open),
                        null
                    )
                    viewBinding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                }
                true
            } else {
                false
            }
        }

        viewBinding.buttonLogin.setOnClickListener {
            val authSuccessful: Boolean = authenticate(
                viewBinding.etEmail.text.toString(),
                viewBinding.etPassword.text.toString(),
            )
            if (authSuccessful) {
                val regModel = UserBasicResModel(null, null, null)
                viewModel.userLogin(
                    viewBinding.etEmail.text.toString(),
                    viewBinding.etPassword.text.toString(),
                    regModel
                )
            }
        }

        viewBinding.viewGoogleLoginParent.setOnClickListener {
            mGoogleSignInClient.signOut()
            signInGoogle()
        }

        viewBinding.viewNotMemberText.setOnClickListener {
            startActivity(Intent(getContext(), RegisterActivity::class.java))
        }

        viewBinding.viewForgotPassword.setOnClickListener {
            startActivity(Intent(getContext(), ForgotPasswordActivity::class.java))
//            startActivity(Intent(getContext(), CreateNewPasswordActivity::class.java))
        }
    }

    fun spannedTextPart(view: TextView) {
        var message = getString(R.string.don_t_have_an_account_sign_up)
        val spannableString =
            SpannableString(message)

        // Create a ClickableSpan for the Privacy Policy
        val privacyPolicySpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
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
        view.setText(spannableString)
        view.movementMethod = LinkMovementMethod.getInstance()
    }

    fun authenticate(
        email: String,
        password: String,
    ): Boolean {
        var check = false
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast(getString(R.string.error_invalid_email))
            check = false

        } else if (password.isEmpty() || password.length < 5) {
            showToast(getString(R.string.error_password_min_length))
            check = false

        } else {
            check = true
        }
        return check
    }

    override fun getActivityBinding(inflater: LayoutInflater): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(inflater)
    }

    override fun getRepository(): LoginRepository {
        return LoginRepository(this)
    }


    override fun bindViewModel() {
    }

    override fun removeViewModelCallbacks() {
    }

    override fun getViewModel(): Class<LoginViewModel> = LoginViewModel::class.java

    fun initObserver(){
        viewModel.userLoginData.observe(this) { resData ->
            when(resData){
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader("Loading...")
                }
                is ViewState.Data -> {
                    hideLoader()

                    if (resData.data.status == 1){
                        val userJson = resData.toJson()
                        KeyStorage.getInstance(this@LoginActivity).setInt(KEY_USER_ID, resData.data.result?.userId)
                        KeyStorage.getInstance(this@LoginActivity).setString(KEY_USER_TOKEN, resData.data.result?.userToken)
                        KeyStorage.getInstance(this@LoginActivity).setString(USER_DATA, userJson)
                        val intent = Intent(this, DashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }else{
                        if (resData.data.message == "Please Verify Otp"){
                            val intent = Intent(this, VerificationActivity::class.java).also {
                                it.putExtra("isEmail", true)
                                it.putExtra("userId", resData.data?.result?.userId)
                                it.putExtra("userToken", resData.data?.result?.userToken)
                                it.putExtra("email", resData.data?.result?.email)
                            }
                            startActivity(intent)
                            return@observe
                        }
                        showToast(resData.data.message ?: "")
                    }

                }
                is ViewState.Error -> {
                    hideLoader()
                    showToast(resData.error)
                    if (resData.data != null){
                        if(resData.data.result?.isVerify == 0){
                            val intent = Intent(this, VerificationActivity::class.java).also {
                                it.putExtra("isEmail", true)
                                it.putExtra("userId", resData.data.result?.userId)
                                it.putExtra("userToken", resData.data.result?.userToken)
                                it.putExtra("email", resData.data.result?.email)
                            }
                            startActivity(intent)
                        }
                    }
                }
            }
        }

        viewModel.userSocialLoginData.observe(this) { resData ->
            when(resData){
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader("Loading...")
                }
                is ViewState.Data -> {
                    hideLoader()

                    if (resData.data.status == 1) {
                        val userJson = resData.toJson()
                        KeyStorage.getInstance(this@LoginActivity)
                            .setInt(KEY_USER_ID, resData.data.result?.userId)
                        KeyStorage.getInstance(this@LoginActivity)
                            .setString(KEY_USER_TOKEN, resData.data.result?.userToken)
                        KeyStorage.getInstance(this@LoginActivity).setString(USER_DATA, userJson)
                        val intent = Intent(this, DashboardActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }else{
                        showToast(resData.data.message ?: "")
                    }
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
            hideLoader()
            showToast(getString(R.string.error_something_went_wrong,e.message))
        }
    }

    private fun UpdateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val regModel = UserBasicResModel(null, null, null)
                viewModel.userSocialLogin(account.email?:"", "Google", account.idToken?:"", account.id?:"", account.displayName?:"", regModel)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Req_Code) {
            if(resultCode == RESULT_OK) {
                val task: Task<GoogleSignInAccount> =
                    GoogleSignIn.getSignedInAccountFromIntent(data)
                handleResult(task)
            }
            else showToast(getString(R.string.login_cancelled))
        }
    }
}