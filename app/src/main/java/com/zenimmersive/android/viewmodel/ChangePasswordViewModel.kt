package com.zenimmersive.android.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenimmersive.android.base.BaseResponse
import com.zenimmersive.android.base.Failure
import com.zenimmersive.android.base.Loading
import com.zenimmersive.android.base.Success
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.repository.ChangePasswordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChangePasswordViewModel(private val repository: ChangePasswordRepository): ViewModel() {
    private val TAG = ChangePasswordViewModel::class.java.simpleName

    private val _changePassword: MutableLiveData<ViewState<BaseResponse>> = MutableLiveData(ViewState.DefaultState)
    val changePasswordRes = _changePassword

    fun changePassword(urlUserToken: String, newPassword: String){
        _changePassword.postValue(ViewState.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            val resModel = BaseResponse()
            when (val res = repository.changePassword(urlUserToken, newPassword, resModel)) {
                is Success -> {
                    Log.d(TAG, "change password response : ===${res.value}")
                    _changePassword.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _changePassword.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }

}