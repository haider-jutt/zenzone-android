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
import com.zenimmersive.android.repository.RegisterRepository
import kotlinx.coroutines.launch

class RegisterViewModel(private val repository: RegisterRepository): ViewModel() {

    private val _registerData: MutableLiveData<ViewState<UserBasicResModel>> = MutableLiveData(ViewState.DefaultState)
    val registerData = _registerData

    fun registerUser(username: String, email: String, password: String, resModel: UserBasicResModel){
        _registerData.postValue(ViewState.Loading)
        viewModelScope.launch {
            when (val res = repository.register(username, email, password, resModel)) {
                is Success -> {
//                    val resp = res.value.string()
//                    val reader: Reader = StringReader(resp)
//                    val builder = GsonBuilder()
//                    val gson = builder.create()
//
//                    val response: RegisterResModel = gson.fromJson(reader, RegisterResModel::class.java)
                    Log.d("BlankViewModel", "user register response : ===${res.value}")
                    _registerData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _registerData.postValue(ViewState.Error(res.errorMessage))
                }
                is Loading -> {
                }
                /*
                                is ResultState.Success -> {
                                    _root_ide_package_.android.util.Log.e("register", "register response: ${res.data}" )
                                    _registerData.postValue(ViewState.Data(res.data))
                                }
                                is ResultState.SuccessWithNoData -> {

                                }
                                is ResultState.Error ->{
                                    _registerData.postValue(ViewState.Error(res.error))
                                }*/
            }
        }
    }

    // social login
    private val _userSocialLoginData: MutableLiveData<ViewState<UserBasicResModel>> = MutableLiveData(
        ViewState.DefaultState)
    val userSocialLoginData = _userSocialLoginData

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