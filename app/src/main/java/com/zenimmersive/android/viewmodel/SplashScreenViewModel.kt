package com.zenimmersive.android.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenimmersive.android.apiresponsemodel.QuoteMessageResModel
import com.zenimmersive.android.apiresponsemodel.UserDetailsResModel
import com.zenimmersive.android.base.Failure
import com.zenimmersive.android.base.Loading
import com.zenimmersive.android.base.Resource
import com.zenimmersive.android.base.Success
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_ID
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_TOKEN
import com.zenimmersive.android.repository.SplashRepository
import kotlinx.coroutines.launch

class SplashScreenViewModel(private val repository: SplashRepository): ViewModel() {

    // social login
    private val _getQuoteResData: MutableLiveData<ViewState<QuoteMessageResModel>> = MutableLiveData(
        ViewState.DefaultState)
    val getQuoteResData = _getQuoteResData

    fun getQuoteMessage(){
        _getQuoteResData.postValue(ViewState.Loading)

        viewModelScope.launch {
            val quoteMessageResModel = QuoteMessageResModel(null,null,null)
            when (val res = repository.getQuoteMessage(quoteMessageResModel)) {
                is Success -> {
                    Log.d("SplashScreenViewModel", "quote response : ===${res.value}")
                    _getQuoteResData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _getQuoteResData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }


    fun fetchUserProfile() : LiveData<Resource<UserDetailsResModel>?> {
        val _userData: MutableLiveData<Resource<UserDetailsResModel>?> = MutableLiveData()
        var context = repository.context
        var userID = KeyStorage.getInstance(context).getInt(KEY_USER_ID)
        val resModel = UserDetailsResModel(null,null,null)
        var userToken = KeyStorage.getInstance(context).getString(KEY_USER_TOKEN)

        viewModelScope.launch {
            when (val res = repository.fetchUserDetailsAsync(userID, userToken,resModel)) {
                is Success -> {
                    _userData.postValue(res)
                    Log.d("SplashScreenViewModel", "user profile response : ===${res.value}")
                }
                is Failure -> {
                    _userData.postValue(res)
                    Log.d("SplashScreenViewModel", "user profile error : ===${res.errorMessage}")
                }
                is Loading -> {
                }
            }
        }
        return _userData
    }
}