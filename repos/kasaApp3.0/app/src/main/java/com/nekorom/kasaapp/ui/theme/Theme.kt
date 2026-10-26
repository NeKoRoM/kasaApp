// =================================================================================================
// ГОЛОВНА ТЕМА ЗАСТОСУНКУ KASAAPP (JETPACK COMPOSE MATERIAL 3 THEME)
//
// Архітектурний шар: Presentation / UI Theme Layer.
// Призначення:
// Налаштовує колірні схеми (світлу/темну), динамічні кольори Material You (Android 12+),
// системні відступи (System Insets) та стиль рядка стану (Status Bar).
// =================================================================================================

package com.nekorom.kasaapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Колірна схема для темного інтерфейсу
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// Колірна схема для світлого інтерфейсу
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * Коренева функція теми [KasaAppTheme] для екранів Jetpack Compose.
 *
 * @param darkTheme Прапорець використання темної теми (за замовчуванням визначається системою).
 * @param dynamicColor Підтримка динамічних системних кольорів на Android 12+ (API 31+).
 * @param content Вміст Composable-ієрархії екрана.
 */
@Composable
fun KasaAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Вибір схеми з урахуванням версії ОС та налаштувань користувача
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Налаштування кольору статус-бару та контрастності іконок
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}