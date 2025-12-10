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
import com.zenimmersive.android.helper.SingleLiveEvent
import com.zenimmersive.android.repository.SettingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingViewModel(private val repository: SettingRepository): ViewModel() {
    private val TAG = SettingViewModel::class.java.simpleName
    private val _logoutData: SingleLiveEvent<ViewState<BaseResponse>> = SingleLiveEvent()
    val logoutData = _logoutData

    fun logout(userId:Int, userToken: String, resModel: BaseResponse){
        _logoutData.postValue(ViewState.Loading)

        viewModelScope.launch(Dispatchers.IO) {
            when (val res = repository.logout(userId, userToken, resModel)) {
                is Success -> {
                    Log.d(TAG, "logout response : ===${res.value}")
                    _logoutData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _logoutData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }

            }
        }
    }

    private val _deleteAccountData: MutableLiveData<ViewState<BaseResponse>> = MutableLiveData(ViewState.DefaultState)
    val deleteAccountData = _deleteAccountData
    fun deleteAccount(){
        _deleteAccountData.postValue(ViewState.Loading)

        viewModelScope.launch(Dispatchers.IO) {
            val resModel = BaseResponse()
            when (val res = repository.deleteAccount(resModel)) {
                is Success -> {
                    Log.d(TAG, "delete account response : ===${res.value}")
                    _deleteAccountData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _deleteAccountData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }
}