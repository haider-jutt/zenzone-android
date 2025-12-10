package com.zenimmersive.android.ui.setting

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import com.zenimmersive.android.R
import com.zenimmersive.android.apiresponsemodel.UserDetailsResModel
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.FragmentEditProfileBinding
import com.zenimmersive.android.helper.CommonUtils
import com.zenimmersive.android.helper.Constants.languageMap
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_ID
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_TOKEN
import com.zenimmersive.android.repository.UserProfileRepository
import com.zenimmersive.android.ui.BackPressListener
import com.zenimmersive.android.ui.player.PlayerManager
import com.zenimmersive.android.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditProfileFragment : BaseFragment<ProfileViewModel, FragmentEditProfileBinding, UserProfileRepository>(),
    BackPressListener {
    var userID: Int = 0
    lateinit var userToken: String
    private var isMale: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userID = KeyStorage.getInstance(requireContext()).getInt(KEY_USER_ID)
        userToken = KeyStorage.getInstance(requireContext()).getString(KEY_USER_TOKEN)
    }

    override fun getViewModel(): Class<ProfileViewModel> {
        return ProfileViewModel::class.java
    }

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentEditProfileBinding {
        return FragmentEditProfileBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObserver()
        val resModel = UserDetailsResModel(null,null,null)
        viewModel.getUserDetails(userID, userToken, resModel)

        val oldSelectedLanguageKey = languageMap.entries.find { it.value == KeyStorage.getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE) }?.key

        var items = resources.getTextArray(R.array.language_array)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        viewBinding.spLanguages.adapter = adapter
        if (!oldSelectedLanguageKey.isNullOrBlank()){
            val position = items.indexOf(oldSelectedLanguageKey)
            if (position >= 0) {
                viewBinding.spLanguages.setSelection(position)
            }
        }

        viewBinding.ivBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        viewBinding.etBirthday.setOnClickListener {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val today = Calendar.getInstance()

            // Try to parse existing date from EditText
            val initialCalendar = Calendar.getInstance()
            val dateText = viewBinding.etBirthday.text.toString()
            if (dateText.isNotEmpty()) {
                try {
                    val parsedDate = dateFormat.parse(dateText)
                    parsedDate?.let { initialCalendar.time = it }
                } catch (e: Exception) {
                    // fallback to today if parsing fails
                    e.printStackTrace()
                }
            }

            val maxDate = Calendar.getInstance().apply {
                add(Calendar.YEAR, -18)
                add(Calendar.DATE, -1)
            }

            val dpd = DatePickerDialog(
                ContextThemeWrapper(requireContext(), R.style.SpinnerDatePickerDialogTheme),
                { _, year, month, dayOfMonth ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }.time
                    viewBinding.etBirthday.setText(dateFormat.format(selectedDate))
                },
                initialCalendar.get(Calendar.YEAR),
                initialCalendar.get(Calendar.MONTH),
                initialCalendar.get(Calendar.DAY_OF_MONTH)
            )

            dpd.datePicker.maxDate = maxDate.timeInMillis
            dpd.show()
            dpd.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
            dpd.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))


            /*val today = Calendar.getInstance()
            val dpd = DatePickerDialog(requireContext(), R.style.DialogTheme, { view, year, monthOfYear, dayOfMonth ->

                val selectedDate = Calendar.getInstance().apply {
                    set(year, monthOfYear, dayOfMonth)
                }.time
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                viewBinding.etBirthday.setText(dateFormat.format(selectedDate))

            }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))

            val maxdate = Calendar.getInstance()
            maxdate.add(Calendar.YEAR, -18)
            maxdate.add(Calendar.DATE, -1)
            dpd.datePicker.maxDate = maxdate.timeInMillis

            dpd.show()*/
        }

        viewBinding.llMale.setOnClickListener {
            isMale = 1
            viewBinding.llMale.background = ContextCompat.getDrawable(requireContext(), R.drawable.background_search_view)
            viewBinding.ivMale.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.colorPrimary))
            viewBinding.tvMale.setLinkTextColor(ContextCompat.getColor(requireContext(),R.color.colorPrimary))

            viewBinding.llFemale.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_shape_secoundry)
            viewBinding.ivFemale.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.light_gray_blue))
            viewBinding.tvFemale.setLinkTextColor(ContextCompat.getColor(requireContext(),R.color.white))
        }
        viewBinding.llFemale.setOnClickListener {
            isMale = 2
            viewBinding.llFemale.background = ContextCompat.getDrawable(requireContext(), R.drawable.background_search_view)
            viewBinding.ivFemale.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.colorPrimary))
            viewBinding.tvFemale.setLinkTextColor(ContextCompat.getColor(requireContext(),R.color.colorPrimary))

            viewBinding.llMale.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_shape_secoundry)
            viewBinding.ivMale.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.light_gray_blue))
            viewBinding.tvMale.setLinkTextColor(ContextCompat.getColor(requireContext(),R.color.white))
        }

        viewBinding.buttonSave.setOnClickListener {
            val authSuccessful: Boolean = authenticate(
                viewBinding.etFullName.text.toString(),
                viewBinding.etEmail.text.toString(),
                viewBinding.etBirthday.text.toString()
            )
            if (authSuccessful) {
                val regModel = UserDetailsResModel(null, null, null)
                viewModel.editProfile(userID, userToken, viewBinding.etFullName.text.toString(), viewBinding.etBirthday.text.toString(), viewBinding.etEmail.text.toString(), isMale, "usa", regModel)
            }
        }
    }

    override fun getRepository(): UserProfileRepository {
        return UserProfileRepository(requireContext())
    }

    override fun registerObservers() {
    }

    override fun unregisterObservers() {
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    fun initObserver(){
        viewModel.userData.observe(viewLifecycleOwner) { resData ->
            when(resData){
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader("Loading...")
                }
                is ViewState.Data -> {
                    hideLoader()
                    viewBinding.etFullName.setText(resData.data.result?.userName ?: "")
                    viewBinding.etEmail.setText(resData.data.result?.email ?: "")

                    resData.data.result?.birthdate?.let {
                        val originalFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val targetFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val date = originalFormat.parse(it)
                        val formattedDate = targetFormat.format(date)
                        viewBinding.etBirthday.setText(formattedDate)
                    }

                    // TODO Confirm of gender value to backend developer
                    if (resData.data.result?.gender == isMale){
                        viewBinding.llMale.performClick()
                    }else{
                        viewBinding.llFemale.performClick()
                    }

                    /*if (!resData.data.result?.country.isNullOrBlank()){
                        viewBinding.cpCountry.setCountryForPhoneCode((resData.data.result?.country?.toInt() ?: 91))
                    }else{
                        viewBinding.cpCountry.setCountryForPhoneCode(91)
                    }*/

                }
                is ViewState.Error -> {
                    hideLoader()
                    showToast(resData.error)
                }
            }
        }

        viewModel.editProfileData.observe(viewLifecycleOwner) { resData ->
            when(resData){
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader(getString(R.string.loading))
                }
                is ViewState.Data -> {
                    hideLoader()
                    resData?.data?.message?.let {
                        showToast(resData.data.message!!)
                    }
                    if (resData?.data?.status == 1){
                        val oldSelectedLanguageKey = languageMap.entries.find { it.value == KeyStorage.getInstance(requireContext()).getString(APP_SELECTED_LANGUAGE) }?.key
                        CommonUtils.setLocale(requireContext(), languageMap[viewBinding.spLanguages.selectedItem.toString()]!!)
                        KeyStorage.getInstance(requireContext()).setString(APP_SELECTED_LANGUAGE, languageMap[viewBinding.spLanguages.selectedItem.toString()])
                        if (oldSelectedLanguageKey != languageMap[viewBinding.spLanguages.selectedItem.toString()]){
                            val playerManager = PlayerManager?.getInstance()
                            if (playerManager?.getCurrentMusicItem() != null){
                                playerManager.reload()
                            }
                        }

                        if(!onBackPressed()) activity?.onBackPressed()
                    }

                }
                is ViewState.Error -> {
                    hideLoader()
                    showToast(resData.error)
                }
            }

        }
    }

    fun authenticate(
        userName: String,
        email: String,
        birthDt: String,
    ): Boolean {
        var check = false
        if (userName.isEmpty()) {
            showToast(getString(R.string.please_enter_user_full_name))
            check = false

        } else if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast(getString(R.string.please_enter_valid_email))
            check = false

        } else if (birthDt.isNullOrBlank()) {
            showToast(getString(R.string.please_select_your_birthdate))
            check = false

        } else {
            check = true
        }
        return check
    }
}