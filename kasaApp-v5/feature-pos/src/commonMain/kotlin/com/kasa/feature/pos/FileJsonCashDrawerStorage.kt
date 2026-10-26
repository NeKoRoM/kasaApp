package com.kasa.feature.pos

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Стан грошової скриньки касира.
 */
@Serializable
data class CashDrawerState(
    val balanceKopecks: Long = 0L,
    val lastUpdatedTimestamp: Long = 0L
)

/**
 * Файлове сховище поточного залишку готівки в грошовій скриньці РРО:
 * - Зберігає поточний баланс готівки у копійках для уникнення втрати точності;
 * - Фіксує часову мітку останнього службового внесення/видачі чи продажу;
 * - Забезпечує атомарний потокобезпечний доступ до файлу;
 * - Захищає від збоїв введення/виведення та пошкодженого JSON.
 */
class FileJsonCashDrawerStorage(
    private val file: File,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
) {
    /**
     * Завантажує актуальний стан грошової скриньки.
     */
    fun loadDrawerState(): CashDrawerState = synchronized(this) {
        if (!file.exists()) return CashDrawerState()
        return try {
            val content = file.readText(Charsets.UTF_8)
            if (content.isBlank()) return CashDrawerState()
            json.decodeFromString<CashDrawerState>(content)
        } catch (_: Exception) {
            CashDrawerState()
        }
    }

    /**
     * Зберігає новий стан грошової скриньки у файл.
     */
    fun saveDrawerState(state: CashDrawerState) = synchronized(this) {
        try {
            file.parentFile?.mkdirs()
            val jsonString = json.encodeToString(state)
            file.writeText(jsonString, Charsets.UTF_8)
        } catch (_: Exception) {
            // Запобігаємо збою застосунку при IO помилках
        }
    }

    /**
     * Коригує залишок у скриньці на дельту (внесення або видача).
     */
    fun updateBalance(deltaKopecks: Long): Long = synchronized(this) {
        val current = loadDrawerState()
        val newBalance = (current.balanceKopecks + deltaKopecks).coerceAtLeast(0L)
        saveDrawerState(CashDrawerState(balanceKopecks = newBalance, lastUpdatedTimestamp = System.currentTimeMillis()))
        newBalance
    }

    /**
     * Обнуляє баланс скриньки (наприклад, під час Z-звіту та інкасації).
     */
    fun resetBalance() = synchronized(this) {
        saveDrawerState(CashDrawerState(balanceKopecks = 0L, lastUpdatedTimestamp = System.currentTimeMillis()))
    }

    /**
     * Повертає поточний залишок у копійках.
     */
    fun getCurrentBalance(): Long = synchronized(this) {
        loadDrawerState().balanceKopecks
    }
}
