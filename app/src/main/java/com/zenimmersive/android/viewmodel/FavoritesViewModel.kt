package com.zenimmersive.android.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenimmersive.android.apiresponsemodel.FavoritesResModel
import com.zenimmersive.android.base.Failure
import com.zenimmersive.android.base.Loading
import com.zenimmersive.android.base.Success
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.repository.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoritesViewModel(private val repository: FavoritesRepository): ViewModel() {
    private val TAG = FavoritesViewModel::class.java.simpleName

    private val _favoriteSongs: MutableLiveData<ViewState<FavoritesResModel>> = MutableLiveData(ViewState.DefaultState)
    val favoriteSongsRes = _favoriteSongs

    fun getFavoriteSongs(){
        _favoriteSongs.postValue(ViewState.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            val resModel = FavoritesResModel(null, null, null)
            when (val res = repository.getFavoriteSongs(resModel)) {
                is Success -> {
                    Log.d(TAG, "getFavoriteSongs response : ===${res.value}")
                    _favoriteSongs.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _favoriteSongs.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }

}