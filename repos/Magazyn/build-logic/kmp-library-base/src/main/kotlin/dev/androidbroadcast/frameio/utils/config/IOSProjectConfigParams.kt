package dev.androidbroadcast.frameio.utils.config

import org.gradle.api.Project

/**
 * ====================================================================================================
 * ПАРАМЕТРИ КОНФІГУРАЦІЇ ЦІЛЬОВИХ АРХІТЕКТУР IOS (KMP IOS CONFIG PARAMS)
 * ====================================================================================================
 *
 * Визначає підтримувані архітектури для компіляції під Apple iOS (arm64, simulator arm64, x64).
 * Дозволяє перевизначати список таргетів через командний рядок (`-Pkmp.ios.platforms`),
 * `local.properties` або змінні середовища `KMP_IOS_PLATFORMS`.
 */

internal val supportedIOSTargetsParam = ProjectConfigParam(
    cmdParamName = "kmp.ios.platforms",
    androidLocalPropertyParamName = "kmp.ios.platforms",
    envParamName = "KMP_IOS_PLATFORMS",
    defaultValue = "arm64,simulatorArmX64,simulatorIntelX64",
)

/**
 * Перелік підтримуваних платформ та симуляторів Apple iOS.
 */
enum class IOSPlatform(
    val value: String,
) {
    ARM_64("arm64"),
    SIMULATOR_ARM64("simulatorArmX64"),
    SIMULATOR_X64("simulatorIntelX64"),
    ;

    companion object {
        /**
         * За замовчуванням iOS-таргети вмикаються лише при складанні на хості macOS.
         */
        fun default(): List<IOSPlatform> {
            val currentOS = System.getProperty("os.name")
            return if (currentOS.contains("mac", ignoreCase = true)) values().toList() else emptyList()
        }
    }
}

/**
 * Список активних iOS платформ для поточного проекту Gradle.
 */
val Project.kmpIosPlatforms: List<IOSPlatform>
    get() {
        val supportedIOSTargetsParamValue = readConfigParam(supportedIOSTargetsParam)
        if (supportedIOSTargetsParamValue == "none") {
            return emptyList()
        }

        return supportedIOSTargetsParamValue?.split(",")?.map { paramValue ->
            IOSPlatform.values().first { it.value == paramValue }
        } ?: IOSPlatform.default()
    }

