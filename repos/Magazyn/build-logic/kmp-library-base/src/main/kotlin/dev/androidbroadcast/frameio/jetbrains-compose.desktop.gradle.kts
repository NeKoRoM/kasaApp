package dev.androidbroadcast.frameio

import dev.androidbroadcast.frameio.utils.applyIfNeeded
import dev.androidbroadcast.frameio.utils.composeExt
import dev.androidbroadcast.frameio.utils.kmpConfig
import org.gradle.kotlin.dsl.getting

plugins.applyIfNeeded("dev.androidbroadcast.frameio.jetbrains-compose.base")

kmpConfig {
    sourceSets {
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(composeExt.dependencies.desktop.common)
            implementation(composeExt.dependencies.desktop.currentOs)
        }
    }
}
