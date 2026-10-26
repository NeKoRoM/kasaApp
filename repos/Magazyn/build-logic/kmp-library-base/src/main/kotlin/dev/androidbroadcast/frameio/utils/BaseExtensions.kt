@file:Suppress("ktlint")

package dev.androidbroadcast.frameio.utils

import com.android.build.api.dsl.AndroidResources
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.BuildFeatures
import com.android.build.api.dsl.BuildType
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.DefaultConfig
import com.android.build.api.dsl.Installation
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ProductFlavor
import com.android.build.api.dsl.TestExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.internal.dsl.DynamicFeatureExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * ====================================================================================================
 * БАЗОВІ РОЗШИРЕННЯ КОНФІГУРАЦІЇ GRADLE DSL (BASE GRADLE EXTENSIONS)
 * ====================================================================================================
 *
 * Надає зручні DSL-методи та розширення над Gradle API для:
 * - Уніфікованої конфігурації Android Gradle Plugin (AGP: application, library, dynamic-feature).
 * - Налаштування Kotlin Multiplatform (KMP), KMP Android Library та Compose Extension.
 * - Управління цільовими версіями Java / JVM Target для Android та Desktop.
 * - Інтеграції статичного аналізатора коду Detekt.
 */

private typealias AndroidExtensions = CommonExtension<
        out BuildFeatures,
        out BuildType,
        out DefaultConfig,
        out ProductFlavor,
        out AndroidResources,
        out Installation,
        >

/**
 * Отримує загальне розширення Android DSL незалежно від типу модуля (app чи library).
 */
private val Project.androidExtension: AndroidExtensions
    get() {
        return extensions.findByType(ApplicationExtension::class)
            ?: extensions.findByType(LibraryExtension::class)
            ?: extensions.findByType(DynamicFeatureExtension::class)
            ?: extensions.findByType(TestExtension::class)
            ?: error(
                "\"Project.androidExtension\" value may be called only "
                        + "from android application"
                        + " or android library gradle script",
            )
    }

/**
 * Блок налаштування параметрів Android (compileSdk, defaultConfig, buildTypes тощо).
 */
internal fun Project.androidConfig(
    block: AndroidExtensions.() -> Unit
): Unit = block(androidExtension)

/**
 * Налаштовує параметри компілятора Kotlin JVM для всіх задач [KotlinJvmCompile].
 */
internal fun Project.kotlinJvmCompilerOptions(block: KotlinJvmCompilerOptions.() -> Unit) {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions(block)
    }
}

/**
 * Швидкий доступ до каталогу версій Gradle Version Catalog (`libs.versions.toml`).
 */
internal val Project.libs: LibrariesForLibs
    get() = the<LibrariesForLibs>()

/**
 * Отримує екземпляр [KotlinMultiplatformExtension] для KMP модулів.
 */
private val Project.kmpExtension: KotlinMultiplatformExtension
    get() {
        return extensions.findByType(KotlinMultiplatformExtension::class) ?: error(
            "\"Project.kmpExtension\" value may be called only "
                    + "from kotlin multiplatform gradle script",
        )
    }

/**
 * Блок налаштування Kotlin Multiplatform конфігурації.
 */
internal inline fun Project.kmpConfig(
    block: KotlinMultiplatformExtension.() -> Unit
) = kmpExtension.block()

/**
 * Отримує екземпляр розширення компонентів KMP Android Library.
 */
private val Project.kmpAndroidLibComponentExtension: KotlinMultiplatformAndroidComponentsExtension
    get() {
        return extensions.findByType(KotlinMultiplatformAndroidComponentsExtension::class) ?: error(
            "\"Project.kmpAndroidLibComponentExtension\" value may be called only "
                    + "from kotlin multiplatform android gradle script",
        )
    }

/**
 * Блок конфігурації компонентів KMP Android Library.
 */
internal inline fun Project.kmpAndroidLibComponentConfig(
    block: KotlinMultiplatformAndroidComponentsExtension.() -> Unit
) = kmpAndroidLibComponentExtension.block()

/**
 * Отримує екземпляр компонентів Android Library.
 */
private val Project.androidLibComponentExtension: LibraryAndroidComponentsExtension
    get() {
        return extensions.findByType(LibraryAndroidComponentsExtension::class) ?: error(
            "\"Project.androidLibComponentExtension\" value may be called only "
                    + "from android library gradle script",
        )
    }

/**
 * Блок конфігурації компонентів Android Library.
 */
internal inline fun Project.androidLibComponentConfig(
    block: LibraryAndroidComponentsExtension.() -> Unit
) = androidLibComponentExtension.block()

/**
 * Доступ до плагіна JetBrains Compose Multiplatform.
 */
internal val Project.composeExt: ComposeExtension
    get() = extensions.findByType(ComposeExtension::class.java)
        ?: error("Compose plugin is not applied")

/**
 * Визначає версію [JavaVersion] на основі цільової JVM-платформи.
 */
@Suppress("MagicNumber")
internal fun LibrariesForLibs.javaVersion(target: ProjectTargets.JvmTarget): JavaVersion {
    val jdkVersion = jvmVersion(target)
    require(jdkVersion >= 10)
    return JavaVersion.toVersion(jdkVersion)
}

/**
 * Зчитує номер цільової версії JDK з Version Catalog (`versions.jdkAndroid` або `versions.jdkDesktop`).
 */
private fun LibrariesForLibs.jvmVersion(target: ProjectTargets.JvmTarget): Int {
    return when (target) {
        ProjectTargets.Android -> versions.jdkAndroid.get().toInt()
        ProjectTargets.Desktop -> versions.jdkDesktop.get().toInt()
    }
}

/**
 * Визначає цільовий [JvmTarget] компілятора Kotlin для Android або Desktop.
 */
@Suppress("MagicNumber")
internal fun LibrariesForLibs.jvmTarget(target: ProjectTargets.JvmTarget): JvmTarget {
    val jdkVersion = jvmVersion(target)
    require(jdkVersion >= 10)
    return JvmTarget.valueOf("JVM_$jdkVersion")
}

/**
 * Доступ до конфігурації статичного аналізатора коду Detekt.
 */
internal val Project.detektExtension: DetektExtension
    get() = checkNotNull(extensions.findByType(DetektExtension::class))

/**
 * Блок налаштування правил перевірки якості коду Detekt.
 */
internal fun Project.detektConfig(block: DetektExtension.() -> Unit) = block(detektExtension)

