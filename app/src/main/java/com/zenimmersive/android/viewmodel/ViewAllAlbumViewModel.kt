package com.zenimmersive.android.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenimmersive.android.apiresponsemodel.AlbumDetailsResModel
import com.zenimmersive.android.base.Failure
import com.zenimmersive.android.base.Loading
import com.zenimmersive.android.base.Success
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.helper.CommonUtils
import com.zenimmersive.android.repository.ViewAllAlbumRepository
import kotlinx.coroutines.async

class ViewAllAlbumViewModel(private val repository: ViewAllAlbumRepository): ViewModel() {
    private val TAG = ViewAllAlbumViewModel::class.java.simpleName

    private val _homeResData: MutableLiveData<ViewState<AlbumDetailsResModel>> = MutableLiveData(ViewState.DefaultState)
    val homeResData = _homeResData

    fun getAllAlbumsData(cache: Boolean = true){
        _homeResData.postValue(ViewState.Loading)

        viewModelScope.async {
            val resModel = AlbumDetailsResModel(null, null, null)
            if(CommonUtils.isNetworkAvailable(repository.context) && !cache)
            {
                when (val res = repository.getAllAlbumsData(resModel)) {
                    is Success -> {
                        Log.d("ViewAllAlbumViewModel", "get all albums data response : ===${res.value}")
                        _homeResData.postValue(ViewState.Data(res.value))
                    }
                    is Failure -> {
                        _homeResData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                    }
                    is Loading -> {
                    }
                }
            }
            else
            {
                when (val res = repository.getAllAlbumsData(resModel, true)) {
                    is Success -> {
                        Log.d("ViewAllAlbumViewModel", "get all albums response : ===${res.value}")
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
}