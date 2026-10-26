package database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сутність співробітника / касира / комірника для мультиплатформного модуля Magazyn (KMP Room Entity).
 *
 * @property name Ім'я або посада співробітника.
 * @property id Унікальний автогенерований ідентифікатор особи в системі.
 */
@Entity
data class Person (
    val name : String,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)