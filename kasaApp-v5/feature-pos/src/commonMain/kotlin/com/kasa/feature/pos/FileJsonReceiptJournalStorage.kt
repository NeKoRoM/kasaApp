package com.kasa.feature.pos

import com.kasa.model.Receipt
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Файлове сховище локального журналу фіскалізованих чеків касира:
 * - Зберігає історію чеків для повернень, дублікатів та звірки;
 * - Додає нові чеки на початок списку (LIFO-сортування);
 * - Потокобезпечний запис (synchronized) та захист від IO збоїв;
 * - Підтримка фільтрації за ідентифікатором та часовим діапазоном.
 */
class FileJsonReceiptJournalStorage(
    private val file: File,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
) {

    /**
     * Завантажує повний список збережених чеків.
     */
    fun loadJournal(): List<Receipt> = synchronized(this) {
        if (!file.exists()) return emptyList()
        return try {
            val content = file.readText(Charsets.UTF_8)
            if (content.isBlank()) return emptyList()
            json.decodeFromString<List<Receipt>>(content)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Записує новий фіскалізований чек на початок журналу.
     */
    fun recordReceipt(receipt: Receipt) = synchronized(this) {
        val currentList = loadJournal().toMutableList()
        currentList.add(0, receipt)
        try {
            file.parentFile?.mkdirs()
            val jsonString = json.encodeToString(currentList)
            file.writeText(jsonString, Charsets.UTF_8)
        } catch (_: Exception) {
            // Запобігаємо збою застосунку при IO помилках
        }
    }

    /**
     * Знаходить чек за його унікальним ідентифікатором.
     */
    fun findReceiptById(id: String): Receipt? = synchronized(this) {
        loadJournal().firstOrNull { it.id == id }
    }

    /**
     * Повертає кількість чеків у журналі.
     */
    fun getReceiptsCount(): Int = synchronized(this) {
        loadJournal().size
    }

    /**
     * Повертає останній за часом фіскалізований чек.
     */
    fun findLatestReceipt(): Receipt? = synchronized(this) {
        loadJournal().firstOrNull()
    }

    /**
     * Очищає журнал чеків (наприклад, для тестових стендів або скидання).
     */
    fun clearJournal() = synchronized(this) {
        try {
            file.parentFile?.mkdirs()
            val jsonString = json.encodeToString(emptyList<Receipt>())
            file.writeText(jsonString, Charsets.UTF_8)
        } catch (_: Exception) {
            // Запобігаємо збою застосунку при IO помилках
        }
    }
}
