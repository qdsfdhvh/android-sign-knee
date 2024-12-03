plugins {
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    id("io.deepmedia.tools.knee") version "1.2.0"
    id("com.github.gmazzo.buildconfig") version "5.5.1"
}

kotlin {
    androidTarget()
    androidNativeArm64 {
        val curlDir = rootProject.layout.buildDirectory.dir("android/curl/install/arm64").get().asFile
        val opensslDir = rootProject.layout.buildDirectory.dir("android/openssl/install/arm64").get().asFile
        val zlibDir = rootProject.layout.buildDirectory.dir("android/zlib/install/arm64").get().asFile

        compilations["main"].apply {
            cinterops {
                create("libcurl") {
                    defFile("src/nativeInterop/cinterop/libcurl.def")
                    includeDirs(curlDir.resolve("include"))
                }
            }
        }

        binaries.all {
            linkerOpts("-L${curlDir.resolve("lib").absolutePath}", "-lcurl")
            linkerOpts("-L${opensslDir.resolve("lib").absolutePath}", "-lssl", "-lcrypto")
            linkerOpts("-L${zlibDir.resolve("lib").absolutePath}", "-lz")
        }
    }

    applyDefaultHierarchyTemplate()
    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.experimental.ExperimentalNativeApi")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
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
            dependencies {
                implementation(libs.kotlinx.io.core)
                implementation(libs.ktor.io)
                implementation(libs.ktor.utils)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.cio)
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
        consumerProguardFiles("consumer-rules.pro")
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

buildConfig {
    packageName = "com.seiko.example.sign"
    sourceSets.named("androidNativeMain") {
        useKotlinOutput {
            topLevelConstants = true
        }

        buildConfigField("targetAppSignSha256", "84f0f24a97d21adf8460ae597fbfcedb8b6225af413d54c24fd78c5566ee0271".toCharArray())
        buildConfigField("DEBUG", true) // TODO: auto detect debug/release build environment
    }
}

