package dev.androidbroadcast.frameio.utils.config

import org.gradle.api.Project

/**
 * ====================================================================================================
 * ПАРАМЕТРИ КОНФІГУРАЦІЇ ANDROID NDK ABI (ANDROID NDK ABI CONFIG PARAMS)
 * ====================================================================================================
 *
 * Керує фільтрацією архітектур процесорів (armeabi-v7a, arm64-v8a, x86, x86_64) для швидкого локального білду.
 * Зчитує параметри з командного рядка (`-Pandroid.ndk.abis`), `local.properties` або `ANDROID_NDK_ABIS`.
 */

internal val requestedAndroidAbisParam = ProjectConfigParam(
    cmdParamName = "android.ndk.abis",
    androidLocalPropertyParamName = "android.ndk.abis",
    envParamName = "ANDROID_NDK_ABIS",
)

/**
 * Список запитаних ABI для поточної збірки або `null`, якщо використовуються стандартні налаштування.
 */
val Project.requestedAndroidAbis: List<String>?
    get() = readConfigParam(requestedAndroidAbisParam)?.split(",")

