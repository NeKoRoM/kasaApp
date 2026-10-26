import org.gradle.kotlin.dsl.desktop
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose.multiplatform)
    alias(libs.plugins.jetbrains.compose.compiler)


}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(compose.desktop.currentOs)

    implementation(libs.ktor.client.engine.cio)

    implementation(libs.multiplatform.settings)


}

compose.desktop {
    application {
        mainClass = "nekorom.com.kasaapp.KasaApp"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "nekorom.com"
            packageVersion = "1.0.0"
        }
    }
}