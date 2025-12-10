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
import com.zenimmersive.android.repository.LoginRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: LoginRepository): ViewModel() {

    private val _userLoginData: MutableLiveData<ViewState<UserBasicResModel>> = MutableLiveData(
        ViewState.DefaultState)
    val userLoginData = _userLoginData

    // social login
    private val _userSocialLoginData: MutableLiveData<ViewState<UserBasicResModel>> = MutableLiveData(
        ViewState.DefaultState)
    val userSocialLoginData = _userSocialLoginData

    fun userLogin(email: String, password: String, resModel: UserBasicResModel){
        _userLoginData.postValue(ViewState.Loading)

        viewModelScope.launch {
            when (val res = repository.userLogin(email, password, resModel)) {
                is Success -> {
                    Log.d("LoginViewModel", "user login response : ===${res.value}")
                    _userLoginData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _userLoginData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }

            }
        }
    }

    fun userSocialLogin(email:String, registrationType:String, socialToken:String, socialId:String, userName:String, resModel : UserBasicResModel){
        _userSocialLoginData.postValue(ViewState.Loading)

        viewModelScope.launch {
            when (val res = repository.userSocialLogin(email, registrationType, socialToken, socialId, userName, resModel)) {
                is Success -> {
                    Log.d("LoginViewModel", "user social login response : ===${res.value}")
                    _userSocialLoginData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _userSocialLoginData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }
}