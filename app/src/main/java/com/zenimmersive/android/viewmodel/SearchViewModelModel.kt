package com.zenimmersive.android.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenimmersive.android.apiresponsemodel.AlbumDetailsResModel
import com.zenimmersive.android.apiresponsemodel.FilterResponseModel
import com.zenimmersive.android.apiresponsemodel.SearchSongsResModel
import com.zenimmersive.android.apiresponsemodel.TagListResModel
import com.zenimmersive.android.base.Failure
import com.zenimmersive.android.base.Loading
import com.zenimmersive.android.base.Success
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchViewModelModel(private val repository: SearchRepository): ViewModel() {
    private val TAG = SearchViewModelModel::class.java.simpleName
    private val _tagsListData: MutableLiveData<ViewState<TagListResModel>> = MutableLiveData(ViewState.DefaultState)
    val tagsListData = _tagsListData

    fun getTagList(){
        _tagsListData.postValue(ViewState.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            val resModel = TagListResModel(null, null, null)
            when (val res = repository.getTags(resModel)) {
                is Success -> {
                    Log.d(TAG, "tags list response : ===${res.value}")
                    _tagsListData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _tagsListData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }

    private val _tagWiseMusicData: MutableLiveData<ViewState<AlbumDetailsResModel>> = MutableLiveData(ViewState.DefaultState)
    val tagWiseMusicData = _tagWiseMusicData

    fun getTagsSongs(tagId: Int){
        _tagWiseMusicData.postValue(ViewState.Loading)

        viewModelScope.launch(Dispatchers.IO) {
            val resModel = AlbumDetailsResModel(null,null,null)
            when (val res = repository.getTagsSongs(tagId, resModel)) {
                is Success -> {
                    Log.d(TAG, "tag wise music response : ===${res.value}")
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

    private val _searchSongsData: MutableLiveData<ViewState<SearchSongsResModel>> = MutableLiveData(ViewState.DefaultState)
    val searchSongsData = _searchSongsData
    fun searchSongs(searchKeyword: String){
        _searchSongsData.postValue(ViewState.Loading)

        viewModelScope.launch(Dispatchers.IO) {
            val resModel = SearchSongsResModel(null,null,null)
            when (val res = repository.searchSongs(searchKeyword, resModel)) {
                is Success -> {
                    Log.d(TAG, "search music response : ===${res.value}")
                    _searchSongsData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _searchSongsData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }

    private val _filterSongsData: MutableLiveData<ViewState<FilterResponseModel>> = MutableLiveData(ViewState.DefaultState)
    val filterSongsData = _filterSongsData
    fun filterSongs(tagIds: String, fromDuration: String, toDuration: String){
        _filterSongsData.postValue(ViewState.Loading)

        viewModelScope.launch(Dispatchers.IO) {
            val resModel = FilterResponseModel(null,null,null)
            when (val res = repository.filterSongs(tagIds, fromDuration, toDuration, resModel)) {
                is Success -> {
                    Log.d(TAG, "filter music response : ===${res.value}")
                    _filterSongsData.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _filterSongsData.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }
}