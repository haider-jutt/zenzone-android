package com.zenimmersive.android.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenimmersive.android.apiresponsemodel.OrderListResModel
import com.zenimmersive.android.base.Failure
import com.zenimmersive.android.base.Loading
import com.zenimmersive.android.base.Success
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.repository.MyOrdersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyOrdersViewModel(private val repository: MyOrdersRepository): ViewModel() {
    private val TAG = MyOrdersViewModel::class.java.simpleName

    private val _myOrdersListRes: MutableLiveData<ViewState<OrderListResModel>> = MutableLiveData(ViewState.DefaultState)
    val myOrdersListRes = _myOrdersListRes

    fun getMyOrders(){
        _myOrdersListRes.postValue(ViewState.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            val resModel = OrderListResModel(null, null, null)
            when (val res = repository.getMyOrders(resModel)) {
                is Success -> {
                    Log.d(TAG, "get my orders response : ===${res.value}")
                    _myOrdersListRes.postValue(ViewState.Data(res.value))
                }
                is Failure -> {
                    _myOrdersListRes.postValue(ViewState.Error(res.errorMessage, res.successBody))
                }
                is Loading -> {
                }
            }
        }
    }

}