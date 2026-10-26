rootProject.name = "kasaApp-v5"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":core-model")
include(":core-protocol")
include(":feature-pos")
include(":feature-sync")
include(":feature-transport")
include(":desktopApp")

val localProps = file("local.properties")
val hasAndroidSdk = (localProps.exists() && localProps.readText().contains("sdk.dir")) ||
        System.getenv("ANDROID_HOME") != null ||
        System.getenv("ANDROID_SDK_ROOT") != null

if (hasAndroidSdk) {
    include(":androidApp")
}


