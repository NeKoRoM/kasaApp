package nekorom.com

import androidx.compose.ui.window.ComposeUIViewController

/**
 * ====================================================================================================
 * ТОЧКА ВХОДУ ДЛЯ IOS (IOS UIKIT BRIDGE - COMPOSE MULTIPLATFORM)
 * ====================================================================================================
 *
 * Фабрична функція, що експортується у Swift/Objective-C фреймворк для платформи Apple iOS.
 * Створює нативний [UIViewController], усередині якого монтується спільний Composable-інтерфейс [KasaApp].
 *
 * @return Нативний контролер представлення iOS для інтеграції у SwiftUI або UIKit `UIWindow`.
 */
fun MainViewController() = ComposeUIViewController { KasaApp() }