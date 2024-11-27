package com.seiko.example.sign

import globalContextObject
import io.deepmedia.tools.knee.annotations.Knee
import io.deepmedia.tools.knee.runtime.callObjectMethod
import io.deepmedia.tools.knee.runtime.callStaticObjectMethod
import io.deepmedia.tools.knee.runtime.currentJavaVirtualMachine
import io.deepmedia.tools.knee.runtime.deleteLocalRef
import io.deepmedia.tools.knee.runtime.findClass
import io.deepmedia.tools.knee.runtime.getArrayLength
import io.deepmedia.tools.knee.runtime.getFieldId
import io.deepmedia.tools.knee.runtime.getMethodId
import io.deepmedia.tools.knee.runtime.getObjectArrayElement
import io.deepmedia.tools.knee.runtime.getObjectClass
import io.deepmedia.tools.knee.runtime.getObjectField
import io.deepmedia.tools.knee.runtime.getPrimitiveArrayCritical
import io.deepmedia.tools.knee.runtime.getStaticMethodId
import io.deepmedia.tools.knee.runtime.getStringUTFChars
import io.deepmedia.tools.knee.runtime.newObject
import io.deepmedia.tools.knee.runtime.newStringUTF
import io.deepmedia.tools.knee.runtime.releasePrimitiveArrayCritical
import io.deepmedia.tools.knee.runtime.useEnv
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toKString
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.android.jbyteArray
import platform.android.jobject

private val targetAppSignSha256 = "84f0f24a97d21adf8460ae597fbfcedb8b6225af413d54c24fd78c5566ee0271".toCharArray()

private val hexCodes = charArrayOf(
    '0', '1', '2', '3', '4', '5', '6', '7',
    '8', '9', 'a', 'b', 'c', 'd', 'e', 'f',
)

