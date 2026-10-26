import org.gradle.kotlin.dsl.invoke
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose.multiplatform)
    alias(libs.plugins.jetbrains.compose.compiler)
    alias(libs.plugins.compose.hot.reload)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.google.ksp)

}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    jvm("desktop")




    sourceSets {
        val desktopMain by getting
        
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.ktor.client.engine.okhttp)

            implementation(compose.preview)



        }
        commonMain.dependencies {

            // This enables the necessary resource management tasks
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)

            // compose
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            //Lifecycle
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            //room
            api(libs.androidx.room.runtime)
            api(libs.androidx.sqlite.bundled)


            //Ktor
            implementation(libs.bundles.ktor)
            implementation(libs.kotlinx.serialization.json)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)


            // Logging
            implementation(libs.napier)

            // Settings/DataStore alternative
            implementation(libs.multiplatform.settings)

            // Decompose
            implementation(libs.decompose)
            implementation(libs.decompose.extensions.compose)

            //modules
            api(projects.core.uikit)


            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")




        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
        }
    }
}

android {
    namespace = "nekorom.com.kasaapp.compose"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    ksp(libs.androidx.room.compiler)

}



////
//plugins {
//    alias(libs.plugins.frameio.kmplib)
//    alias(libs.plugins.frameio.compose)
//}
//
//kotlin {
//    sourceSets {
//        commonMain.dependencies {
//            implementation(projects.frameSdk)
//            implementation(projects.features.auth.ui)
//            implementation(projects.navigation)
//            implementation(projects.featuresCommon)
//            implementation(libs.kotlinx.serialization.json)
//            implementation(projects.core.utils)
//            implementation(projects.core.userSession)
//            implementation(libs.androidx.lifecycle.viewmodel)
//            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
//            implementation(libs.koin.compose.viewmodel)
//            implementation(libs.jetbrains.compose.ui.backhandler)
//        }
//    }
//}
//
//android {
//    namespace = "dev.androidbroadcast.frameio.composeapp"
//}