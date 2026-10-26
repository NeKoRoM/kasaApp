package dev.androidbroadcast.frameio.light

import dev.androidbroadcast.frameio.utils.applyIfNeeded
import dev.androidbroadcast.frameio.utils.composeExt
import dev.androidbroadcast.frameio.utils.kmpConfig
import dev.androidbroadcast.frameio.utils.libs

plugins.applyIfNeeded(libs.plugins.jetbrains.compose.compiler.get().pluginId)
plugins.applyIfNeeded(libs.plugins.jetbrains.compose.multiplatform.get().pluginId)

kmpConfig {
    sourceSets {
        commonMain.dependencies {
            implementation(composeExt.dependencies.components.resources)
            implementation(composeExt.dependencies.components.uiToolingPreview)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.koin.core.viewmodel)
            implementation(libs.koin.compose)
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(composeExt.dependencies.desktop.common)
            implementation(composeExt.dependencies.desktop.currentOs)
        }

        androidMain.dependencies {
            implementation(composeExt.dependencies.preview)
        }
    }
}
