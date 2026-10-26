package com.kasa.feature.pos

import com.kasa.model.BarcodeParser
import com.kasa.model.Customer
import com.kasa.model.KeyboardLayoutCorrector
import com.kasa.model.ParsedBarcode
import com.kasa.model.Product
import com.kasa.model.Receipt

/**
 * Чиста логіка розбору та маршрутизації вхідних штрих-кодів від сканера (Barcode Processing Domain Logic).
 */
object PosBarcodeLogic {

    sealed interface BarcodeScanAction {
        data class SetPrefilledBarcode(val barcode: String, val message: String) : BarcodeScanAction
        data class OpenProductEdit(val product: Product?, val prefilledBarcode: String?, val message: String) : BarcodeScanAction
        data class EnterExciseStamp(val itemIndex: Int, val stamp: String) : BarcodeScanAction
        data class AttachCustomer(val customer: Customer) : BarcodeScanAction
        data class LoadReturnReceipt(val receipt: Receipt) : BarcodeScanAction
        data class IncrementExistingItem(val itemIndex: Int, val newQuantity: Double) : BarcodeScanAction
        data class AddProduct(val product: Product, val quantity: Double?) : BarcodeScanAction
        data class ProductNotFound(val cleanBarcode: String, val nextPlu: Long, val hasLetters: Boolean) : BarcodeScanAction
    }

