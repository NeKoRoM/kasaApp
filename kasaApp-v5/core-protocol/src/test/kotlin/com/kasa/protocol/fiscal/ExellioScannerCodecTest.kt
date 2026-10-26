package com.kasa.protocol.fiscal

import com.kasa.model.PaymentType
import com.kasa.model.Receipt
import com.kasa.model.ReceiptItem
import com.kasa.model.TaxGroup
import com.kasa.protocol.transport.KasaTransport
import com.kasa.protocol.transport.TransportState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExellioScannerCodecTest {

    private val windows1251: Charset = Charset.forName("windows-1251")

    @Test
    fun testEncodePluSaleCodeMatchesKasaApp30Algorithm() {
        // PLU = 1, кількість = 1.000 -> "23" + "00001" + "01" + "000" = "230000101000"
        // Сума: 2*3 + 3*1 + 0 + 0 + 0 + 0 + 1*3 + 0 + 1*3 + 0 + 0 + 0 = 6 + 3 + 3 + 3 = 15 -> CS = (10 - 5) = 5
        val code1 = ExellioScannerCodec.encodePluSaleCode(pluCode = 1L, quantity = 1.0)
        assertEquals("2300001010005", code1)

        // PLU = 3, кількість = 1.250 кг -> "23" + "00003" + "01" + "250" = "230000301250"
        // Індекси: 0:'2'*3=6, 1:'3'*1=3, 6:'3'*3=9, 8:'1'*3=3, 9:'2'*1=2, 10:'5'*3=15 -> 6+3+9+3+2+15 = 38 -> CS = 2
        val codeWeighted = ExellioScannerCodec.encodePluSaleCode(pluCode = 3L, quantity = 1.25)
        assertEquals("2300003012502", codeWeighted)
    }

    @Test
    fun testEncodePluSaleBytesAppendsCarriageReturn() {
        val bytes = ExellioScannerCodec.encodePluSaleBytes(pluCode = 1L, quantity = 1.0)
        val decoded = String(bytes, windows1251)
        assertEquals("2300001010005\r", decoded)
        assertEquals(0x0D.toByte(), bytes.last())
    }

    @Test
    fun testScannerRegistrationModeExecutesBarcodesAndExciseStampsInOrder() = runTest {
        val sentPayloads = mutableListOf<String>()

        val mockScannerTransport = object : KasaTransport {
            override val state: StateFlow<TransportState> =
                MutableStateFlow(TransportState.Connected("EXELLIO DP-25"))
            override val isScannerRegistrationMode: Boolean = true

            override suspend fun connect(target: String): Boolean = true

            override suspend fun sendFrame(frame: ByteArray): Boolean {
                sentPayloads.add(String(frame, windows1251))
                return true
            }

            override suspend fun receiveFrame(timeoutMs: Long): ByteArray? = null
            override suspend fun disconnect() {}
        }

        val engine = FiscalReceiptEngine(mockScannerTransport)
        val receipt = Receipt(
            id = "REC-EXELLIO-1",
            createdAt = 1700000000000L,
            cashierId = 1L,
            paymentType = PaymentType.CASH,
            items = listOf(
                ReceiptItem(
                    productId = 6L,
                    productName = "Цигарки Marlboro",
                    priceKopecks = 9800L,
                    quantity = 1.0,
                    taxGroup = TaxGroup.GROUP_A_EXCISE,
                    uktzed = "2402209020",
                    exciseStamp = "AA123456"
                ),
                ReceiptItem(
                    productId = 1L,
                    productName = "Хліб пшеничний",
                    priceKopecks = 2450L,
                    quantity = 2.0,
                    taxGroup = TaxGroup.GROUP_A
                )
            )
        )

        val result = engine.executeReceipt(receipt)
        assertTrue(result is FiscalReceiptResult.Success)
        assertEquals("РРО", result.fiscalCheckNumber)

        // Перевіряємо порядок відправки: спочатку товар без акцизу (PLU 1, к-сть 2),
        // потім підакцизний товар (PLU 6, к-сть 1) і одразу його акцизна марка
        assertEquals(3, sentPayloads.size)
        assertEquals(ExellioScannerCodec.encodePluSaleCode(1L, 2.0) + "\r", sentPayloads[0])
        assertEquals(ExellioScannerCodec.encodePluSaleCode(6L, 1.0) + "\r", sentPayloads[1])
        assertEquals("AA123456\r", sentPayloads[2])
    }
}
