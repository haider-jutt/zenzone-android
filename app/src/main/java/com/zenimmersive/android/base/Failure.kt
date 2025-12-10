package com.zenimmersive.android.base

import okhttp3.ResponseBody

class Failure<T> : Resource<T> {
    val isNetworkError: Boolean
    val errorCode: Int
    var errorBody: ResponseBody? = null
    var errorMessage = ""
    var successBody: T? = null

    constructor(
        isNetworkError: Boolean,
        errorCode: Int,
        errorBody: ResponseBody?
    ) : super(Status.FAIL) {
        this.isNetworkError = isNetworkError
        this.errorCode = errorCode
        this.errorBody = errorBody
    }

    constructor(
        isNetworkError: Boolean,
        errorCode: Int,
        errorMessage: String,
        successBody: T?
    ) : super(Status.FAIL) {
        this.isNetworkError = isNetworkError
        this.errorCode = errorCode
        this.errorMessage = errorMessage
        this.successBody = successBody
    }
}