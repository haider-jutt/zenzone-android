package com.zenimmersive.android.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenimmersive.android.apiresponsemodel.HomeDataResModel
import com.zenimmersive.android.base.Failure
import com.zenimmersive.android.base.Loading
import com.zenimmersive.android.base.Success
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.LogSystem
import com.zenimmersive.android.repository.HomeRepository
import kotlinx.coroutines.async

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {

    private val _homeResData: MutableLiveData<ViewState<HomeDataResModel>> =
        MutableLiveData(ViewState.DefaultState)
    val homeResData = _homeResData

    fun getHomeData(
        userId: Int,
        userToken: String,
        resModel: HomeDataResModel,
        cache: Boolean = true
    ) {
        LogSystem.e("PageLifeCycle","Home API Called isServer = ${!cache}")
        _homeResData.postValue(ViewState.Loading)

        viewModelScope.async {
            when (val res = repository.homeData(userId, userToken, resModel, cache)) {
                is Success -> {
                    if (!cache) {
                        KeyStorage.getInstance(repository.context).storeHomePageServerCall()
                    }
                    _homeResData.postValue(ViewState.Data(res.value))
                }

                is Failure -> {
                    _homeResData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }

                is Loading -> {
                }
            }
        }
    }
}