    fun parseBarcodeAction(current: PosState, rawBarcode: String): BarcodeScanAction? {
        var cleanBarcode = rawBarcode.trim()
        if (cleanBarcode.isBlank()) return null

        if (current.isProductEditDialogOpen) {
            return BarcodeScanAction.SetPrefilledBarcode(
                barcode = cleanBarcode,
                message = "Штрих-код '$cleanBarcode' додано до товару"
            )
        }

        if (current.isCatalogManagementDialogOpen) {
            val matchingProduct = current.catalogProducts.find { p ->
                p.barcodes.any { it.equals(cleanBarcode, ignoreCase = true) } || p.id.toString() == cleanBarcode
            }
            return if (matchingProduct != null) {
                BarcodeScanAction.OpenProductEdit(
                    product = matchingProduct,
                    prefilledBarcode = null,
                    message = "Товар '${matchingProduct.name}' відкрито для редагування"
                )
            } else {
                BarcodeScanAction.OpenProductEdit(
                    product = null,
                    prefilledBarcode = cleanBarcode,
                    message = "Новий штрих-код '$cleanBarcode': створіть товар"
                )
            }
        }

        var multiplierQty: Double? = null
        val asteriskIndex = cleanBarcode.indexOfAny(charArrayOf('*', 'x', 'X', 'х', 'Х'))
        if (asteriskIndex in 1 until cleanBarcode.length - 1) {
            val potentialQty = cleanBarcode.substring(0, asteriskIndex).trim().replace(',', '.').toDoubleOrNull()
            if (potentialQty != null && potentialQty > 0.0) {
                multiplierQty = potentialQty
                cleanBarcode = cleanBarcode.substring(asteriskIndex + 1).trim()
            }
        } else {
            val spaceIndex = cleanBarcode.indexOf(' ')
            if (spaceIndex in 1 until cleanBarcode.length - 1) {
                val potentialQty = cleanBarcode.substring(0, spaceIndex).trim().replace(',', '.').toDoubleOrNull()
                if (potentialQty != null && potentialQty > 0.0) {
                    multiplierQty = potentialQty
                    cleanBarcode = cleanBarcode.substring(spaceIndex + 1).trim()
                }
            }
        }

        val isExciseUrl = cleanBarcode.contains("t.gov.ua/")
        if (current.activeExcisePromptIndex != null) {
            val stamp = if (isExciseUrl) cleanBarcode.substringAfter("t.gov.ua/").take(16).trim() else cleanBarcode
            return BarcodeScanAction.EnterExciseStamp(current.activeExcisePromptIndex, stamp)
        }
        if (isExciseUrl) {
            val targetIndex = current.items.indexOfFirst { it.requiresExciseStamp && it.exciseStamp.isNullOrBlank() }
            if (targetIndex != -1) {
                val stamp = cleanBarcode.substringAfter("t.gov.ua/").take(16).trim()
                return BarcodeScanAction.EnterExciseStamp(targetIndex, stamp)
            }
        }

        val matchingCustomer = current.customersList.find {
            it.cardBarcode.equals(cleanBarcode, ignoreCase = true) ||
            (cleanBarcode.length >= 7 && it.phone.replace(Regex("[^0-9]"), "").endsWith(cleanBarcode.replace(Regex("[^0-9]"), "")))
        }
        if (matchingCustomer != null) {
            return BarcodeScanAction.AttachCustomer(matchingCustomer)
        }

        val matchingJournalReceipt = current.journalReceipts.find {
            it.fiscalCheckNumber?.equals(cleanBarcode, ignoreCase = true) == true ||
            it.id.equals(cleanBarcode, ignoreCase = true) ||
            (cleanBarcode.startsWith("CHECK#") && it.fiscalCheckNumber?.equals(cleanBarcode.substringAfter("CHECK#"), ignoreCase = true) == true)
        }
        if (matchingJournalReceipt != null) {
            return BarcodeScanAction.LoadReturnReceipt(matchingJournalReceipt)
        }

        val parsedBarcode = BarcodeParser.parse(cleanBarcode)
        val product: Product?
        val parsedQuantity: Double?

        when (parsedBarcode) {
            is ParsedBarcode.Weighted -> {
                product = current.catalogProducts.find {
                    it.id == parsedBarcode.plu || it.barcodes.any { b -> b.equals(cleanBarcode, ignoreCase = true) }
                }
                parsedQuantity = parsedBarcode.weightKilograms
            }
            is ParsedBarcode.PriceEmbedded -> {
                product = current.catalogProducts.find {
                    it.id == parsedBarcode.plu || it.barcodes.any { b -> b.equals(cleanBarcode, ignoreCase = true) }
                }
                parsedQuantity = if (product != null && product.price > 0.0) {
                    val priceKopecks = Math.round(product.price * 100.0)
                    if (priceKopecks > 0L) {
                        (parsedBarcode.totalPriceKopecks.toDouble() / priceKopecks.toDouble())
                    } else {
                        1.0
                    }
                } else {
                    1.0
                }
            }
            is ParsedBarcode.Standard -> {
                val pluCandidate = cleanBarcode.toLongOrNull()
                product = current.catalogProducts.find { p ->
                    (pluCandidate != null && p.id == pluCandidate) ||
                    p.barcodes.any { it.equals(cleanBarcode, ignoreCase = true) } ||
                    p.name.equals(cleanBarcode, ignoreCase = true) ||
                    KeyboardLayoutCorrector.matchesQuery(p.name, cleanBarcode)
                }
                parsedQuantity = multiplierQty
            }
        }

        if (product == null) {
            val hasLetters = cleanBarcode.any { it.isLetter() }
            val nextPlu = (current.catalogProducts.maxOfOrNull { it.id } ?: 0L) + 1L
            return BarcodeScanAction.ProductNotFound(
                cleanBarcode = cleanBarcode,
                nextPlu = nextPlu,
                hasLetters = hasLetters
            )
        }

        val existingIndex = current.items.indexOfFirst { it.productId == product.id && !it.requiresExciseStamp }
        if (!product.isWeighted && existingIndex != -1 && parsedQuantity == null) {
            val existingItem = current.items[existingIndex]
            return BarcodeScanAction.IncrementExistingItem(existingIndex, existingItem.quantity + 1.0)
        }

        return BarcodeScanAction.AddProduct(product, parsedQuantity)
    }
}
