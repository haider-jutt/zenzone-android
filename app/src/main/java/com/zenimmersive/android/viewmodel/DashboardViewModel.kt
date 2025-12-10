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
import com.zenimmersive.android.repository.DashboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: DashboardRepository): ViewModel() {
    private val TAG = DashboardViewModel::class.java.simpleName

    private val _deviceToken: MutableLiveData<ViewState<BaseResponse>> = MutableLiveData(ViewState.DefaultState)
    val deviceToken = _deviceToken

    fun updateDeviceToken(fcmDeviceToken: String){
        _deviceToken.postValue(ViewState.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            val resModel = BaseResponse()
            when (val res = repository.updateDeviceToken(fcmDeviceToken, resModel)) {
                is Success -> {
                    Log.d(TAG, "fcm device token update response : ===${res.value}")
                    _deviceToken.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _deviceToken.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }

}