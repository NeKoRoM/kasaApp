package dev.androidbroadcast.frameio

import dev.androidbroadcast.frameio.utils.applyIfNeeded
import dev.androidbroadcast.frameio.utils.kmpConfig
import dev.androidbroadcast.frameio.utils.libs

plugins.applyIfNeeded(libs.plugins.jetbrains.kotlin.multiplatform.get().pluginId)

kmpConfig {
    compilerOptions {
        optIn.addAll(
            "org.koin.core.annotation.KoinExperimentalAPI",
            "kotlin.time.ExperimentalTime",
        )
        freeCompilerArgs.add("-Xexpect-actual-classes")

        // Code running from IDEA/Android Studio
        if (System.getProperty("idea.active") == "true"){
            // Turn on debug mode
            freeCompilerArgs = listOf("-Xdebug")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.annotation)
            implementation(libs.kotlinx.datetime)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test.commonn)
            implementation(libs.kotlinx.coroutines.test)

            implementation(libs.koin.test)
        }
    }
}
