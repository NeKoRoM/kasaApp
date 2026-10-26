@file:Suppress("unused")

package dev.androidbroadcast.frameio.utils

/**
 * ====================================================================================================
 * ТАКСОНОМІЯ ЦІЛЬОВИХ ПЛАТФОРМ ПРОЕКТУ (KMP PROJECT TARGETS)
 * ====================================================================================================
 *
 * Типізована ієрархія цільових компіляційних платформ у конвенційних плагінах KMP:
 * - [JvmTarget]: JVM платформи (Desktop, Android)
 * - [Desktop]: настільні застосунки (JVM / Compose Desktop)
 * - [Android]: мобільні пристрої Android
 * - [IOS]: Apple iOS (фізичні пристрої ARM64 та симулятори ARM64 / x64)
 */
sealed interface ProjectTargets {
    sealed interface JvmTarget

    object Desktop : ProjectTargets, JvmTarget

    object Android : ProjectTargets, JvmTarget

    sealed interface IOS : ProjectTargets {
        sealed interface Simulator

        object SimulatorX64 : IOS, Simulator

        object SimulatorArm64 : IOS, Simulator

        object ARM64 : IOS
    }
}

