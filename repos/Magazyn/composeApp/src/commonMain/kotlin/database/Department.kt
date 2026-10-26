package database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сутність відділу / складу магазину для мультиплатформного модуля Magazyn (KMP Room Entity).
 *
 * Використовується для категоризації товарів за відділами торговельної точки.
 *
 * @property id Унікальний автогенерований ідентифікатор відділу.
 * @property name Назва відділу/складу.
 */
@Entity
data class Department(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
)
