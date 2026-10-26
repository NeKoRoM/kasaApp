@file:Suppress("unused")

package nekorom.com.uikit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * ====================================================================================================
 * БАЗОВА ТЕМА КОРИСТУВАЦЬКОГО ІНТЕРФЕЙСУ (KASA APP THEME - COMPOSE MULTIPLATFORM)
 * ====================================================================================================
 *
 * Головний обгортковий Composable-компонент, який налаштовує дизайн-систему додатку:
 * - Автоматично визначає системну темну/світлу тему ([isSystemInDarkTheme]).
 * - Надає через [CompositionLocalProvider] доступ до кастомних кольорів [LocalColorScheme] та форм [LocalShapes].
 * - Транслює кастомні налаштування у стандартну Material Design 3 тему [MaterialTheme].
 *
 * @param isDarkTheme Прапорець активації темної теми (за замовчуванням визначається системою).
 * @param colors Палітра кольорів [ColorScheme].
 * @param typography Типографіка інтерфейсу [Typography].
 * @param shapes Геометрія та скруглення компонентів [Shapes].
 * @param content Вкладений вміст користувацького інтерфейсу.
 */
@Composable
fun KasaAppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    colors: ColorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme(),
    typography: Typography = typography(),
    shapes: Shapes = shapes(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColorScheme provides colors,
        LocalShapes provides shapes,
    ) {
        MaterialTheme(
            colorScheme = colors.toM3ColorScheme(),
            typography = typography,
            shapes = shapes.toM3Shapes(),
            content = content,
        )
    }
}

/**
 * Об'єкт швидкого доступу до поточної теми в Composable-функціях:
 * `KasaAppTheme.colors`, `KasaAppTheme.typography`, `KasaAppTheme.shapes`.
 */
object KasaAppTheme {
    /**
     * Поточна колірна палітра з урахуванням темного або світлого режиму.
     */
    val colors: ColorScheme
        @[Composable ReadOnlyComposable] get() = LocalColorScheme.current

    /**
     * Поточні текстові стилі Material Design 3.
     */
    val typography: Typography
        @[Composable ReadOnlyComposable] get() = MaterialTheme.typography

    /**
     * Поточні налаштування геометричних форм та заокруглень.
     */
    val shapes: Shapes
        @[Composable ReadOnlyComposable] get() = LocalShapes.current
}