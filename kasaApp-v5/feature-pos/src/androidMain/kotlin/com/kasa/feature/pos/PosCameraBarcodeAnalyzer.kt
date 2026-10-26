package com.kasa.feature.pos

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Аналізатор кадрів CameraX з розпізнаванням штрих-кодів через Google ML Kit.
 * Підтримує всі формати роздрібної торгівлі: EAN-13, EAN-8, UPC, Code 128, QR-коди, е-акциз.
 */
class PosCameraBarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_ITF
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    private var lastScannedCode: String? = null
    private var lastScannedTimeMs: Long = 0L
    var isScanningEnabled: Boolean = true

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!isScanningEnabled) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val now = System.currentTimeMillis()
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue ?: continue
                    if (PosCameraDebounceLogic.shouldAcceptScannedCode(rawValue, lastScannedCode, lastScannedTimeMs, now)) {
                        val accepted = PosCameraDebounceLogic.normalizeScannedBarcode(rawValue)
                        lastScannedCode = accepted
                        lastScannedTimeMs = now
                        onBarcodeDetected(accepted)
                        break
                    }
                }
            }
            .addOnFailureListener {
                // Ігноруємо поодинокі збої розпізнавання розмитих кадрів
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun reset() {
        lastScannedCode = null
        lastScannedTimeMs = 0L
    }
}
