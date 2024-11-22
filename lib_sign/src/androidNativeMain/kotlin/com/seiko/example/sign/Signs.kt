package com.seiko.example.sign

import io.deepmedia.tools.knee.annotations.Knee
import io.deepmedia.tools.knee.runtime.currentJavaVirtualMachine
import kotlinx.cinterop.ExperimentalForeignApi
import platform.android.ANDROID_LOG_WARN
import platform.android.__android_log_print

@OptIn(ExperimentalForeignApi::class)
@Knee
fun helloWorld() {
    __android_log_print(ANDROID_LOG_WARN.toInt(), "Sample", "Hello")
    __android_log_print(ANDROID_LOG_WARN.toInt(), "Sample", "Hello $currentJavaVirtualMachine")
}
