package com.example.exellio20.models

import java.io.Serializable

/**
 * ====================================================================================================
 * МОДЕЛЬ BLUETOOTH-ПРИСТРОЮ В СПИСКУ ПОШУКУ (BT LIST ITEM - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Представляє знайдений або спарений пристрій Bluetooth (РРО Datecs/Exellio, бездротові ваги або сканер).
 * Реалізує [Serializable] для передачі через Intent extras між активностями та фрагментами.
 *
 * @property name Назва Bluetooth пристрою (наприклад, "DP-25", "FP-700", "DP-05").
 * @property mac MAC-адреса Bluetooth модуля (формат `XX:XX:XX:XX:XX:XX`).
 */
data class ListItem(
    var name: String,
    var mac: String
) : Serializable

