@file:Suppress("MagicNumber")

package nekorom.com.uikit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme as M3ColorScheme

/**
 * ====================================================================================================
 * СИСТЕМА КОЛЬОРІВ ТА ТЕМАТИЧНА ПАЛІТРА UI-KIT (COMPOSE MULTIPLATFORM)
 * ====================================================================================================
 *
 * Модуль визначає дизайн-систему кольорів для багатоплатформного клієнта "Magazyn" (Android, Desktop, Wasm).
 * Включає:
 * 1. Розширену палітру кольорів відповідно до специфікації Material Design 3 (M3).
 * 2. Специфічні складські доменні кольори для статусу товарів та матеріальних цінностей (Asset status):
 *    - [assetApproved]: затверджений / прийнятий на баланс
 *    - [assetNoStatus]: невизначений статус
 *    - [assetNeedsReview]: потребує перевірки / ревізії
 *    - [assetInProgress]: в процесі переміщення або видачі
 * 3. Механізм надання через [CompositionLocal] та конвертер у стандартний [M3ColorScheme].
 */

/**
 * Локальний провайдер теми Compose для доступу до поточної палітри кольорів через [LocalColorScheme.current].
 */
internal val LocalColorScheme = staticCompositionLocalOf { lightColorScheme() }

/**
 * Світла колірна схема інтерфейсу.
 */
internal fun lightColorScheme(): ColorScheme {
    return ColorScheme(
        primary = Color(0xFF7466FE),
        onPrimary = Color(0xFFF5F5F6),
        primaryContainer = Color(0xFF7466FE),
        onPrimaryContainer = Color(0xFFF5F5F6),
        secondary = Color(0xFF8060B7),
        onSecondary = Color(0xFFF5F5F6),
        secondaryContainer = Color(0xFF8060B7),
        onSecondaryContainer = Color(0xFFF5F5F6),
        tertiary = Color(0xFF5A6A8E),
        onTertiary = Color(0xFFF5F5F6),
        tertiaryContainer = Color(0xFF5A6A8E),
        onTertiaryContainer = Color(0xFFF5F5F6),
        error = Color(0xFFFC8079),
        onError = Color(0xFF43120F),
        errorContainer = Color(0xFFD7251D),
        onErrorContainer = Color(0XFFF9DEDC),
        surfaceBright = Color.White,
        surface = Color.White,
        surfaceVariant = Color(0xFF42475A),
        surfaceDim = Color(0xFFE1E3EF),
        surfaceContainerHighest = Color(0xFFDADCE8),
        surfaceContainerHigh = Color(0xFFE3E3EE),
        surfaceContainer = Color(0xFFE9ECF3),
        surfaceContainerLow = Color(0xFFEFF1F6),
        surfaceContainerLowest = Color.White,
        onSurface = Color(0xFF1B1B20),
        onSurfaceVariant = Color(0xFF45464F),
        outline = Color(0xFFAEB1BE),
        outlineVariant = Color(0xFFDADDE7),
        inversePrimary = Color(0xFF8B7EFF),
        inverseSurface = Color(0xFF15151C),
        inverseOnSurface = Color(0xFFF2F2F9),
        scrim = Color.Black,
        surfaceTint = Color(0xFF11121C),
        background = Color.Black,
        onBackground = Color.White,
        onSurfaceDim = Color(0xFF626773),
        outlineDim = Color(0xFF13141B),
        inverseOutline = Color(0xFFE3E6EC),
        inverseOnSurfaceVariant = Color(0xFF5C6473),
        inverseOnSurfaceDim = Color(0xFF7D829C),
        assetApproved = Color(0xFF4B5065),
        assetNoStatus = Color(0xFFFF9659),
        assetNeedsReview = Color(0xFF61CAFF),
        assetInProgress = Color(0xFF1DC0A6),
    )
}

/**
 * Темна колірна схема інтерфейсу для складських терміналів в умовах низького освітлення.
 */
internal fun darkColorScheme(): ColorScheme {
    return ColorScheme(
        primary = Color(0xFF7466FE),
        onPrimary = Color(0xFFF5F5F6),
        primaryContainer = Color(0xFF7466FE),
        onPrimaryContainer = Color(0xFFF5F5F6),
        secondary = Color(0xFF8060B7),
        onSecondary = Color(0xFFF5F5F6),
        secondaryContainer = Color(0xFF8060B7),
        onSecondaryContainer = Color(0xFFF5F5F6),
        tertiary = Color(0xFF5A6A8E),
        onTertiary = Color(0xFFF5F5F6),
        tertiaryContainer = Color(0xFF5A6A8E),
        onTertiaryContainer = Color(0xFFF5F5F6),
        error = Color(0xFFFC8079),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFFD7251D),
        onErrorContainer = Color(0XFFF9DEDC),
        surfaceBright = Color(0xFF4B5065),
        surface = Color(0xFF161825),
        surfaceVariant = Color(0xFFDADCE8),
        surfaceDim = Color.Black,
        surfaceContainerHighest = Color(0xFF42475A),
        surfaceContainerHigh = Color(0xFF34374B),
        surfaceContainer = Color(0xFF242737),
        surfaceContainerLow = Color(0xFF11121C),
        surfaceContainerLowest = Color(0xFF0C0D14),
        onSurface = Color(0xFFF5F5F6),
        onSurfaceVariant = Color(0xFF9AA6B9),
        outline = Color(0xFF4C4E61),
        outlineVariant = Color(0xFF333545),
        inversePrimary = Color(0xFF978CFF),
        inverseSurface = Color.White,
        inverseOnSurface = Color(0xFF20222B),
        scrim = Color.Black,
        surfaceTint = Color.White,
        background = Color.White,
        onBackground = Color.Black,
        onSurfaceDim = Color(0xFF616773),
        outlineDim = Color(0xFF9195A1),
        inverseOutline = Color(0xFF3A3C4D),
        inverseOnSurfaceVariant = Color(0xFF9AA6B9),
        inverseOnSurfaceDim = Color(0xFF666A72),
        assetApproved = Color(0xFF4B5065),
        assetNoStatus = Color.White,
        assetNeedsReview = Color(0xFF61CAFF),
        assetInProgress = Color(0xFF1DC0A6),
    )
}

/**
 * Незмінна модель схеми кольорів додатку, оптимізована для Compose Runtime.
 */
@Immutable
data class ColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceTint: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerLowest: Color,
    val onSurfaceDim: Color,
    val outlineDim: Color,
    val inverseOutline: Color,
    val inverseOnSurfaceVariant: Color,
    val inverseOnSurfaceDim: Color,
    val assetNoStatus: Color,
    val assetNeedsReview: Color,
    val assetInProgress: Color,
    val assetApproved: Color,
)

/**
 * Конвертер кастомної палітри у стандартну [M3ColorScheme] для сумісності з Material3 Scaffold, Buttons тощо.
 */
internal fun ColorScheme.toM3ColorScheme(): M3ColorScheme {
    return M3ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,
    )
}