package dev.androidbroadcast.frameio

import dev.androidbroadcast.frameio.utils.ProjectTargets
import dev.androidbroadcast.frameio.utils.applyIfNeeded
import dev.androidbroadcast.frameio.utils.javaVersion
import dev.androidbroadcast.frameio.utils.jvmTarget
import dev.androidbroadcast.frameio.utils.kotlinJvmCompilerOptions
import dev.androidbroadcast.frameio.utils.libs
import org.gradle.kotlin.dsl.dependencies

plugins.apply("dev.androidbroadcast.frameio.android.base")
if (plugins.hasPlugin(libs.plugins.jetbrains.kotlin.multiplatform.get().pluginId)) {
    plugins.applyIfNeeded(libs.plugins.jetbrains.kotlin.android.get().pluginId)
}

project.dependencies {
    "implementation"(libs.kotlinx.coroutines.android)
    "implementation"(libs.androidx.core)
    "implementation"(libs.androidx.annotation)
}

kotlinJvmCompilerOptions {
    jvmTarget.set(libs.jvmTarget(ProjectTargets.Android))
    freeCompilerArgs.add("-Xjdk-release=${libs.javaVersion(ProjectTargets.Android)}")
    freeCompilerArgs.add("-Xexpect-actual-classes")
    freeCompilerArgs.add("-opt-in=org.koin.core.annotation.KoinExperimentalAPI")
}
