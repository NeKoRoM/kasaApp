// =================================================================================================
// ТИПОГРАФІКА ДИЗАЙН-СИСТЕМИ COMPOSE (MATERIAL 3 TYPOGRAPHY)
//
// Архітектурний шар: Presentation / UI Theme Layer.
// Призначення:
// Визначає базові шрифтові набори та стилі тексту для елементів інтерфейсу касира
// (чек, назви товарів, суми, кнопки управління).
// =================================================================================================

package com.nekorom.kasaapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Базова конфігурація типографіки Material Design 3 для POS-терміналу.
 */
val Typography = Typography(
    // Основний текст елементів інтерфейсу та списків товарів
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)