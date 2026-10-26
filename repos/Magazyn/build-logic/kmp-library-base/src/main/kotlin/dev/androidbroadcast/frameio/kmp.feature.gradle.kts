package dev.androidbroadcast.frameio

import com.android.build.api.dsl.androidLibrary
import dev.androidbroadcast.frameio.utils.applyIfNeeded
import dev.androidbroadcast.frameio.utils.kmpConfig

plugins.applyIfNeeded("dev.androidbroadcast.frameio.light.kmp.library.all")
plugins.applyIfNeeded("dev.androidbroadcast.frameio.light.jetbrains-compose.all")

kmpConfig {
    androidLibrary {
        androidResources.enable = true
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:uikit"))
        }
    }
}
