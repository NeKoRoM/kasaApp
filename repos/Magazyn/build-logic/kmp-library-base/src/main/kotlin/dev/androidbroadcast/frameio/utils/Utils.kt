package dev.androidbroadcast.frameio.utils

import org.gradle.api.Project
import org.gradle.api.plugins.PluginContainer

/**
 * ====================================================================================================
 * УТИЛІТИ НАЛАШТУВАННЯ СИСТЕМИ ЗБІРКИ GRADLE (GRADLE BUILD UTILITIES)
 * ====================================================================================================
 *
 * Набір допоміжних функцій-розширень для конвенційних плагінів Gradle проекту:
 * - Безпечне застосування плагінів без дублювання.
 * - Реакція на додавання плагінів у життєвому циклі конфігурації проекту.
 */

/**
 * Застосовує вказаний плагін за ідентифікатором [id], якщо він або альтернативні [ids] ще не підключені.
 *
 * @param id Основний ідентифікатор плагіна.
 * @param ids Додаткові споріднені ідентифікатори.
 * @return `true`, якщо плагін було успішно застосовано; `false`, якщо плагін уже був присутній.
 */
internal fun PluginContainer.applyIfNeeded(
    id: String,
    vararg ids: String,
): Boolean {
    if (hasPlugin(id) || ids.any(::hasPlugin)) return false

    apply(id)
    return true
}

/**
 * Виконує лямбду [action] для проекту, коли цільовий плагін [pluginId] присутній або щойно доданий.
 *
 * @param pluginId Ідентифікатор очікуваного плагіна.
 * @param action Дія конфігурації.
 */
fun Project.applyForPlugin(
    pluginId: String,
    action: () -> Unit,
) {
    if (plugins.hasPlugin(pluginId)) {
        action()
    } else {
        plugins.whenPluginAdded {
            if (plugins.hasPlugin(pluginId)) {
                action()
            }
        }
    }
}

