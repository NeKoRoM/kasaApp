package com.example.kasaapp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import nekorom.com.KasaApp

/**
 * ====================================================================================================
 * ТОЧКА ВХОДУ ДЛЯ ВЕБ (KOTLIN WEBASSEMBLY - WASM/JS)
 * ====================================================================================================
 *
 * Головна функція [main], що запускає додаток у сучасному веб-браузері з підтримкою Wasm:
 * 1. Отримує DOM-елемент `document.body` HTML-сторінки.
 * 2. Ініціалізує полотно [ComposeViewport], на якому малюється інтерфейс через WebGPU / CanvasKit.
 * 3. Відображає спільний Composable-екран [KasaApp].
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        KasaApp()
    }
}