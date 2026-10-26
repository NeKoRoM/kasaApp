plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(project(":core-protocol"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jserialcomm)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}
