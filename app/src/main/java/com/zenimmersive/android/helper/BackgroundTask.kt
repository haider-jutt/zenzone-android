package com.zenimmersive.android.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class BackgroundTask {
    companion object {
        @JvmStatic
        fun <T> execute(caller: String, listener: Listener<T>) {
            CoroutineScope(Dispatchers.IO).launch {
                var data = listener.doWork()

                CoroutineScope(Dispatchers.Main).launch {
                    listener.onTaskDone(data)
                }
            }
        }
    }

    interface Listener<T> {
        fun doWork(): T
        fun onTaskDone(result: T)
    }
}