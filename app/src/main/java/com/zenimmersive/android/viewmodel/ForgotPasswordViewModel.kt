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
import com.zenimmersive.android.repository.ForgotPasswordRepository
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(private val repository: ForgotPasswordRepository): ViewModel() {

    private val _forgotPasswordData: MutableLiveData<ViewState<UserBasicResModel>> = MutableLiveData(
        ViewState.DefaultState)
    val forgotPasswordData = _forgotPasswordData

    fun forgotPassword(email: String, resModel: UserBasicResModel){
        _forgotPasswordData.postValue(ViewState.Loading)

        viewModelScope.launch {
            when (val res = repository.forgotPassword(email, resModel)) {
                is Success -> {
                    Log.d("ForgotPasswordViewModel", "forgot password response : ===${res.value}")
                    _forgotPasswordData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _forgotPasswordData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }

            }
        }
    }
}