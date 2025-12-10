package com.zenimmersive.android.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenimmersive.android.base.BaseResponse
import com.zenimmersive.android.base.Failure
import com.zenimmersive.android.base.Loading
import com.zenimmersive.android.base.Success
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.helper.CommonUtils
import com.zenimmersive.android.repository.SubscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SubscriptionViewModel(private val repository: SubscriptionRepository) : ViewModel() {
    private val TAG = SubscriptionViewModel::class.java.simpleName

    private val _verifySubscriptionRes: MutableLiveData<ViewState<BaseResponse>> =
        MutableLiveData(ViewState.DefaultState)
    val verifySubscriptionRes = _verifySubscriptionRes


    fun verifyMusicPackPurchase(
        songId: Int,
        productId: String,
        purchaseToken: String,
        price: String,
        orderId: String
    ) {
        _verifySubscriptionRes.postValue(ViewState.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            val resModel = BaseResponse()
            when (val res = repository.handleUserPurchase(
                productId,
                1,
                CommonUtils.getUtcTime(),
                1,
                songId,
                purchaseToken,
                orderId,
                price,
                resModel
            )) {
                is Success -> {
                    Log.d(TAG, "verify subscription response : ===${res.value}")
                    _verifySubscriptionRes.postValue(ViewState.Data(res.value))
                }

                is Failure -> {
                    _verifySubscriptionRes.postValue(
                        ViewState.Error(
                            res.errorMessage,
                            res.successBody
                        )
                    )
                }

                is Loading -> {
                }
            }
        }
    }

    fun verifySubscriptionPackPurchase(
        purchasedPackId: String,
        purchasedPackType: Int,
        purchasePackDuration: Int,
        purchaseToken: String,
        price: String, orderId: String
    ) {
        _verifySubscriptionRes.postValue(ViewState.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            val resModel = BaseResponse()
            when (val res = repository.handleUserPurchase(
                purchasedPackId,
                purchasedPackType,
                CommonUtils.getUtcTime(),
                purchasePackDuration,
                0,
                purchaseToken,
                orderId,
                price,
                resModel
            )) {
                is Success -> {
                    Log.d(TAG, "verify subscription pack purchase response : ===${res.value}")
                    _verifySubscriptionRes.postValue(ViewState.Data(res.value))
                }

                is Failure -> {
                    _verifySubscriptionRes.postValue(
                        ViewState.Error(
                            res.errorMessage,
                            res.successBody
                        )
                    )
                }

                is Loading -> {
                }
            }
        }
    }
} 