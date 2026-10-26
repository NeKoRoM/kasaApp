package pl.neko

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import database.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kasaapp.composeapp.generated.resources.Res
import kasaapp.composeapp.generated.resources.compose_multiplatform
import kotlinx.coroutines.flow.firstOrNull
import pl.neko.database.entities.SettingsEntity

@Composable
@Preview
fun App(
    database: AppDatabase
) {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        var settingsText by remember { mutableStateOf("Loading settings...") }

        LaunchedEffect(Unit) {
            scope.launch {
                val settingsDao = database.dao()
                // Insert a test setting if none exists

                    settingsDao.upsertSettings(SettingsEntity(userId = "testUser", isDarkMode = true))


                // Retrieve and display the setting
                val settings = settingsDao.getSettingsByUserId("testUser").firstOrNull()
                settingsText = settings?.let { "User: ${it.userId}, Dark Mode: ${it.isDarkMode}" } ?: "No settings found."
            }
        }

        var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(settingsText)
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                    Text(settingsText)
                }
            }
        }
    }
}