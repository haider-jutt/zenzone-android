package com.zenimmersive.android.ui.player

import android.annotation.SuppressLint

@SuppressLint("UnsafeOptInUsageError")
object LocalVideoPlayerPropertyManager {

    var lastInteractionTime: Long = -1L
    var isPlayButtonPressed = false

    var isLocalPlayerPageExpanded = false

    var isLyricDisplay = false

}
