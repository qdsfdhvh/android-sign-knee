package com.seiko.example.sign

import android.content.Context

object Signs {
    external fun initLibrary()

    external fun nativeGetSignatureSha1(context: Context): String

    fun getSignatureSha1(context: Context): String {
        return nativeGetSignatureSha1(context)
    }

    init {
        System.loadLibrary("libsign")
    }
}