@Knee
fun isSignatureValid(): Boolean = currentJavaVirtualMachine.useEnv { env ->
    logd { "helloWorld" }

    val contextObject = globalContextObject

    val contextClass = env.getObjectClass(contextObject)

    // get PackageManager
    var methodId = env.getMethodId(
        contextClass,
        "getPackageManager",
        "()Landroid/content/pm/PackageManager;",
    )
    val packageMangerObject = env.callObjectMethod(contextObject, methodId)
    if (packageMangerObject == null) {
        logd { "packageManger is null" }
        return false
    }
    logd { "get packageManager" }

    // get PackageName
    methodId = env.getMethodId(contextClass, "getPackageName", "()Ljava/lang/String;")
    val packageName = env.callObjectMethod(contextObject, methodId)
    if (packageName == null) {
        logd { "packageName is null" }
        return false
    }
    env.deleteLocalRef(contextClass)
    logd { "get packageName: ${env.getStringUTFChars(packageName).toKString()}" }

    // get PackageInfo
    val packageManagerClass = env.getObjectClass(packageMangerObject)
    methodId = env.getMethodId(
        packageManagerClass,
        "getPackageInfo",
        "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;",
    )
    env.deleteLocalRef(packageManagerClass)
    val packageInfoObject = env.callObjectMethod(packageMangerObject, methodId, packageName, 64)
    if (packageInfoObject == null) {
        logd { "packageInfo is null" }
        return false
    }
    logd { "get packageInfo" }

    // get Signature
    val packageInfoClass = env.getObjectClass(packageInfoObject)
    val fieldId = env.getFieldId(packageInfoClass, "signatures", "[Landroid/content/pm/Signature;")
    env.deleteLocalRef(packageInfoClass)
    val signaturesObject = env.getObjectField(packageInfoObject, fieldId) as jobject
    val firstSignatureObject = env.getObjectArrayElement(signaturesObject, 0)
    env.deleteLocalRef(packageMangerObject)
    logd { "get first signature" }

    val firstSignatureClass = env.getObjectClass(firstSignatureObject)
    methodId = env.getMethodId(firstSignatureClass, "toByteArray", "()[B")
    env.deleteLocalRef(firstSignatureClass)
    val signatureByte = env.callObjectMethod(firstSignatureObject, methodId) as jobject
    logd { "get signature byte" }

    val byteArrayInputStreamClass = env.findClass("java/io/ByteArrayInputStream")
    methodId = env.getMethodId(byteArrayInputStreamClass, "<init>", "([B)V")
    val byteArrayInputStream = env.newObject(byteArrayInputStreamClass, methodId, signatureByte)
    env.deleteLocalRef(byteArrayInputStreamClass)
    logd { "get byte array input stream" }

    val certificateFactoryClass = env.findClass("java/security/cert/CertificateFactory")
    methodId = env.getStaticMethodId(
        certificateFactoryClass,
        "getInstance",
        "(Ljava/lang/String;)Ljava/security/cert/CertificateFactory;",
    )
    val x509String = env.newStringUTF("X.509")
    val certFactory =
        env.callStaticObjectMethod(certificateFactoryClass, methodId, x509String) as jobject
    methodId = env.getMethodId(
        certificateFactoryClass,
        "generateCertificate",
        "(Ljava/io/InputStream;)Ljava/security/cert/Certificate;",
    )
    val x509Cert = env.callObjectMethod(certFactory, methodId, byteArrayInputStream) as jobject
    env.deleteLocalRef(certificateFactoryClass)
    logd { "get x509 cert" }

    val x509CertClass = env.getObjectClass(x509Cert)
    methodId = env.getMethodId(x509CertClass, "getEncoded", "()[B")
    val certByte = env.callObjectMethod(x509Cert, methodId) as jobject
    env.deleteLocalRef(x509CertClass)
    logd { "get cert byte" }

    val messageDigestClass = env.findClass("java/security/MessageDigest")
    methodId = env.getStaticMethodId(
        messageDigestClass,
        "getInstance",
        "(Ljava/lang/String;)Ljava/security/MessageDigest;",
    )
    val sha256String = env.newStringUTF("SHA256")
    val sha256Digest = env.callStaticObjectMethod(messageDigestClass, methodId, sha256String) as jobject
    methodId = env.getMethodId(messageDigestClass, "digest", "([B)[B")
    val sha256JavaBytes = env.callObjectMethod(sha256Digest, methodId, certByte) as jbyteArray
    env.deleteLocalRef(messageDigestClass)
    logd { "get sha256 java bytes" }

    val sha256ByteSize = env.getArrayLength(sha256JavaBytes)
    logd { "sha256 byte size: $sha256ByteSize" }

    // convert jbyteArray to ByteArray
    val sha256BytesCritical = env.getPrimitiveArrayCritical(sha256JavaBytes)
    val sha256ByteArray = sha256BytesCritical.readBytes(sha256ByteSize)
    env.releasePrimitiveArrayCritical(sha256JavaBytes, sha256BytesCritical, 0)

    // get sha256 hex
    val hexSha1 = CharArray(sha256ByteSize * 2)
    for (i in 0 until sha256ByteSize) {
        hexSha1[2 * i] = hexCodes[(sha256ByteArray[i].toInt() and 0xFF) shr 4]
        hexSha1[2 * i + 1] = hexCodes[sha256ByteArray[i].toInt() and 0x0F]
    }

    logd { "sha256: ${hexSha1.concatToString()}" }

    val isMatch = hexSha1.contentEquals(targetAppSignSha256)
    logd { "isMatch: $isMatch" }

    return isMatch
}

@Knee
fun getTestBytes(array: ByteArray): ByteArray {
    return array + byteArrayOf(1, 2, 3, 4, 5)
}

@Knee
suspend fun testHttpRequest(): String {
    // https://f.m.suning.com/api/ct.do
    return suspendCancellableCoroutine { cont ->
        val kurl = KUrl()
        kurl.fetch("https://wanandroid.com/harmony/index/json", null, {
            if (cont.isActive) {
                cont.resume(it) { cause, _, _ ->
                    // logd("cancel")
                    // kurl.close()
                }
            }
        },  null)
    }
    // val client = HttpClient(CIO) {
    // }
    // return client.get("http://f.m.suning.com/api/ct.do").bodyAsText()
}
