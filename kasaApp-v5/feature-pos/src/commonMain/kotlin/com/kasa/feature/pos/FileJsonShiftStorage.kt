package com.kasa.feature.pos

import com.kasa.model.CashierShift
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Модель персистентного стану журналу касових змін.
 */
@Serializable
data class ShiftsJournalData(
    val currentShift: CashierShift? = null,
    val history: List<CashierShift> = emptyList()
)

/**
 * Файлове JSON-сховище журналу касових змін.
 * Зберігає активну зміну касира та історію закритих змін із номерами Z-звітів:
 * - Персистентний облік поточного стану касової зміни;
 * - Збереження історії змін із часовими мітками відкриття/закриття;
 * - Пошук архівних змін за номером фіскального Z-звіту;
 * - Потокобезпечний запис та відновлення після збоїв.
 */
class FileJsonShiftStorage(
    private val file: File,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
) {

    fun loadData(): ShiftsJournalData = synchronized(this) {
        if (!file.exists()) {
            return ShiftsJournalData()
        }
        return runCatching {
            val content = file.readText(Charsets.UTF_8)
            if (content.isBlank()) ShiftsJournalData() else json.decodeFromString<ShiftsJournalData>(content)
        }.getOrElse { ShiftsJournalData() }
    }

    fun saveData(data: ShiftsJournalData) = synchronized(this) {
        runCatching {
            file.parentFile?.mkdirs()
            val content = json.encodeToString(data)
            file.writeText(content, Charsets.UTF_8)
        }
    }

    fun loadCurrentShift(): CashierShift? = synchronized(this) {
        return loadData().currentShift
    }

    fun loadHistory(): List<CashierShift> = synchronized(this) {
        return loadData().history
    }

    fun saveCurrentShift(shift: CashierShift?) = synchronized(this) {
        val currentData = loadData()
        saveData(currentData.copy(currentShift = shift))
    }

    fun recordClosedShift(shift: CashierShift) = synchronized(this) {
        val currentData = loadData()
        val updatedHistory = listOf(shift) + currentData.history
        saveData(
            currentData.copy(
                currentShift = null,
                history = updatedHistory
            )
        )
    }

    /**
     * Знаходить закриту зміну за номером виконаного фіскального Z-звіту.
     */
    fun findShiftByZReport(zReportNumber: String): CashierShift? = synchronized(this) {
        return loadHistory().firstOrNull { it.zReportNumber == zReportNumber }
    }

    /**
     * Повертає загальну кількість архівних змін у журналі.
     */
    fun getClosedShiftsCount(): Int = synchronized(this) {
        return loadHistory().size
    }

    /**
     * Очищає історію закритих змін (наприклад, для тестових стендів).
     */
    fun clearHistory() = synchronized(this) {
        val currentData = loadData()
        saveData(currentData.copy(history = emptyList()))
    }
}
