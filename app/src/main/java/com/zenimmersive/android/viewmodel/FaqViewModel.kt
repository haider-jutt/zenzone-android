package com.zenimmersive.android.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenimmersive.android.apiresponsemodel.FaqResModel
import com.zenimmersive.android.base.Failure
import com.zenimmersive.android.base.Loading
import com.zenimmersive.android.base.Success
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.repository.SettingRepository
import kotlinx.coroutines.launch

class FaqViewModel(private val repository: SettingRepository): ViewModel() {

    private val _faqData: MutableLiveData<ViewState<FaqResModel>> = MutableLiveData(ViewState.DefaultState)
    val faqData = _faqData

    fun faqGetList(userId:Int, userToken: String, resModel: FaqResModel){
        _faqData.postValue(ViewState.Loading)

        viewModelScope.launch {
            when (val res = repository.getFaq(userId, userToken, resModel, KeyStorage.getInstance(repository.context).getString(
                KeyStorage.APP_SELECTED_LANGUAGE
            ))) {
                is Success -> {
                    Log.d("FaqViewModel", "faq list response : ===${res.value}")
                    _faqData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _faqData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }

            }
        }
    }
}