package com.zenimmersive.android.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenimmersive.android.apiresponsemodel.AddRemoveFavoriteSongApi
import com.zenimmersive.android.apiresponsemodel.SongDetailsResModel
import com.zenimmersive.android.base.Failure
import com.zenimmersive.android.base.Loading
import com.zenimmersive.android.base.Success
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.repository.PlayerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayerViewModel(private val repository: PlayerRepository): ViewModel() {
    val TAG = PlayerViewModel::class.java.simpleName
    private val _songData: MutableLiveData<ViewState<SongDetailsResModel>> = MutableLiveData(ViewState.DefaultState)
    val songData = _songData

    fun postSongAnalysisUse(songId: Int){
        _songData.postValue(ViewState.Loading)

        viewModelScope.launch(Dispatchers.IO) {
            val resModel = SongDetailsResModel(null, null, null)
            when (val res = repository.getSongData(songId,resModel)) {
                is Success -> {
                    Log.d(TAG, "song data response : === ${res.value}")
                    _songData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _songData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }

    private val _addRemoveFavoriteSong: MutableLiveData<ViewState<AddRemoveFavoriteSongApi>> = MutableLiveData(ViewState.DefaultState)
    val addRemoveFavoriteSong = _addRemoveFavoriteSong
    fun addRemoveFavoriteSong(songId: Int, isFavorite: Int){
        _addRemoveFavoriteSong.postValue(ViewState.Loading)

        viewModelScope.launch(Dispatchers.IO) {
            val resModel = AddRemoveFavoriteSongApi(null, null, null)
            when (val res = repository.addRemoveFavorite(songId, isFavorite, resModel)) {
                is Success -> {
                    Log.d(TAG, "add remove favorite music response : ===${res.value}")
                    _addRemoveFavoriteSong.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _addRemoveFavoriteSong.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }
}