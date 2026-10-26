import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(project(":core-model"))
    implementation(project(":core-protocol"))
    implementation(project(":feature-pos"))
    implementation(project(":feature-sync"))
    implementation(project(":feature-transport"))

    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)

    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test)
}

compose.desktop {
    application {
        mainClass = "com.kasa.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KasaApp"
            packageVersion = "5.0.0"
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}
