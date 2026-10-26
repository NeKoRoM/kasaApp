package nekorom.com.uikit

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Shapes as M3Shapes

/**
 * ====================================================================================================
 * ГЕОМЕТРИЧНІ ФОРМИ ТА СКРУГЛЕННЯ UI-KIT (COMPOSE MULTIPLATFORM)
 * ====================================================================================================
 *
 * Визначає набір стандартних радіусів скруглення кутів для карток, діалогів, кнопок та полів вводу:
 * - [none]: прямі кути (0.dp)
 * - [extraSmall]: мінімальне скруглення (6.dp) для чіпів та дрібних бейджів
 * - [small]: базове скруглення (8.dp) для кнопок та карток
 * - [medium]: середнє скруглення (12.dp) для меню та контейнерів
 * - [large]: велике скруглення (16.dp) для діалогових вікон
 * - [extraLarge]: максимальне скруглення (28.dp)
 * - [full]: повне скруглення у формі таблетки (50%)
 */

/**
 * Провайдер геометричних форм для доступу через [LocalShapes.current].
 */
internal val LocalShapes = staticCompositionLocalOf { shapes() }

/**
 * Фабрична функція ініціалізації стандартних радіусів скруглення.
 */
internal fun shapes(): Shapes {
    return Shapes(
        none = RoundedCornerShape(size = 0.dp),
        extraSmall = RoundedCornerShape(size = 6.dp),
        small = RoundedCornerShape(size = 8.dp),
        medium = RoundedCornerShape(size = 12.dp),
        large = RoundedCornerShape(size = 16.dp),
        extraLarge = RoundedCornerShape(size = 28.dp),
        full = RoundedCornerShape(percent = 50),
    )
}

/**
 * Незмінна модель конфігурації форм для Compose Runtime.
 */
@Immutable
data class Shapes(
    val none: CornerBasedShape,
    val extraSmall: CornerBasedShape,
    val small: CornerBasedShape,
    val medium: CornerBasedShape,
    val large: CornerBasedShape,
    val extraLarge: CornerBasedShape,
    val full: CornerBasedShape,
)

/**
 * Конвертер у стандартний набір [M3Shapes] для сумісності з компонентами бібліотеки Material 3.
 */
internal fun Shapes.toM3Shapes(): M3Shapes {
    return M3Shapes(
        small = small,
        medium = medium,
        large = large,
        extraLarge = extraLarge,
        extraSmall = extraSmall,
    )
}