@file:OptIn(ExperimentalForeignApi::class)

package com.seiko.example.sign

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import libcurl.CURLE_OK
import libcurl.CURLOPT_CAINFO
import libcurl.CURLOPT_CAPATH
import libcurl.CURLOPT_COOKIEFILE
import libcurl.CURLOPT_COOKIEJAR
import libcurl.CURLOPT_COOKIELIST
import libcurl.CURLOPT_HEADERDATA
import libcurl.CURLOPT_HEADERFUNCTION
import libcurl.CURLOPT_SSL_VERIFYHOST
import libcurl.CURLOPT_SSL_VERIFYPEER
import libcurl.CURLOPT_URL
import libcurl.CURLOPT_WRITEDATA
import libcurl.CURLOPT_WRITEFUNCTION
import libcurl.curl_easy_cleanup
import libcurl.curl_easy_escape
import libcurl.curl_easy_init
import libcurl.curl_easy_perform
import libcurl.curl_easy_setopt
import libcurl.curl_easy_strerror
import libcurl.curl_free
import platform.posix.size_t

class KUrlError(message: String) : Error(message)

typealias HttpHandler = (String) -> Unit

private fun CPointer<ByteVar>.toKString(length: Int) = this.readBytes(length).toKString()

/**
 * fork: https://github.com/Kotlin/kotlinconf-spinner/blob/master/kurl/src/nativeMain/kotlin/kurl/KUrl.kt
 */
class KUrl(val cookies: String? = null) {
    var curl = curl_easy_init()

    fun escape(string: String) =
        curl_easy_escape(curl, string, 0)?.let {
            val result = it.toKString()
            curl_free(it)
            result
        } ?: ""

    fun fetch(
        url: String,
        options: Map<String, String>?,
        onData: HttpHandler,
        onHeader: HttpHandler?,
    ) {
        // curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 1L)    // default
        curl_easy_setopt(curl, CURLOPT_CAPATH, "/system/etc/security/cacerts")
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0)
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 0)

        val args =
            if (options != null)
                "?" + options.map { (key, value) -> "$key=${escape(value)}" }.joinToString("&")
            else
                ""
        curl_easy_setopt(curl, CURLOPT_URL, url + args)
        if (cookies != null) {
            curl_easy_setopt(curl, CURLOPT_COOKIEFILE, cookies)
            curl_easy_setopt(curl, CURLOPT_COOKIELIST, "RELOAD")
        }
        val stables = mutableListOf<StableRef<Any>>()
        val result = try {
            if (onHeader != null) {
                curl_easy_setopt(
                    curl,
                    CURLOPT_HEADERFUNCTION,
                    staticCFunction { buffer: CPointer<ByteVar>?, size: size_t, nitems: size_t, userdata: COpaquePointer? ->

                        if (buffer == null) return@staticCFunction 0.toLong()
                        val handler = userdata!!.asStableRef<HttpHandler>().get()
                        handler(buffer.toKString((size * nitems).toInt()).trim())
                        return@staticCFunction (size * nitems).toLong()
                    })
                val onHeaderStable = StableRef.create(onHeader)
                stables += onHeaderStable
                curl_easy_setopt(curl, CURLOPT_HEADERDATA, onHeaderStable.asCPointer())
            }

            curl_easy_setopt(
                curl,
                CURLOPT_WRITEFUNCTION,
                staticCFunction { buffer: CPointer<ByteVar>?, size: size_t, nitems: size_t, userdata: COpaquePointer? ->

                    if (buffer == null) return@staticCFunction 0.toLong()
                    val header = buffer.toKString((size * nitems).toInt())
                    val handler = userdata!!.asStableRef<HttpHandler>().get()
                    handler(header)
                    return@staticCFunction (size * nitems).toLong()
                })
            val onDataStable = StableRef.create(onData)
            stables += onDataStable
            curl_easy_setopt(curl, CURLOPT_WRITEDATA, onDataStable.asCPointer())

            curl_easy_perform(curl)
        } finally {
            stables.forEach {
                it.dispose()
            }
        }

        if (result != CURLE_OK)
            throw KUrlError(
                "curl_easy_perform() failed with code $result: ${
                    curl_easy_strerror(
                        result
                    )?.toKString() ?: ""
                }"
            )
    }

    fun close() {
        if (curl == null) return
        if (cookies != null)
            curl_easy_setopt(curl, CURLOPT_COOKIEJAR, cookies)
        curl_easy_cleanup(curl)
        curl = null
    }

    // So that we can use DSL syntax.
    fun fetch(url: String, options: Map<String, String>? = null, onData: HttpHandler) =
        fetch(url, options, onData, null)
}

inline fun <T> withUrl(url: KUrl, function: (KUrl) -> T): T =
    try {
        function(url)
    } finally {
        url.close()
    }