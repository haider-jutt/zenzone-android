package com.zenimmersive.android.base

//class Success<T>(var value: T) : Resource<T>(Status.SUCCESS)
class Success<R>(var value: R) : Resource<R>(Status.SUCCESS)