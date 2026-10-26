package com.kasa.feature.pos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkiaImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Base64

/**
 * Десктопна реалізація перегляду камери-сканера:
 * - На macOS: інтеграція з нативним AVFoundation + Apple Vision (mac-camera-scanner);
 * - Дзеркальний оптимізований відеопотік з вебкамери FaceTime / USB веб-камери;
 * - Апаратне розпізнавання EAN-13, EAN-8, QR, Code-128;
 * - Відкат до режиму USB/Bluetooth лазерного сканера (Wedge F2) на Linux/Windows.
 */
@Composable
actual fun PosCameraScannerView(
    modifier: Modifier,
    isFlashlightOn: Boolean,
    isScanningEnabled: Boolean,
    onBarcodeDetected: (String) -> Unit
) {
    val isMac = remember {
        System.getProperty("os.name")?.contains("Mac", ignoreCase = true) == true
    }

    var cameraFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var statusMessage by remember { mutableStateOf<String?>("Ініціалізація камери...") }
    val scope = rememberCoroutineScope()

    DisposableEffect(isMac) {
        if (!isMac) {
            statusMessage = "Десктоп: сканер готовий (F2 / USB)"
            return@DisposableEffect onDispose {}
        }

        val binaryFile = resolveMacScannerBinary()
        if (binaryFile == null || !binaryFile.canExecute()) {
            statusMessage = "Камера доступна через USB-сканер або F2"
            return@DisposableEffect onDispose {}
        }

        var process: Process? = null
        val job = scope.launch(Dispatchers.IO) {
            try {
                val proc = ProcessBuilder(binaryFile.absolutePath)
                    .redirectErrorStream(true)
                    .start()
                process = proc

                val reader = proc.inputStream.bufferedReader()
                while (isActive) {
                    val line = reader.readLine() ?: break
                    when {
                        line.startsWith("BARCODE:") -> {
                            val code = line.removePrefix("BARCODE:").trim()
                            if (code.isNotEmpty() && isScanningEnabled) {
                                withContext(Dispatchers.Main) {
                                    onBarcodeDetected(code)
                                }
                            }
                        }
                        line.startsWith("FRAME:") -> {
                            val base64 = line.removePrefix("FRAME:").trim()
                            runCatching {
                                val bytes = Base64.getDecoder().decode(base64)
                                val bitmap = SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                                withContext(Dispatchers.Main) {
                                    cameraFrame = bitmap
                                    statusMessage = null
                                }
                            }
                        }
                        line.startsWith("READY") -> {
                            withContext(Dispatchers.Main) {
                                statusMessage = null
                            }
                        }
                        line.startsWith("ERROR:PERMISSION_DENIED") -> {
                            withContext(Dispatchers.Main) {
                                statusMessage = "Доступ до камери вимкнено в Системних параметрах Mac"
                            }
                        }
                        line.startsWith("ERROR:NO_CAMERA") -> {
                            withContext(Dispatchers.Main) {
                                statusMessage = "Вбудовану камеру не виявлено"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusMessage = "Помилка камери: ${e.message}"
                }
            }
        }

        onDispose {
            job.cancel()
            runCatching {
                process?.let { p ->
                    p.outputStream.runCatching {
                        write("STOP\n".toByteArray())
                        flush()
                    }
                    p.destroy()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        val frame = cameraFrame
        if (frame != null) {
            Image(
                bitmap = frame,
                contentDescription = "Відеопотік камери",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📷 Сканер камери",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusMessage ?: "Очікування відеопотоку...",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        PosCameraViewfinderOverlay(
            modifier = Modifier.fillMaxSize(),
            statusText = statusMessage ?: if (isScanningEnabled) "Наведіть приціл на штрих-код" else "Сканування призупинено"
        )
    }
}

/**
 * Локалізація та підготовка нативного бінарника розпізнавання штрих-кодів macOS.
 */
private fun resolveMacScannerBinary(): File? {
    val candidates = listOf(
        File("desktopApp/native/mac-camera-scanner"),
        File("kasaApp-v5/desktopApp/native/mac-camera-scanner"),
        File("../desktopApp/native/mac-camera-scanner"),
        File(System.getProperty("user.dir"), "desktopApp/native/mac-camera-scanner"),
        File(System.getProperty("user.dir"), "kasaApp-v5/desktopApp/native/mac-camera-scanner"),
        File(System.getProperty("user.home"), ".kasaapp/bin/mac-camera-scanner")
    )
    for (candidate in candidates) {
        if (candidate.exists() && candidate.isFile && candidate.canExecute()) {
            return candidate
        }
    }

    // Ресурс із JAR classpath
    val resource = object {}.javaClass.getResourceAsStream("/bin/mac-camera-scanner") ?: return null
    val tempFile = File(System.getProperty("java.io.tmpdir"), "kasa-mac-camera-scanner-v5")
    return try {
        tempFile.outputStream().use { out -> resource.copyTo(out) }
        tempFile.setExecutable(true, false)
        tempFile
    } catch (_: Exception) {
        if (tempFile.exists() && tempFile.canExecute()) tempFile else null
    }
}
