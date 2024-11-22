package com.seiko.example.sign

import io.deepmedia.tools.knee.runtime.JniEnvironment
import io.deepmedia.tools.knee.runtime.callObjectMethod
import io.deepmedia.tools.knee.runtime.deleteLocalRef
import io.deepmedia.tools.knee.runtime.getFieldId
import io.deepmedia.tools.knee.runtime.getMethodId
import io.deepmedia.tools.knee.runtime.getObjectArrayElement
import io.deepmedia.tools.knee.runtime.getObjectClass
import io.deepmedia.tools.knee.runtime.getObjectField
import io.deepmedia.tools.knee.runtime.getStringUTFChars
import io.deepmedia.tools.knee.runtime.newStringUTF
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.android.jclass
import platform.android.jobject
import platform.android.jstring
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName(externName = "Java_com_seiko_example_sign_Signs_nativeGetSignatureSha1")
fun nativeGetSignatureSha1(
    env: JniEnvironment,
    `_`: jclass,
    contextObject: jobject,
): jstring? {
    logd("helloWorld")

    val contextClass = env.getObjectClass(contextObject)

    // get PackageManager
    var methodId = env.getMethodId(contextClass, "getPackageManager", "()Landroid/content/pm/PackageManager;")
    val packageMangerObject = env.callObjectMethod(contextObject, methodId)
    if (packageMangerObject == null) {
        logd("packageManger is null")
        return null
    }
    logd("get PackageManager")

    // get PackageName
    methodId = env.getMethodId(contextClass, "getPackageName", "()Ljava/lang/String;")
    val packageName = env.callObjectMethod(contextObject, methodId)
    if (packageName == null) {
        logd("packageName is null")
        return null
    }
    env.deleteLocalRef(contextClass)
    logd("get packageName: ${env.getStringUTFChars(packageName).toKString()}")

    // get PackageInfo
    val packageManagerClass = env.getObjectClass(packageMangerObject)
    methodId = env.getMethodId(packageManagerClass, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;")
    val packageInfoObject = env.callObjectMethod(packageMangerObject, methodId, packageName, 64)
    if (packageInfoObject == null) {
        logd("packageInfo is null")
        return null
    }
    env.deleteLocalRef(packageManagerClass)
    logd("get packageInfo")

    // get Signature
    val packageInfoClass = env.getObjectClass(packageInfoObject)
    val fieldId = env.getFieldId(packageInfoClass, "signatures", "[Landroid/content/pm/Signature;")
    val signaturesObject = env.getObjectField(packageInfoObject, fieldId) as jobject
    val firstSignatureObject = env.getObjectArrayElement(signaturesObject, 0)
    env.deleteLocalRef(packageMangerObject)
    logd("get first signature")

    // get Signature.toCharsString
    val firstSignatureClass = env.getObjectClass(firstSignatureObject)
    methodId = env.getMethodId(firstSignatureClass, "toCharsString", "()Ljava/lang/String;")
    val signature = env.callObjectMethod(firstSignatureObject, methodId)!!
    env.deleteLocalRef(firstSignatureClass)

    val chars = env.getStringUTFChars(signature)
    logd("get signature: ${chars.toKString()}")

    return env.newStringUTF(chars)
}