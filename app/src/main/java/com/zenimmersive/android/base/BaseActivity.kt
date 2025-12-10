package com.zenimmersive.android.base

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.zenimmersive.android.helper.CommonUtils
import com.zenimmersive.android.helper.DialogView
import com.zenimmersive.android.helper.KeyboardUtil
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.helper.ProgressDialog
import com.zenimmersive.android.ui.player.LocalVideoPlayerPropertyManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ViewModel
 * ViewBinding
 * Base Repository
 * */
open abstract class BaseActivity<VM : ViewModel, B : ViewBinding, R : BaseRepository> :
    AppCompatActivity() {

    var viewLoader: FrameLayout? = null
    var tvLoaderMessage: TextView? = null

    lateinit var viewBinding: B
    lateinit var viewModel: VM
    protected val remoteDataSource: RemoteDataSource = RemoteDataSource()

    var progressDialog: ProgressDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CommonUtils.setLocale(this)
        viewBinding = getActivityBinding(layoutInflater)
        setContentView(viewBinding.root)

        progressDialog = ProgressDialog(this)

        val factory = ViewModelFactory(getRepository())
        viewModel = ViewModelProvider(this@BaseActivity, factory).get(getViewModel())

        KeyboardUtil.setupUI(viewBinding.root)
        bindViewModel()

        applyActivityTransitionAnimation()


        findViewById<View?>(com.zenimmersive.android.R.id.ivBack)?.setOnClickListener { onBackPressed() }
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        view?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }

    fun enterFullscreen() {

        window?.decorView?.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

        window.statusBarColor = Color.TRANSPARENT
    }
    fun exitFullscreen() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }


    abstract fun getViewModel(): Class<VM>
    abstract fun getActivityBinding(inflater: LayoutInflater): B
    abstract fun getRepository(): R

    override fun onResume() {
        super.onResume()
        enterFullscreen()
    }

    override fun onDestroy() {
        removeViewModelCallbacks()
        clearDialogs()
        super.onDestroy()
    }

    override fun finish() {
        clearDialogs()
        super.finish()
    }

    fun showLoader() {
        viewLoader?.let {
            viewLoader?.visibility = View.VISIBLE
            tvLoaderMessage?.text = ""
        } ?: run {
            progressDialog?.setMessage("")
            progressDialog?.show()
        }

    }

    fun showLoader(message: String) {
        viewLoader?.let {
            viewLoader?.visibility = View.VISIBLE
            tvLoaderMessage?.text = message
        } ?: run {
            progressDialog?.setMessage(message)
            progressDialog?.show()
        }

    }

    fun hideLoader() {
        viewLoader?.post { viewLoader?.visibility = View.GONE }
        progressDialog?.dismiss()
    }

    fun onFail(message: String) {
        showToast(message)
    }

    fun getContext(): Context = this@BaseActivity

    fun showToast(msg: String) {
        runOnUiThread { Toast.makeText(this@BaseActivity, msg, Toast.LENGTH_LONG).show() }
    }

    abstract fun bindViewModel()
    abstract fun removeViewModelCallbacks()


    protected fun toPx(dip: Number): Float {

        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX, dip.toFloat() * 1f, resources.displayMetrics
        )
    }

    private fun applyActivityTransitionAnimation() {
        overridePendingTransition(
            com.zenimmersive.android.R.anim.animate_swipe_left_enter,
            com.zenimmersive.android.R.anim.animate_swipe_left_exit
        )
    }


    private fun applyActivityTransitionAnimation2() {
        overridePendingTransition(
            com.zenimmersive.android.R.anim.animate_swipe_right_enter,
            com.zenimmersive.android.R.anim.animate_swipe_right_exit
        )
    }

    override fun onBackPressed() {
        super.onBackPressed()
        applyActivityTransitionAnimation2()
    }

    private var dialogStack = ArrayList<DialogView>()
    private fun clearDialogs() {
        if (dialogStack.isNotEmpty()) {
            dialogStack.forEach {
                it.onDestroyView()
            }
        }
        dialogStack.clear()
    }

    open fun showDialog(
        title: String = "METStech",
        message: String = "Empty!",
        positiveButton: String = "OK",
        negativeButton: String = "Dismiss",
        listener: DialogView.ButtonListener = object : DialogView.ButtonListener {
            override fun onPositiveButtonClick(dialog: AlertDialog) {
                dialog.dismiss()
            }

            override fun onNegativeButtonClick(dialog: AlertDialog) {
                dialog.dismiss()
            }

        }
    ) {
        CommonUtils.writeEvent(
            "${javaClass.simpleName} showDialog() called with: title = $title, message = $message, positiveButton = $positiveButton, negativeButton = $negativeButton, listener = $listener"
        )
        if(isNotSafe()) return
        var dialogView = DialogView(this)
        dialogView?.setTitle(title)
        dialogView?.setMessage(message)
        dialogView?.setListener(object : DialogView.ButtonListener {
            override fun onNegativeButtonClick(dialog: AlertDialog) {
                listener.onNegativeButtonClick(dialog)
                moveNextDialog()
            }

            override fun onPositiveButtonClick(dialog: AlertDialog) {
                listener.onPositiveButtonClick(dialog)
                moveNextDialog()
            }
        })
        dialogView?.setPositiveButtonText(positiveButton)
        dialogView?.setNegativeButtonText(negativeButton)
        if (dialogStack.isEmpty()) dialogView?.show()

        dialogStack.add(dialogView)
        LogSystem.e(BaseActivity::class.java.simpleName, "Dialog Stack Size : ${dialogStack.size}")
    }

    private fun moveNextDialog() {
        if (dialogStack.isNotEmpty()) {
            dialogStack.get(0).onDestroyView()
            dialogStack.removeAt(0)
        }
        if (dialogStack.isNotEmpty()) dialogStack.get(0).show()
    }

    var blockEvents = AtomicBoolean(false)
    private var blockEventHandler: Handler? = null
    private val blockEventRunnable = Runnable { blockEvents.set(false) }
    open fun blockEvents() {
        if (blockEvents.get()) return
        blockEvents.set(true)
        blockEventHandler!!.removeCallbacks(blockEventRunnable)
        blockEventHandler!!.postDelayed(blockEventRunnable, 600)
    }


    var ActivityLaunder: BetterActivityResult<Intent, ActivityResult> =
        BetterActivityResult.registerActivityForResult(this)

    fun hasPermission(permission: String) =
        (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)


    fun getActivity() = this


    fun writeLog(tag: String, message: String) {
        LogSystem.e(tag, message)
    }


    fun showInfoDialog(
        message: String?, lister: DialogView.ButtonListener = object : DialogView.ButtonListener {
            override fun onNegativeButtonClick(dialog: AlertDialog) {
                dialog.dismiss()
            }

            override fun onPositiveButtonClick(dialog: AlertDialog) {
                dialog.dismiss()
            }
        }
    ) {
        CommonUtils.writeEvent("${javaClass.simpleName} showInfoDialog() called with: message = $message")
        showDialog(
            title = "Info",
            message = message ?: "N/A",
            positiveButton = "OK",
            negativeButton = "",
            listener = lister
        )
    }

    fun showErrorDialog(message: String?) {
        CommonUtils.writeEvent("${javaClass.simpleName} showErrorDialog() called with: message = $message")
        showDialog(title = "Error",
            message = message ?: "N/A",
            positiveButton = "OK",
            negativeButton = "",
            listener = object : DialogView.ButtonListener {
                override fun onNegativeButtonClick(dialog: AlertDialog) {
                    dialog.dismiss()
                }

                override fun onPositiveButtonClick(dialog: AlertDialog) {
                    dialog.dismiss()
                }
            })
    }

    fun showErrorDialog(title: String, message: String?) {
        CommonUtils.writeEvent("${javaClass.simpleName} showErrorDialog() called with: title = $title, message = $message")
        showDialog(title = title,
            message = message ?: "N/A",
            positiveButton = "OK",
            negativeButton = "",
            listener = object : DialogView.ButtonListener {
                override fun onNegativeButtonClick(dialog: AlertDialog) {
                    dialog.dismiss()
                }

                override fun onPositiveButtonClick(dialog: AlertDialog) {
                    dialog.dismiss()
                }
            })
    }

    public fun showInternetDialog() {
        showErrorDialog(
            "Internet",
            "Please turn on the mobile data. it's required to upload your data in the server."
        )
    }

    fun isNotSafe() = isFinishing || isDestroyed

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        LocalVideoPlayerPropertyManager.lastInteractionTime = System.currentTimeMillis()
        return super.dispatchTouchEvent(ev)
    }
}