package com.seiko.example.sign

import android.content.Context

object Signs {
    external fun initLibrary(context: Context)

    init {
        System.loadLibrary("libsign")
    }
}
