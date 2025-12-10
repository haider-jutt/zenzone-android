package com.zenimmersive.android.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenimmersive.android.apiresponsemodel.VipSongsResModel
import com.zenimmersive.android.base.Failure
import com.zenimmersive.android.base.Loading
import com.zenimmersive.android.base.Success
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.repository.VipMusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VipMusicViewModel(private val repository: VipMusicRepository): ViewModel() {
    private val TAG = VipMusicViewModel::class.java.simpleName

    private val _vipSongs: MutableLiveData<ViewState<VipSongsResModel>> = MutableLiveData(ViewState.DefaultState)
    val vipSongs = _vipSongs

    fun getVipSongs(){
        _vipSongs.postValue(ViewState.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            val resModel = VipSongsResModel(null, null, null)
            when (val res = repository.getVipMusics(resModel)) {
                is Success -> {
                    Log.d(TAG, "get vip songs response : ===${res.value}")
                    _vipSongs.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _vipSongs.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }

}