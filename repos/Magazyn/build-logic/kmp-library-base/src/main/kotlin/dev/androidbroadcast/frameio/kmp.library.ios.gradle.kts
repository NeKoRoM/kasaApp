package dev.androidbroadcast.frameio

import dev.androidbroadcast.frameio.utils.applyIfNeeded
import dev.androidbroadcast.frameio.utils.config.IOSPlatform
import dev.androidbroadcast.frameio.utils.config.kmpIosPlatforms
import dev.androidbroadcast.frameio.utils.kmpConfig
import dev.androidbroadcast.frameio.utils.libs

plugins.applyIfNeeded(libs.plugins.jetbrains.kotlin.multiplatform.get().pluginId)
plugins.applyIfNeeded("dev.androidbroadcast.frameio.kmp.library.base")

print("Supported iOS targets: ${kmpIosPlatforms.asSequence()}")

kmpConfig {
    kmpIosPlatforms.asSequence()
        .map {
            when (it) {
                IOSPlatform.ARM_64 -> iosArm64()
                IOSPlatform.SIMULATOR_ARM64 -> iosSimulatorArm64()
                IOSPlatform.SIMULATOR_X64 -> iosX64()
            }
        }.forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = project.name
                isStatic = true
            }
        }
}

tasks.register("compileKotlinIosAll") {
    group = "build"
    description = "Compiles klib for all iOS targets"
    val kmpIosPlatforms = kmpIosPlatforms
    enabled = kmpIosPlatforms.isNotEmpty()
    kmpIosPlatforms.forEach { iosPlatform ->
        when (iosPlatform) {
            IOSPlatform.ARM_64 -> dependsOn(tasks.named("compileKotlinIosArm64"))
            IOSPlatform.SIMULATOR_ARM64 -> dependsOn(tasks.named("compileKotlinIosSimulatorArm64"))
            IOSPlatform.SIMULATOR_X64 -> dependsOn(tasks.named("compileKotlinIosX64"))
        }
    }
}
