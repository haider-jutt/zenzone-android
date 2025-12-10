package com.zenimmersive.android.base

/*
sealed class ResultState<out T: Any> {
    data class Success<out T: Any>(val data: T):ResultState<T>()
    object SuccessWithNoData:ResultState<Nothing>()
    data class Error(val error: String, val data: String? = null):ResultState<Nothing>()
}*/

sealed class ViewState<out T: Any>{
    object DefaultState: ViewState<Nothing>()
    object Loading: ViewState<Nothing>()
    data class  Data<out T: Any>(val data: T):ViewState<T>()
    data class Error<out T: Any>(val error:String, val data: T? = null):ViewState<T>()
}