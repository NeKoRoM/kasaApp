package dev.androidbroadcast.frameio

import dev.androidbroadcast.frameio.utils.androidConfig
import dev.androidbroadcast.frameio.utils.applyIfNeeded
import dev.androidbroadcast.frameio.utils.libs

plugins.applyIfNeeded(libs.plugins.jetbrains.compose.compiler.get().pluginId)

androidConfig {
    buildFeatures {
        compose = true
    }
}
