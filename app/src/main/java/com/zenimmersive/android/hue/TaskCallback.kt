package com.zenimmersive.android.hue

interface TaskCallback<T> {
    fun onTaskComplete(result: T)
    fun onTaskError(error: String? = "No Error Message")
}