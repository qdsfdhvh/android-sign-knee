package com.seiko.example.sign

import android.content.Context
import android.content.pm.PackageManager

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

private fun getSha1(context: Context) {
    val packageName = context.packageName

    val packageInfo = context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)

    val signatures = packageInfo.signatures
    val firstSignature = signatures!![0]

    firstSignature.toByteArray()
}