@file:Suppress("OPT_IN_USAGE")

package dev.androidbroadcast.frameio

import dev.androidbroadcast.frameio.utils.ProjectTargets
import dev.androidbroadcast.frameio.utils.androidConfig
import dev.androidbroadcast.frameio.utils.androidLibComponentConfig
import dev.androidbroadcast.frameio.utils.applyIfNeeded
import dev.androidbroadcast.frameio.utils.jvmTarget
import dev.androidbroadcast.frameio.utils.kmpConfig
import dev.androidbroadcast.frameio.utils.libs
import org.gradle.internal.extensions.stdlib.capitalized

plugins.applyIfNeeded(libs.plugins.jetbrains.kotlin.multiplatform.get().pluginId)
plugins.applyIfNeeded(
    libs.plugins.android.library.get().pluginId,
    libs.plugins.android.application.get().pluginId,
)
plugins.applyIfNeeded("dev.androidbroadcast.frameio.kmp.library.base")

kmpConfig {
    applyDefaultHierarchyTemplate {
        common {
            group("nonAndroid") {
                withIos()
                withIosX64()
                withIosArm64()
                withIosSimulatorArm64()
                withNative()
                withJvm()
            }
        }
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(libs.jvmTarget(ProjectTargets.Android))
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core)
            implementation(libs.kotlinx.coroutines.android)

            implementation(libs.koin.android)
        }

        named("nonAndroidMain")

        val androidUnitTest by getting
        androidUnitTest.dependencies {
            implementation(libs.kotlin.test.junit)
            implementation(libs.junit)
        }
    }
}

androidConfig {
    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
    }
}

plugins.apply("dev.androidbroadcast.frameio.android.base")


// Fix for KSP tasks not depending on AGP tasks
if (plugins.hasPlugin(libs.plugins.google.ksp.get().pluginId)) {
    fixAndroidTaskOrderingForKsp()
} else {
    plugins.whenPluginAdded {
        if (plugins.hasPlugin(libs.plugins.google.ksp.get().pluginId)) {
            fixAndroidTaskOrderingForKsp()
        }
    }
}

private fun fixAndroidTaskOrderingForKsp() {
    androidLibComponentConfig {
        onVariants(selector().all()) { variant ->
            val variantCapitalized = variant.name.capitalized()
            tasks.named("ksp${variantCapitalized}KotlinAndroid") {
                mustRunAfter(
                    "${variant.name}AssetsCopyForAGP",
                    "process${variantCapitalized}Manifest",
                    "write${variantCapitalized}AarMetadata",
                    "javaPreCompile${variantCapitalized}",
                    "merge${variantCapitalized}Assets",
                    "merge${variantCapitalized}JniLibFolders",
                    "merge${variantCapitalized}NativeLibs",
                    "copy${variantCapitalized}JniLibsProjectOnly",
                    "generate${variantCapitalized}EmptyResourceFiles",
                    "copy${variantCapitalized}JniLibsProjectAndLocalJars",
                    "prepare${variantCapitalized}ArtProfile",
                    "write${variantCapitalized}LintModelMetadata",
                    "extractProguardFiles",
                    "prepareLintJarForPublish",
                )
            }
        }
    }

}
