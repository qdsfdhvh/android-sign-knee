package com.seiko.example.sign

import android.content.Context
import android.content.pm.PackageManager

object Signs {
    external fun initLibrary()

    external fun getSignatureSha1(context: Context): String

    init {
        System.loadLibrary("libsign")
    }
}
