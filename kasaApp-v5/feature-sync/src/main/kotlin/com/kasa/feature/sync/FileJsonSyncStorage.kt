package com.kasa.feature.sync

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Файлове персистентне сховище офлайн-черги чеків у форматі JSON.
 * Гарантує збереження невідправлених чеків між перезапусками застосунку
 * та при раптовому вимкненні живлення касового терміналу.
 */
class FileJsonSyncStorage(
    private val file: File,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
) : SyncStoragePersistence {

    override fun loadQueue(): List<OrderCreateDto> = synchronized(this) {
        if (!file.exists()) return emptyList()
        return try {
            val content = file.readText(Charsets.UTF_8)
            if (content.isBlank()) return emptyList()
            json.decodeFromString<List<OrderCreateDto>>(content)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun saveQueue(queue: List<OrderCreateDto>) {
        synchronized(this) {
            try {
                file.parentFile?.mkdirs()
                val jsonString = json.encodeToString(queue)
                file.writeText(jsonString, Charsets.UTF_8)
            } catch (_: Exception) {
                // Запобігаємо збою застосунку при обмеженнях файлової системи
            }
        }
    }
}
