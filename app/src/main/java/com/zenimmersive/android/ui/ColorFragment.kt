package com.zenimmersive.android.ui

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.zenimmersive.android.R
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.databinding.FragmentColorBinding
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.helper.WhiteAmbiencePalette
import com.zenimmersive.android.helper.hide
import com.zenimmersive.android.hue.HueLightManager
import com.zenimmersive.android.hue.TaskCallback
import com.zenimmersive.android.model.LightListResult
import com.zenimmersive.android.repository.BlankRepository
import com.zenimmersive.android.viewmodel.BlankViewModel
import com.skydoves.colorpickerview.ActionMode
import com.skydoves.colorpickerview.flag.BubbleFlag
import com.skydoves.colorpickerview.flag.FlagMode
import com.skydoves.colorpickerview.listeners.ColorListener


class ColorFragment : BaseFragment<BlankViewModel, FragmentColorBinding, BlankRepository>() {


    companion object {
        @JvmStatic
        fun newInstance() =
            ColorFragment()
    }

    override fun getViewModel(): Class<BlankViewModel> = BlankViewModel::class.java

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentColorBinding = FragmentColorBinding.inflate(inflater, container, false)

    override fun getRepository(): BlankRepository = BlankRepository(requireContext())

    override fun registerObservers() {
        setupUiEvents()

        val bubbleFlag = BubbleFlag(context)
        bubbleFlag.flagMode = FlagMode.FADE
        viewBinding.colorPickerView.setFlagView(bubbleFlag)
        viewBinding.colorPickerView.actionMode = ActionMode.LAST


        var hueLightManager = HueLightManager.getInstance(requireContext())

        viewBinding.colorPickerView.setColorListener(object : ColorListener {
            override fun onColorSelected(color: Int, fromUser: Boolean) {
                LogSystem.e("Color Change Fire : $color FromUser : $fromUser")
                if (fromUser) {
                    hueLightManager.changeColor(color)
                }
            }
        })

        hueLightManager?.fetchLightListByCache().let {
            if (it == null) {
                hueLightManager?.fetchLights(object : TaskCallback<LightListResult> {
                    override fun onTaskComplete(result: LightListResult) {
                        bindResult(result)
                        viewBinding.bridgeSyncMessage.hide()
                    }

                    override fun onTaskError(error: String?) {
                        viewBinding.bridgeSyncMessage.hide()
                    }

                })
            } else {
                bindResult(it!!)
                viewBinding.bridgeSyncMessage.hide()
            }
        }
    }

    var lightListResult: LightListResult? = null
    private fun bindResult(it: LightListResult) {
        lightListResult = it
    }

    private fun setupUiEvents() {
        viewBinding.viewColorWheel.setOnClickListener {
            updateSelectionColorWheelUI(viewBinding.viewColorWheel)
            viewBinding.colorPickerView.setHsvPaletteDrawable()
        }
        viewBinding.viewColorWhiteAmbienceWheel.setOnClickListener {
            updateSelectionColorWheelUI(viewBinding.viewColorWhiteAmbienceWheel)
            viewBinding.colorPickerView.post {
                var drawable = WhiteAmbiencePalette(
                    resources,
                    viewBinding.colorPickerView.width,
                    viewBinding.colorPickerView.height
                )
                drawable.setBounds(
                    0,
                    0,
                    viewBinding.colorPickerView.width,
                    viewBinding.colorPickerView.height
                )
                var bitmap = drawable.createBitmap(
                    viewBinding.colorPickerView.width,
                    viewBinding.colorPickerView.height
                )
                viewBinding.colorPickerView.setPaletteDrawable(BitmapDrawable(resources, bitmap))
            }
        }
    }

    private fun updateSelectionColorWheelUI(view: FrameLayout) {
        viewBinding.viewColorWheel.getChildAt(0).setBackgroundColor(Color.TRANSPARENT)
        viewBinding.viewColorWhiteAmbienceWheel.getChildAt(0).setBackgroundColor(Color.TRANSPARENT)

        view.getChildAt(0).setBackgroundResource(R.drawable.rounded_circle_white)
    }

    override fun unregisterObservers() {
    }
}