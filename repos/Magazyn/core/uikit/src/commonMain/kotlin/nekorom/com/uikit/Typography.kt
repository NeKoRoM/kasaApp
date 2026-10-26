package nekorom.com.uikit

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography as M3Typography

/**
 * ====================================================================================================
 * ТИПОГРАФІКА ТА ТЕКСТОВІ СТИЛІ UI-KIT (COMPOSE MULTIPLATFORM)
 * ====================================================================================================
 *
 * Визначає єдину типографічну шкалу Material Design 3 для додатку "Magazyn":
 * - Базовий шрифт: [FontFamily.SansSerif] для максимальної кросплатформної читабельності (Android, Desktop, Web).
 * - Розміри та вага кегля:
 *   - Display (Large 57sp, Medium 45sp, Small 36sp) — великі заголовки екранів
 *   - Headline (Large 32sp, Medium 28sp, Small 24sp) — розділи та акцентні блоки
 *   - Title (Large 22sp, Medium 18sp, Small 16sp) — назви карток та полів
 *   - Body (Large 16sp, Medium 14sp, Small 12sp) — основний текст та описи
 *   - Label (Large 16sp, Medium 14sp, Small 11sp) — підписи кнопок, інпутів та тегів
 */

private val DefaultTextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
)

/**
 * Генерує конфігурацію типографіки [M3Typography], інтегровану в дизайн-систему Material3.
 */
internal fun typography(): M3Typography {
    return M3Typography(
        displayLarge = DefaultTextStyle.copy(
            fontSize = 57.sp,
        ),
        displayMedium = DefaultTextStyle.copy(
            fontSize = 45.sp,
        ),
        displaySmall = DefaultTextStyle.copy(
            fontSize = 36.sp,
        ),
        headlineLarge = DefaultTextStyle.copy(
            fontSize = 32.sp,
        ),
        headlineMedium = DefaultTextStyle.copy(
            fontSize = 28.sp,
        ),
        headlineSmall = DefaultTextStyle.copy(
            fontSize = 24.sp,
        ),
        titleLarge = DefaultTextStyle.copy(
            fontSize = 22.sp,
        ),
        titleMedium = DefaultTextStyle.copy(
            fontSize = 18.sp,
        ),
        titleSmall = DefaultTextStyle.copy(
            fontSize = 16.sp,
        ),
        bodyLarge = DefaultTextStyle.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
        ),
        bodyMedium = DefaultTextStyle.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
        ),
        bodySmall = DefaultTextStyle.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
        ),
        labelLarge = DefaultTextStyle.copy(
            fontSize = 16.sp,
        ),
        labelMedium = DefaultTextStyle.copy(
            fontSize = 14.sp,
        ),
        labelSmall = DefaultTextStyle.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
        ),
    )
}