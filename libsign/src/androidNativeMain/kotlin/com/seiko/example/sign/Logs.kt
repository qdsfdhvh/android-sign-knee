package com.seiko.example.sign

import platform.android.ANDROID_LOG_DEBUG
import platform.android.__android_log_print

internal fun logd(message: () -> String) {
    __android_log_print(ANDROID_LOG_DEBUG.toInt(), "LibSign", message())
}
