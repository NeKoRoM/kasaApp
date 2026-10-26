// =================================================================================================
// АНАЛІЗАТОР ШТРИХ-КОДІВ CAMERAX + GOOGLE ML KIT (BARCODE SCANNER PIPELINE)
//
// Архітектурний шар: Domain / Presentation Pipeline (CameraX ImageAnalysis).
// Призначення:
// Обробляє потік відеокадрів із камери планшета/смартфона в реальному часі для автоматичного
// зчитування штрих-кодів товарів (EAN-13, EAN-8, Code-128, QR) за допомогою Google ML Kit.
// =================================================================================================

package com.nekorom.kasaapp.domain

import android.graphics.ImageFormat
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Аналізатор кадрів CameraX для детекції штрих-кодів за допомогою Google ML Kit.
 *
 * @param callback Функція зворотного виклику для передачі знайденого списку розпізнаних [Barcode].
 */
class BarCodeAnalyser(
    val callback: (List<Barcode>) -> Unit
) : ImageAnalysis.Analyzer {

    /**
     * Покадровий аналіз вхідного зображення з відеопотоку камери.
     * Обов'язково закриває [imageProxy] після завершення аналізу для запобігання витоку пам'яті
     * та зависання конвеєра CameraX.
     */
    @OptIn(ExperimentalGetImage::class) 
    override fun analyze(imageProxy: ImageProxy) {
        // Конфігурація сканера ML Kit за замовчуванням (підтримує всі формати 1D та 2D кодів)
        val options = BarcodeScannerOptions.Builder().build()
        val scanner = BarcodeScanning.getClient(options)
        val image = imageProxy

        image.image?.let {
            // Перевіряємо відповідність формату кольору стандартному Android YUV_420_888
            if (image.format == ImageFormat.YUV_420_888) {
                // Створюємо InputImage з Bitmap з урахуванням поточного кута повороту пристрою
                val imageToProcess = (InputImage.fromBitmap(image.toBitmap(), image.imageInfo.rotationDegrees))

                // Асинхронний запуск нейромережевого детектора ML Kit
                scanner.process(imageToProcess)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            callback(barcodes)
                        }
                    }
                    .addOnFailureListener {
                        image.image?.close()
                        image.close()
                    }
            }
            // Закриття проксі обов'язкове для передачі наступного кадру в чергу
            imageProxy.close()
        }
    }
}