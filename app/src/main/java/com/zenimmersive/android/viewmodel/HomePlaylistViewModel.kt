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
import com.zenimmersive.android.repository.HomePlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomePlaylistViewModel(private val repository: HomePlaylistRepository): ViewModel() {

    private val _albumDetailsData: MutableLiveData<ViewState<AlbumDetailsResModel>> = MutableLiveData(ViewState.DefaultState)
    val albumDetailsData = _albumDetailsData

    fun getHomeData(userId:Int, userToken: String, albumId: Int, resModel: AlbumDetailsResModel){
        _albumDetailsData.postValue(ViewState.Loading)

        viewModelScope.launch {
            when (val res = repository.getAlbumDetail(userId, userToken, albumId, resModel)) {
                is Success -> {
                    Log.d("HomePlaylistViewModel", "album detail response : ===${res.value}")
                    _albumDetailsData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _albumDetailsData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }

    private val _tagWiseMusicData: MutableLiveData<ViewState<AlbumDetailsResModel>> = MutableLiveData(ViewState.DefaultState)
    val tagWiseMusicData = _tagWiseMusicData

    fun getHomeData(tagId: Int, cache : Boolean=  false){
        _tagWiseMusicData.postValue(ViewState.Loading)

        viewModelScope.launch(Dispatchers.IO) {
            val resModel = AlbumDetailsResModel(null,null,null)
            when (val res = repository.getTagsSongs(tagId, resModel,cache)) {
                is Success -> {
                    Log.d("HomePlaylistViewModel", "tag wise music response : ===${res.value}")
                    _tagWiseMusicData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _tagWiseMusicData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }
}