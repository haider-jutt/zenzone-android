package com.zenimmersive.android.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenimmersive.android.apiresponsemodel.StaticPagesResModel
import com.zenimmersive.android.base.Failure
import com.zenimmersive.android.base.Loading
import com.zenimmersive.android.base.Success
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.repository.SettingRepository
import kotlinx.coroutines.launch

class StaticPageViewModel(private val repository: SettingRepository): ViewModel() {

    private val _staticPageData: MutableLiveData<ViewState<StaticPagesResModel>> = MutableLiveData(ViewState.DefaultState)
    val staticPageData = _staticPageData

    fun staticPageInfo(userId:Int, userToken: String, staticPageId: Int, resModel: StaticPagesResModel){
        _staticPageData.postValue(ViewState.Loading)

        viewModelScope.launch {
            when (val res = repository.staticPages(userId, userToken, staticPageId, resModel)) {
                is Success -> {
                    Log.d("StaticPageViewModel", "static page response : ===${res.value}")
                    _staticPageData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _staticPageData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }

            }
        }
    }
}