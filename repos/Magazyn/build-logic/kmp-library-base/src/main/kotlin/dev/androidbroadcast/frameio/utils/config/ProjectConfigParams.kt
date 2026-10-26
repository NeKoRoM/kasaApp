package dev.androidbroadcast.frameio.utils.config

import org.gradle.api.Project
import java.util.Properties

/**
 * ====================================================================================================
 * ІЄРАРХІЧНЕ ЗЧИТУВАННЯ ПАРАМЕТРІВ ПРОЕКТУ GRADLE (PROJECT CONFIG PARAMS READER)
 * ====================================================================================================
 *
 * Модуль забезпечує гнучке налаштування параметрів компіляції та плагінів за пріоритетами:
 * 1. Властивості Gradle (`-Pparam=value`).
 * 2. Файл `local.properties` (локальні налаштування розробника).
 * 3. Змінні оточення операційної системи (CI/CD пайплайни).
 * 4. Значення за замовчуванням ([ProjectConfigParam.defaultValue]).
 */

/**
 * Опис параметра конфігурації із зазначенням імен ключів у різних джерелах.
 */
internal data class ProjectConfigParam(
    val cmdParamName: String,
    val androidLocalPropertyParamName: String? = null,
    val envParamName: String? = null,
    val defaultValue: String? = null,
)

/**
 * Зчитує значення параметра згідно зі встановленою ієрархією пріоритетів.
 */
@Suppress("ktlint:standard:function-signature")
internal fun Project.readConfigParam(param: ProjectConfigParam): String? = with(param) {
    return rootProject.findProperty(cmdParamName) as String? // 1. Gradle property
        ?: androidLocalPropertyParamName?.let(::readFromLocalProperties) // 2. From local.properties
        ?: envParamName?.let(System::getenv) // 3. Environment variable
        ?: defaultValue
}

/**
 * Допоміжна функція зчитування ключа безпосередньо з файлу `local.properties` у корені проекту.
 */
internal fun Project.readFromLocalProperties(key: String): String? {
    val localPropertiesFile = rootProject.file("local.properties")
    if (!localPropertiesFile.exists()) return null

    val localProperties = Properties().apply {
        localPropertiesFile.inputStream().use(::load)
    }
    return localProperties.getProperty(key)
}

