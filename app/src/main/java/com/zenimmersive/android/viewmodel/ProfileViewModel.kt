package com.zenimmersive.android.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenimmersive.android.apiresponsemodel.UserDetailsResModel
import com.zenimmersive.android.base.Failure
import com.zenimmersive.android.base.Loading
import com.zenimmersive.android.base.Success
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.repository.UserProfileRepository
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: UserProfileRepository): ViewModel() {

    private val _userData: MutableLiveData<ViewState<UserDetailsResModel>> = MutableLiveData(
        ViewState.DefaultState)
    val userData = _userData


    private val _editProfileData: MutableLiveData<ViewState<UserDetailsResModel>> = MutableLiveData(
        ViewState.DefaultState)
    val editProfileData = _editProfileData

    fun getUserDetails(userId:Int, userToken: String, resModel: UserDetailsResModel){
        _userData.postValue(ViewState.Loading)

        viewModelScope.launch {
            when (val res = repository.fetchUserDetailsAsync(userId, userToken, resModel)) {
                is Success -> {
                    Log.d("EditProfileViewModel", "get user details response : ===${res.value}")
                    _userData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _userData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }

            }
        }
    }

    fun editProfile(userId: Int, userToken: String, userName: String, birthdate: String, email: String, gender:Int, country:String,  resModel:UserDetailsResModel){
        _editProfileData.postValue(ViewState.Loading)

        viewModelScope.launch {
            when (val res = repository.editProfile(userId, userToken, userName, birthdate, email, gender, country, resModel)) {
                is Success -> {
                    Log.d("EditProfileViewModel", "edit user response : ===${res.value}")
                    _editProfileData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _editProfileData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }
}