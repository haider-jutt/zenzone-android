package com.zenimmersive.android.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenimmersive.android.apiresponsemodel.UserBasicResModel
import com.zenimmersive.android.base.Failure
import com.zenimmersive.android.base.Loading
import com.zenimmersive.android.base.Success
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.repository.VerificationRepository
import kotlinx.coroutines.launch

class VerificationViewModel(private val repository: VerificationRepository): ViewModel() {

    private val _otpVerifyData: MutableLiveData<ViewState<UserBasicResModel>> = MutableLiveData(
        ViewState.DefaultState)
    val otpVerifyData = _otpVerifyData

    fun emailOtpVerify(userId: Int, userToken: String, otp: String, resModel: UserBasicResModel){
        _otpVerifyData.postValue(ViewState.Loading)

        viewModelScope.launch {
            when (val res = repository.verificationEmail(userId, userToken, otp, resModel)) {
                is Success -> {
                    Log.d("VerificationViewModel", "email verification response : ===${res.value}")
                    _otpVerifyData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _otpVerifyData.postValue(ViewState.Error(res.errorMessage))
                }
                is Loading -> {
                }

            }
        }
    }
}