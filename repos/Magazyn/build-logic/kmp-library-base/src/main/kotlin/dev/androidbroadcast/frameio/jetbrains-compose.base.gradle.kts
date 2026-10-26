package dev.androidbroadcast.frameio

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
    }
}
