import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose.multiplatform)
    alias(libs.plugins.jetbrains.compose.compiler)


    //id("org.jetbrains.compose") // якщо використовуєш Compose for Web



}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {

            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                        //todo check add before
                        add("${projectDirPath}src/wasmJsMain/resources")
                        add("$projectDir/public")
                        add("$projectDir/src/wasmJsMain/resources") // ← тут index.html


                    }
                }
            }

        }
        binaries.executable()
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(projects.composeApp)
                //implementation(libs.kotlin.stdlib.js)
                //implementation(compose.html.core)

                // implementation(compose.material3) ← поки не працює
            }
        }
    }
}