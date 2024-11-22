import io.deepmedia.tools.knee.runtime.JniEnvironment
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi

// @OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
// @CName(externName = "JNI_OnLoad")
// fun onLoad(vm: JavaVirtualMachine): Int {
//     vm.useEnv { io.deepmedia.tools.knee.runtime.initKnee(it) }
//     return 0x00010006 // JNI_VERSION_1_6
// }

// must run initKnee before use library
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName(externName = "Java_com_seiko_example_sign_Signs_initLibrary")
fun initLibrary(env: JniEnvironment) {
    io.deepmedia.tools.knee.runtime.initKnee(env)
}
