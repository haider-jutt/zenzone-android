package com.zenimmersive.android.base

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.zenimmersive.android.helper.CommonUtils
import com.zenimmersive.android.helper.DialogView
import com.zenimmersive.android.helper.KeyboardUtil
import com.zenimmersive.android.helper.LogManager
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.helper.ProgressDialog
import com.zenimmersive.android.helper.hide
import com.zenimmersive.android.helper.show
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean


abstract class BaseFragment<VM : ViewModel, B : ViewBinding, R : BaseRepository> :
    Fragment() {

    var viewLoader: FrameLayout? = null
    var tvLoaderMessage: TextView? = null

    lateinit var viewBinding: B

    lateinit var viewModel: VM
    protected val remoteDataSource: RemoteDataSource = RemoteDataSource()

    var progressDialog: ProgressDialog? = null

    abstract fun getViewModel(): Class<VM>
    abstract fun getActivityBinding(inflater: LayoutInflater, container: ViewGroup?): B
    abstract fun getRepository(): R

    abstract fun registerObservers()
    abstract fun unregisterObservers()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogSystem.e("PageLifeCycle", "${this.javaClass.simpleName} onCreate Invoked")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        LogSystem.e("PageLifeCycle", "${this.javaClass.simpleName} onCreateView Invoked")
        synchronized(this) {
            viewBinding = getActivityBinding(inflater, container)
            return viewBinding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LogSystem.e("PageLifeCycle", "${this.javaClass.simpleName} onViewCreated Invoked")

        KeyboardUtil.setupUI(viewBinding.root)

        CommonUtils.setLocale(requireContext())

        progressDialog = ProgressDialog(requireContext())

        val factory = ViewModelFactory(getRepository())
        viewModel = ViewModelProvider(this@BaseFragment, factory).get(getViewModel())

        isObserversRemoved = false

        synchronized(this) {
            registerObservers()
        }
    }

    fun setFragmentViewBinding(viewBinding: B) {
        synchronized(this) {
            this.viewBinding = viewBinding
        }
    }

    var isObserversRemoved = false
    override fun onDestroy() {
        if (!isObserversRemoved) {
            unregisterObservers()
            isObserversRemoved = true
        }
        clearDialogs()
        super.onDestroy()
    }

    override fun onDestroyView() {
        if (!isObserversRemoved) {
            unregisterObservers()
            isObserversRemoved = true
        }
        clearDialogs()
        super.onDestroyView()
    }

    fun showLoader() {
        viewLoader?.show()
        tvLoaderMessage?.text = getString(com.zenimmersive.android.R.string.please_wait)
    }

    fun showLoader(message: String) {
        viewLoader?.let {
            viewLoader?.show()
            tvLoaderMessage?.text = message
        } ?: run {
            progressDialog?.setMessage(message)
            progressDialog?.show()
        }
    }

    fun hideLoader() {
        viewLoader?.post { viewLoader?.hide() }
        progressDialog?.dismiss()
    }

    fun onFail(message: String) {
        showToast(message)
    }

    fun showToast(msg: String) {
        try {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
        }
    }

    fun applyActivityTransitionAnimation() {
        requireActivity()?.overridePendingTransition(
            com.zenimmersive.android.R.anim.animate_swipe_left_enter,
            com.zenimmersive.android.R.anim.animate_swipe_left_exit
        )
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
        title: String = "Zen Immersive",
        message: String = "Empty!",
        positiveButton: String = "OK",
        negativeButton: String = "Dimiss",
        listener: DialogView.ButtonListener = object : DialogView.ButtonListener {
            override fun onNegativeButtonClick(dialog: AlertDialog) {
                dialog.dismiss()
            }

            override fun onPositiveButtonClick(dialog: AlertDialog) {
                dialog.dismiss()
            }
        }
    ) {
        CommonUtils.writeEvent(
            "${javaClass.simpleName} showDialog() called with: title = $title, message = $message, positiveButton = $positiveButton, negativeButton = $negativeButton, listener = $listener"
        )
        if (!isSafe()) return
        var dialogView = DialogView(requireContext())
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

    fun isSafe() = !isRemoving && !isDetached && view != null && context != null && isAdded
    fun isNotSafe() = !isSafe()

    fun shareLogFile() {
        try {
            var logFile = LogManager.getLogManager().sendLogs()
            var path = logFile.filePath
            val file = File(path)
            var uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().applicationContext.packageName + ".provider",
                file
            )
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.setType("text/*")
            sharingIntent.putExtra(
                Intent.EXTRA_STREAM,
                uri
            )
            sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(sharingIntent, "share file with"))
        } catch (e: Exception) {

        }
    }

    fun getParentHandler(): FragmentViewPressEvent? {
        if (parentFragment != null && parentFragment is FragmentViewPressEvent) return parentFragment as FragmentViewPressEvent
        if (requireActivity() is FragmentViewPressEvent) return requireActivity() as FragmentViewPressEvent
        return null
    }

    override fun onPause() {
        super.onPause()
        LogSystem.e("PageLifeCycle", "${this.javaClass.simpleName} onPause Invoked")
    }

    override fun onResume() {
        super.onResume()
        LogSystem.e("PageLifeCycle", "${this.javaClass.simpleName} onResume Invoked")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        LogSystem.e("PageLifeCycle", "${this.javaClass.simpleName} onSaveInstanceState Invoked")

    }
}