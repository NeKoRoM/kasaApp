package dev.androidbroadcast.frameio

import dev.androidbroadcast.frameio.utils.androidConfig
import dev.androidbroadcast.frameio.utils.applyIfNeeded
import dev.androidbroadcast.frameio.utils.composeExt
import dev.androidbroadcast.frameio.utils.kmpConfig
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.invoke

plugins.applyIfNeeded("dev.androidbroadcast.frameio.jetbrains-compose.base")
plugins.apply("dev.androidbroadcast.frameio.jetpack-compose.base")

kmpConfig {
    androidConfig {
        buildFeatures {
            compose = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(composeExt.dependencies.preview)
        }
    }
}

dependencies {
    "debugImplementation"(composeExt.dependencies.uiTooling)
}
