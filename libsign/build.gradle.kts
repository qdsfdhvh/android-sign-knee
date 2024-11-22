plugins {
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
    // alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.library)
    id("io.deepmedia.tools.knee") version "1.2.0"
}

kotlin {
    androidTarget()
    androidNativeArm64()

    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        androidMain {
            dependencies {
            }
        }
        androidNativeMain {
            languageSettings {
                // optIn("kotlin.experimental.ExperimentalNativeApi")
                // optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
    }
    jvmToolchain(17)
}

android {
    namespace = "com.seiko.example.lib_sign"
    defaultConfig {
        compileSdk = 35
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

knee {
    enabled.set(true)
    verboseLogs.set(true)
    verboseRuntime.set(true)
    verboseSources.set(true)
}
