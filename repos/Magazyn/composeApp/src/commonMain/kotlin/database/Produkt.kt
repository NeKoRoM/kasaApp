package database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сутність складського товару для мультиплатформного модуля Magazyn (KMP Room Entity).
 *
 * Зберігає залишки товарів на складі, синхронізовані з Google Таблицями (Google Sheets)
 * та касовими накопичувачами.
 *
 * @property code Штрихкод або внутрішній касовий артикул товару.
 * @property amountInStore Залишок товару на складі/у торговельному залі.
 * @property name Найменування товару.
 * @property id Унікальний строковий первинний ключ товару.
 */
@Entity
data class Product(
    val code : String,
    val amountInStore: String,
    val name : String,
    @PrimaryKey
    val id: String
)
