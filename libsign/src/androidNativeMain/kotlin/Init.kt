import io.deepmedia.tools.knee.runtime.JniEnvironment
import io.deepmedia.tools.knee.runtime.newGlobalRef
import kotlinx.cinterop.ExperimentalForeignApi
import platform.android.jclass
import platform.android.jobject
import kotlin.experimental.ExperimentalNativeApi

// @OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
// @CName(externName = "JNI_OnLoad")
// fun onLoad(vm: JavaVirtualMachine): Int {
//     vm.useEnv { io.deepmedia.tools.knee.runtime.initKnee(it) }
//     return 0x00010006 // JNI_VERSION_1_6
// }

private var currentGlobalContextObject: jobject? = null

internal val globalContextObject: jobject
    get() = checkNotNull(currentGlobalContextObject) { "Context object is not initialized" }

// must run initKnee before use library
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName(externName = "Java_com_seiko_example_sign_Signs_initLibrary")
fun initLibrary(
    env: JniEnvironment,
    `_`: jclass,
    contextObject: jobject,
) {
    io.deepmedia.tools.knee.runtime.initKnee(env)
    currentGlobalContextObject = env.newGlobalRef(contextObject)
}
