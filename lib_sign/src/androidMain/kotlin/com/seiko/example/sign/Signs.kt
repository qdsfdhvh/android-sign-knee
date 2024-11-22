package com.seiko.example.sign

object Signs {
    external fun initLibrary()

    fun hello() {
        helloWorld()
    }

    init {
        System.loadLibrary("lib_sign")
    }
}