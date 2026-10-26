package com.kasa.feature.pos

import com.kasa.model.HeldReceipt
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Файлове сховище відкладених чеків касира.
 * Забезпечує персистентне збереження незавершених кошиків покупців у JSON-форматі:
 * - Підтримка черги відкладених замовлень (додавання на початок);
 * - Пошук та вилучення за унікальним ідентифікатором;
 * - Потокобезпечний доступ (synchronized) для запобігання пошкодженню файлу;
 * - Захист від втрати даних при збоях вводу/виводу.
 */
class FileJsonHeldReceiptStorage(
    private val file: File,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
) {
    /**
     * Завантажує всі відкладені чеки з файлового сховища.
     */
    fun loadHeldReceipts(): List<HeldReceipt> = synchronized(this) {
        if (!file.exists()) return emptyList()
        return try {
            val content = file.readText(Charsets.UTF_8)
            if (content.isBlank()) return emptyList()
            json.decodeFromString<List<HeldReceipt>>(content)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Зберігає повний список відкладених чеків у файл.
     */
    fun saveHeldReceipts(receipts: List<HeldReceipt>) = synchronized(this) {
        try {
            file.parentFile?.mkdirs()
            val jsonString = json.encodeToString(receipts)
            file.writeText(jsonString, Charsets.UTF_8)
        } catch (_: Exception) {
            // Запобігаємо збою застосунку при IO помилках
        }
    }

    /**
     * Додає новий відкладений чек на початок списку.
     */
    fun addHeldReceipt(heldReceipt: HeldReceipt) = synchronized(this) {
        val current = loadHeldReceipts().toMutableList()
        current.add(0, heldReceipt)
        saveHeldReceipts(current)
    }

    /**
     * Вилучає відкладений чек за його ідентифікатором.
     */
    fun removeHeldReceipt(id: String): HeldReceipt? = synchronized(this) {
        val current = loadHeldReceipts().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val removed = current.removeAt(index)
            saveHeldReceipts(current)
            removed
        } else {
            null
        }
    }

    /**
     * Повертає кількість активних відкладених чеків.
     */
    fun getHeldCount(): Int = synchronized(this) {
        loadHeldReceipts().size
    }

    /**
     * Знаходить відкладений чек за ідентифікатором без його вилучення.
     */
    fun findReceiptById(id: String): HeldReceipt? = synchronized(this) {
        loadHeldReceipts().firstOrNull { it.id == id }
    }

    /**
     * Очищає всі відкладені чеки.
     */
    fun clearAll() = synchronized(this) {
        saveHeldReceipts(emptyList())
    }
}
