package com.kasa.model

import kotlin.test.Test
import kotlin.test.assertTrue

class ReceiptTextRendererTest {

    @Test
    fun testRenderReceiptText() {
        val item1 = ReceiptItem(
            productId = 1L,
            productName = "Хліб білий",
            priceKopecks = 2450L,
            quantity = 2.0,
            taxGroup = TaxGroup.GROUP_A
        )
        val item2 = ReceiptItem(
            productId = 3L,
            productName = "Горілка Преміум",
            priceKopecks = 15000L,
            quantity = 1.0,
            taxGroup = TaxGroup.GROUP_A_EXCISE,
            exciseStamp = "АА12345678"
        )

        val receipt = Receipt(
            id = "test-rec-1",
            createdAt = 1700000000000L,
            cashierId = 1L,
            items = listOf(item1, item2),
            paymentType = PaymentType.CASH,
            fiscalCheckNumber = "48102"
        )

        val rendered = ReceiptTextRenderer.render(
            receipt = receipt,
            headerLines = listOf("ТОВ \"РОЗДРІБ ТРЕЙД\"", "м. Київ, вул. Хрещатик, 1", "ФН 12345678"),
            footerLines = listOf("ДЯКУЄМО ЗА ПОКУПКУ!"),
            tapeWidth = 40
        )

        assertTrue(rendered.contains("ТОВ \"РОЗДРІБ ТРЕЙД\""))
        assertTrue(rendered.contains("ФІСКАЛЬНИЙ ЧЕК"))
        assertTrue(rendered.contains("ЧЕК № 48102"))
        assertTrue(rendered.contains("Хліб білий"))
        assertTrue(rendered.contains("2.000 x 24.50"))
        assertTrue(rendered.contains("Горілка Преміум"))
        assertTrue(rendered.contains("АКЦИЗ: АА12345678"))
        assertTrue(rendered.contains("ДО ОПЛАТИ"))
        assertTrue(rendered.contains("ГОТІВКА"))
        assertTrue(rendered.contains("АКЦИЗНИЙ ЗБІР 5.00%"))
        assertTrue(rendered.contains("ДЯКУЄМО ЗА ПОКУПКУ!"))
        assertTrue(rendered.contains("ПЕРЕВІРКА ЧЕКА В ДПС:"))
        assertTrue(rendered.contains("cabinet.tax.gov.ua"))
    }

    @Test
    fun testRenderReturnReceipt() {
        val item = ReceiptItem(
            productId = 1L,
            productName = "Хліб білий",
            priceKopecks = 2450L,
            quantity = 1.0,
            taxGroup = TaxGroup.GROUP_A
        )

        val returnReceipt = Receipt(
            id = "return-1",
            createdAt = 1700000000000L,
            cashierId = 1L,
            items = listOf(item),
            paymentType = PaymentType.CARD,
            receiptType = ReceiptType.RETURN,
            fiscalCheckNumber = "48103"
        )

        val rendered = ReceiptTextRenderer.render(returnReceipt)
        assertTrue(rendered.contains("ВИДАТКОВИЙ ЧЕК"))
        assertTrue(rendered.contains("ПОВЕРНЕННЯ"))
        assertTrue(rendered.contains("БЕЗГОТІВКОВА (КАРТКА)"))
    }

    @Test
    fun testRenderCombinedSplitReceipt() {
        val item = ReceiptItem(
            productId = 1L,
            productName = "Кава в зернах",
            priceKopecks = 20000L,
            quantity = 1.0,
            taxGroup = TaxGroup.GROUP_A
        )

        val splitReceipt = Receipt(
            id = "split-1",
            createdAt = 1700000000000L,
            cashierId = 1L,
            items = listOf(item),
            paymentType = PaymentType.COMBINED,
            splitCashKopecks = 5000L,
            splitCardKopecks = 15000L,
            fiscalCheckNumber = "48104"
        )

        val rendered = ReceiptTextRenderer.render(splitReceipt)
        assertTrue(rendered.contains("БЕЗГОТІВКА (КАРТКА)"))
        assertTrue(rendered.contains("150.00"))
        assertTrue(rendered.contains("ГОТІВКА"))
        assertTrue(rendered.contains("50.00"))
    }

    @Test
    fun testRenderReceiptWithCustomerLoyalty() {
        val customer = Customer(
            id = 101L,
            name = "Іван Петренко",
            phone = "+380501112233",
            cardBarcode = "2900000000018",
            discountPercent = 5.0
        )
        val item = ReceiptItem(
            productId = 1L,
            productName = "Кава Арабіка",
            priceKopecks = 6000L,
            quantity = 1.0,
            taxGroup = TaxGroup.GROUP_A
        )
        val receipt = Receipt(
            id = "r-loyalty",
            createdAt = 1700000000000L,
            cashierId = 1L,
            items = listOf(item),
            paymentType = PaymentType.CASH,
            customer = customer,
            discountPercent = 5.0,
            fiscalCheckNumber = "50001"
        )

        val rendered = ReceiptTextRenderer.render(receipt)
        assertTrue(rendered.contains("КЛІЄНТ: ІВАН ПЕТРЕНКО"))
        assertTrue(rendered.contains("КАРТКА:"))
        assertTrue(rendered.contains("**** 0018"))
        assertTrue(rendered.contains("ЗНИЖКА 5.0%"))
    }
}

