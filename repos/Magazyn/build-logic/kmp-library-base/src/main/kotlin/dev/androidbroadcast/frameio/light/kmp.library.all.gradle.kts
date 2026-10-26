package dev.androidbroadcast.frameio.light

import com.android.build.api.dsl.androidLibrary
import dev.androidbroadcast.frameio.utils.ProjectTargets
import dev.androidbroadcast.frameio.utils.applyIfNeeded
import dev.androidbroadcast.frameio.utils.config.IOSPlatform
import dev.androidbroadcast.frameio.utils.config.kmpIosPlatforms
import dev.androidbroadcast.frameio.utils.jvmTarget
import dev.androidbroadcast.frameio.utils.kmpAndroidLibComponentConfig
import dev.androidbroadcast.frameio.utils.kmpConfig
import dev.androidbroadcast.frameio.utils.libs
import org.gradle.internal.extensions.stdlib.capitalized

plugins.applyIfNeeded(libs.plugins.jetbrains.kotlin.multiplatform.get().pluginId)
plugins.applyIfNeeded(libs.plugins.android.libraryKmp.get().pluginId)

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

    androidLibrary {
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        buildToolsVersion = libs.versions.android.buildtools.get()
        minSdk = libs.versions.android.minSdk.get().toInt()

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }

        compilerOptions {
            jvmTarget.set(libs.jvmTarget(ProjectTargets.Android))
        }
    }

    jvm("desktop") {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions.jvmTarget.set(libs.jvmTarget(ProjectTargets.Desktop))
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core)
            implementation(libs.kotlinx.coroutines.android)

            implementation(libs.koin.android)
        }

        named("nonAndroidMain")
    }

    kmpIosPlatforms.asSequence()
        .map {
            when (it) {
                IOSPlatform.ARM_64 -> iosArm64()
                IOSPlatform.SIMULATOR_ARM64 -> iosSimulatorArm64()
                IOSPlatform.SIMULATOR_X64 -> iosX64()
            }
        }.forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = project.name
                isStatic = true
            }
        }

    compilerOptions {
        optIn.addAll(
            "org.koin.core.annotation.KoinExperimentalAPI",
            "kotlin.time.ExperimentalTime",
        )
        freeCompilerArgs.add("-Xexpect-actual-classes")

        // Code running from IDEA/Android Studio
        if (System.getProperty("idea.active") == "true") {
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

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test.commonn)
            implementation(libs.kotlinx.coroutines.test)

            implementation(libs.koin.test)
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

tasks.register("compileKotlinIosAll") {
    group = "build"
    description = "Compiles klib for all iOS targets"
    val kmpIosPlatforms = kmpIosPlatforms
    enabled = kmpIosPlatforms.isNotEmpty()
    kmpIosPlatforms.forEach { iosPlatform ->
        when (iosPlatform) {
            IOSPlatform.ARM_64 -> dependsOn(tasks.named("compileKotlinIosArm64"))
            IOSPlatform.SIMULATOR_ARM64 -> dependsOn(tasks.named("compileKotlinIosSimulatorArm64"))
            IOSPlatform.SIMULATOR_X64 -> dependsOn(tasks.named("compileKotlinIosX64"))
        }
    }
}

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
    kmpAndroidLibComponentConfig {
        onVariants { variant ->
            val variantCapitalized = variant.name.capitalized()
            tasks.named("ksp${variantCapitalized}") {
                mustRunAfter(
                    "process${variantCapitalized}Manifest",
                    "write${variantCapitalized}AarMetadata",
                    "merge${variantCapitalized}JniLibFolders",
                    "merge${variantCapitalized}NativeLibs",
                    "copy${variantCapitalized}JniLibsProjectOnly",
                    "generate${variantCapitalized}EmptyResourceFiles",
                    "prepare${variantCapitalized}ArtProfile",
                    "extractProguardFiles",
                    "prepareLintJarForPublish",
                    "extract${variantCapitalized}Annotations",
                    "check${variantCapitalized}AarMetadata",
                )
            }
        }
    }

    afterEvaluate {
        // Fixing running check tasks in IDEs
        tasks.named("kspKotlinDesktop") {
            dependsOn(tasks.named("kspAndroidMain"))
            dependsOn(tasks.named("compileAndroidMain"))
            dependsOn(tasks.named("bundleAndroidMainClassesToCompileJar"))
            dependsOn(tasks.named("processAndroidMainJavaRes"))
            dependsOn(tasks.named("bundleAndroidMainClassesToRuntimeJar"))
        }

        if (IOSPlatform.ARM_64 in kmpIosPlatforms) {
            tasks.named("kspAndroidMain") {
                dependsOn(tasks.named("kspKotlinIosX64"))
                dependsOn(tasks.named("compileKotlinIosX64"))
                dependsOn(tasks.named("linkDebugTestIosX64"))
                dependsOn(tasks.named("compileTestKotlinIosX64"))
            }

            if (IOSPlatform.SIMULATOR_ARM64 in kmpIosPlatforms) {
                tasks.named("kspKotlinIosX64") {
                    dependsOn(tasks.named("kspKotlinIosSimulatorArm64"))
                    dependsOn(tasks.named("compileKotlinIosSimulatorArm64"))
                    dependsOn(tasks.named("linkDebugTestIosSimulatorArm64"))
                    dependsOn(tasks.named("compileTestKotlinIosSimulatorArm64"))
                }
            }
        }

        if (IOSPlatform.SIMULATOR_ARM64 in kmpIosPlatforms) {
            tasks.named("kspKotlinIosSimulatorArm64") {
                dependsOn(tasks.named("generateAndroidMainEmptyResourceFiles"))
                dependsOn(tasks.named("processAndroidMainManifest"))
                dependsOn(tasks.named("writeAndroidMainAarMetadata"))
                dependsOn(tasks.named("iosSimulatorArm64ProcessResources"))
                dependsOn(tasks.named("iosX64ProcessResources"))
            }
        }
    }
}
