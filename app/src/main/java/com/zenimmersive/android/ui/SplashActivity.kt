package com.zenimmersive.android.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.QuoteMessageResModel
import com.zenimmersive.android.base.BaseActivity
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.ActivitySplashBinding
import com.zenimmersive.android.helper.Constants.ACCEPT_TERMS
import com.zenimmersive.android.helper.Constants.NOTIFICATION_ALBUM_ID
import com.zenimmersive.android.helper.Constants.NOTIFICATION_SONG_ID
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_LAST_QUOTE_MESSAGE_EN
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_LAST_QUOTE_MESSAGE_FR
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_TOKEN
import com.zenimmersive.android.repository.SplashRepository
import com.zenimmersive.android.ui.payment.PurchaseHelper
import com.zenimmersive.android.viewmodel.SplashScreenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject


class SplashActivity :
    BaseActivity<SplashScreenViewModel, ActivitySplashBinding, SplashRepository>() {

    val TAG = "SplashActivity"
    lateinit var biometricManager: BiometricManager
    private var cancellationSignal: CancellationSignal? = null
    lateinit var authenticationCallback: BiometricPrompt.AuthenticationCallback
    var lastQuoteMessage: String? = null

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val notificationData = intent.extras
        convertBundleToJsonExample(notificationData)
        Log.d(TAG, "splash screen intent ${notificationData}: ")
        Log.d(TAG, "notification data: ${notificationData?.containsKey("type")}")

        if (notificationData != null) {
            if (notificationData.containsKey("type")) {
                val dataJsonString = notificationData.getString("type")
                Log.d(TAG, "notification data $dataJsonString")

                if (dataJsonString.toString().lowercase() == "album") {
                    val bundle = Bundle()
                    bundle.putString(NOTIFICATION_ALBUM_ID, notificationData.getString("albumId"))
                    val i = Intent(this@SplashActivity, DashboardActivity::class.java)
                    i.putExtras(bundle)
                    startActivity(i)
                    finish()
                } else if (dataJsonString.toString().lowercase() == "song") {
                    val bundle = Bundle()
                    bundle.putString(NOTIFICATION_SONG_ID, notificationData.getString("songId"))
                    val i = Intent(this@SplashActivity, DashboardActivity::class.java)
                    i.putExtras(bundle)
                    startActivity(i)
                    finish()
                }
            }
        }

        if (KeyStorage.getInstance(this@SplashActivity).getString(APP_SELECTED_LANGUAGE)
                ?.equals("fr") == true
        ) lastQuoteMessage =
            KeyStorage.getInstance(this@SplashActivity).getString(KEY_LAST_QUOTE_MESSAGE_FR)
        else lastQuoteMessage = KeyStorage.getInstance(this@SplashActivity)
            .getString(KEY_LAST_QUOTE_MESSAGE_EN)
        initObserver()

        viewBinding.ivBiometric.setOnClickListener {
            initView()
        }

    }

    private fun animateView() {
        viewBinding.root.postDelayed({
            viewBinding.root.postDelayed({
                viewBinding.viewQuoteContent.post {
                    viewBinding.viewQuoteContent.animate().alpha(1f).start()
                }
            }, 600)
//            viewBinding.ivLogo.animate().alpha(0f).setDuration(600).start()
            viewBinding.imageView.animate().alpha(1f).setDuration(600).start()
            viewBinding.imageView2.animate().alpha(0f).setDuration(600).start()
            val zoomOut: Animation = AnimationUtils.loadAnimation(
                applicationContext,
                R.anim.animate_zoom_out
            )
            // Convert 30dp to pixels for top margin
            val marginTopInPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                10f,
                viewBinding.ivLogo.resources.displayMetrics
            )

            // Animate translationY (move up)
            val moveUpAnimator = ObjectAnimator.ofFloat(
                viewBinding.ivLogo,
                "y",
                viewBinding.ivLogo.y,
                marginTopInPx
            )

            // Animate scaleX and scaleY (zoom out)
            val scaleX = ObjectAnimator.ofFloat(viewBinding.ivLogo, "scaleX", 1f, 0.5f)
            val scaleY = ObjectAnimator.ofFloat(viewBinding.ivLogo, "scaleY", 1f, 0.5f)

            // Play animations together
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(moveUpAnimator, scaleX, scaleY)
            animatorSet.duration = 600
            animatorSet.interpolator = AccelerateDecelerateInterpolator()
            animatorSet.start()
        }, 1000)
    }

    override fun getViewModel(): Class<SplashScreenViewModel> = SplashScreenViewModel::class.java

    override fun getActivityBinding(inflater: LayoutInflater): ActivitySplashBinding =
        ActivitySplashBinding.inflate(inflater)

    override fun getRepository(): SplashRepository = SplashRepository(this)

    override fun bindViewModel() {

    }

    override fun removeViewModelCallbacks() {
        val observable: MutableLiveData<ViewState<QuoteMessageResModel>> = viewModel.getQuoteResData
        if (observable != null && observable.hasObservers()) {
            Log.v("removeObserver", "Removing Observers")
            observable.removeObservers(this)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getQuoteMessage()

        /*viewBinding.root.postDelayed({
            postNavigationMainPage()
        }, 5000)*/
    }

    private fun navigateNextScreen() {
        if (!KeyStorage.getInstance(this@SplashActivity).getString(KEY_USER_TOKEN)
                .isNullOrBlank()
        ) {
            CoroutineScope(Dispatchers.Main).launch {
                delay(5000)
//                initView() // Biometric login
                triggerSubscriptionSync {
                    navigateToDashboard()
                }
            }
        } else {

            if (KeyStorage.getInstance(this@SplashActivity).getBoolean(ACCEPT_TERMS)) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(5000)
                    startActivity(
                        Intent(
                            this@SplashActivity,
                            AuthenticationOptionActivity::class.java
                        )
                    )
                    finish()
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(5000)
                    startActivity(Intent(this@SplashActivity, TermAcceptActivity::class.java))
                    finish()
                }
            }
        }
    }

    fun initObserver() {
        viewModel.getQuoteResData.observe(this@SplashActivity) { resData ->
            when (resData) {
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    viewBinding.progressBar.visibility = VISIBLE
                }

                is ViewState.Data -> {
                    viewBinding.progressBar.visibility = GONE
                    if (resData.data.status == 1) {
                        if (resData.data.result != null) {
                            var quote = resData.data.getQuote(
                                KeyStorage.getInstance(this@SplashActivity)
                                    .getString(APP_SELECTED_LANGUAGE)
                            )
                            viewBinding.tvQuoteMessage.text = quote
                            KeyStorage.getInstance(this@SplashActivity).setString(
                                KEY_LAST_QUOTE_MESSAGE_FR,
                                resData.data.result?.fquote ?: ""
                            )
                            KeyStorage.getInstance(this@SplashActivity).setString(
                                KEY_LAST_QUOTE_MESSAGE_EN,
                                resData.data.result?.quote ?: ""
                            )
                        }
                    } else {
                        if (!lastQuoteMessage.isNullOrBlank()) {
                            viewBinding.tvQuoteMessage.text = lastQuoteMessage
                        }
                    }
                    animateView()
                    navigateNextScreen()
                }

                is ViewState.Error -> {
                    viewBinding.progressBar.visibility = GONE
                    if (!lastQuoteMessage.isNullOrBlank()) {
                        viewBinding.tvQuoteMessage.text = lastQuoteMessage
                    }
                    animateView()
                    navigateNextScreen()
                }
            }
        }
    }

    fun convertBundleToJsonExample(bundle: Bundle?) {
        if (bundle != null) {
            val jsonString = bundleToJson(bundle)
            Log.d(TAG, "convertBundleToJson data: $jsonString")
        } else {
            Log.d(TAG, "convert bundle is null")
        }
    }

    fun bundleToJson(bundle: Bundle): String {
        val jsonObject = JSONObject()
        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            try {
                jsonObject.put(key, value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return jsonObject.toString()
    }


    private fun initView() {
        authenticationCallback = @RequiresApi(Build.VERSION_CODES.P)
        object : BiometricPrompt.AuthenticationCallback() {
            // here we need to implement two methods
            // onAuthenticationError and onAuthenticationSucceeded
            // If the fingerprint is not recognized by the app it will call
            // onAuthenticationError and show a toast
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                super.onAuthenticationError(errorCode, errString)

                if (errorCode == 11) {
                    showToast(getString(R.string.error_biometric_not_found))
                }
            }

            // If the fingerprint is recognized by the app then it will call
            // onAuthenticationSucceeded and show a toast that Authentication has Succeed
            // Here you can also start a new activity after that
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)
                showToast(getString(R.string.success_authentication_succeeded))
                navigateToDashboard()
            }
        }
        checkBiometricSupport()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // This creates a dialog of biometric auth and
            // it requires title , subtitle ,
            // and description
            // In our case there is a cancel button by
            // clicking it, it will cancel the process of
            // fingerprint authentication
            val biometricPrompt = BiometricPrompt.Builder(this)
                .setTitle(getString(R.string.app_name))
                .setConfirmationRequired(true)
                .setDeviceCredentialAllowed(true)

                .build()


            // start the authenticationCallback in mainExecutor
            biometricPrompt.authenticate(
                getCancellationSignal(),
                mainExecutor,
                authenticationCallback
            )
        }
    }

    // it will be called when authentication is cancelled by the user
    private fun getCancellationSignal(): CancellationSignal {
        cancellationSignal = CancellationSignal()
        cancellationSignal?.setOnCancelListener {
            showToast(getString(R.string.info_authentication_cancelled))
        }
        return cancellationSignal as CancellationSignal
    }

    // it checks whether the app the app has fingerprint permission
    private fun checkBiometricSupport() {
        val biometricManager = BiometricManager.from(this@SplashActivity)
        when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showToast(getString(R.string.info_fingerprint_available))
                viewBinding.ivBiometric.visibility = VISIBLE
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                showToast(getString(R.string.error_no_fingerprint_sensor))
                viewBinding.ivBiometric.visibility = GONE
                navigateToDashboard()
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                showToast(getString(R.string.error_biometric_unavailable))
                viewBinding.ivBiometric.visibility = GONE
                navigateToDashboard()
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                showToast(getString(R.string.error_biometric_unavailable))
                viewBinding.ivBiometric.visibility = GONE
                navigateToDashboard()
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun navigateToDashboard() {
        startActivity(Intent(this@SplashActivity, DashboardActivity::class.java))
        finish()
    }

    var isMainPageLoaded = false

    @SuppressLint("UnsafeOptInUsageError")
    private fun postNavigationMainPage() {
        if (isMainPageLoaded) {
            return
        }
        isMainPageLoaded = true

        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun triggerSubscriptionSync(onComplete: () -> Unit) {
        val keyStorage = KeyStorage.getInstance(this)
        val hasSubscription = keyStorage.getEffectiveUserSubscriptionType() > 1
        if (!hasSubscription) {
            onComplete()
            return
        }
        viewModel.fetchUserProfile().observe(this, { result->
            onComplete()
        })
    }
}