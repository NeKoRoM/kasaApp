package com.kasa.feature.sync

import com.kasa.model.Product
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Файлове сховище каталогу товарів для локальної роботи касира офлайн.
 */
class FileJsonProductStorage(
    private val file: File,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
) {

    fun loadCatalog(): List<Product> = synchronized(this) {
        if (!file.exists()) return emptyList()
        return try {
            val content = file.readText(Charsets.UTF_8)
            if (content.isBlank()) return emptyList()
            json.decodeFromString<List<Product>>(content)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCatalog(products: List<Product>) {
        synchronized(this) {
            try {
                file.parentFile?.mkdirs()
                val jsonString = json.encodeToString(products)
                file.writeText(jsonString, Charsets.UTF_8)
            } catch (_: Exception) {
                // Запобігаємо збою застосунку при обмеженнях файлової системи
            }
        }
    }
}
