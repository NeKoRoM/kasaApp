package pl.neko

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import pl.neko.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "KasaApp",
        ) {
            App()
        }
    }
}