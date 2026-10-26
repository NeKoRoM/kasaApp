package com.example.exelioshop.dataclass

import java.io.Serializable

/**
 * [ListItem] — модель представлення знайденого Bluetooth-пристрою (РРО Datecs/Exellio або сканера) у списку вибору.
 * 
 * @property name мережеве або широкомовне ім'я пристрою (наприклад, "DP-05", "DP-25", "Exellio")
 * @property mac унікальна фізична MAC-адреса Bluetooth-модуля (наприклад, "00:12:6F:XX:XX:XX")
 */
data class ListItem(
    var name: String,
    var mac: String
) : Serializable
