package dev.androidbroadcast.frameio

import dev.androidbroadcast.frameio.utils.ProjectTargets
import dev.androidbroadcast.frameio.utils.applyIfNeeded
import dev.androidbroadcast.frameio.utils.jvmTarget
import dev.androidbroadcast.frameio.utils.kmpConfig
import dev.androidbroadcast.frameio.utils.libs
import org.gradle.kotlin.dsl.getting

plugins.applyIfNeeded(libs.plugins.jetbrains.kotlin.multiplatform.get().pluginId)
plugins.applyIfNeeded("dev.androidbroadcast.frameio.kmp.library.base")

kmpConfig {
    jvm("desktop") {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions.jvmTarget.set(libs.jvmTarget(ProjectTargets.Desktop))
            }
        }
    }

    sourceSets {
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }

        val desktopTest by getting
        desktopTest.dependencies {
            implementation(libs.kotlin.test.junit)
        }
    }
}

