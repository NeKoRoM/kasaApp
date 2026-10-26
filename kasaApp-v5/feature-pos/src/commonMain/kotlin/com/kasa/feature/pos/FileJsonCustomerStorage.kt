package com.kasa.feature.pos

import com.kasa.model.Customer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Файлове JSON-сховище бази постійних покупців та карток програми лояльності.
 */
class FileJsonCustomerStorage(
    private val file: File,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
) {
    fun loadCustomers(): List<Customer> = synchronized(this) {
        if (!file.exists()) {
            val defaults = defaultSampleCustomers()
            saveCustomers(defaults)
            return defaults
        }
        return try {
            val content = file.readText(Charsets.UTF_8)
            if (content.isBlank()) return defaultSampleCustomers()
            json.decodeFromString<List<Customer>>(content)
        } catch (_: Exception) {
            defaultSampleCustomers()
        }
    }

    fun saveCustomers(customers: List<Customer>) = synchronized(this) {
        try {
            file.parentFile?.mkdirs()
            val jsonString = json.encodeToString(customers)
            file.writeText(jsonString, Charsets.UTF_8)
        } catch (_: Exception) {
            // Запобігаємо збою застосунку при винятках запису на диск
        }
    }

    fun findCustomerByBarcode(barcode: String): Customer? {
        val clean = barcode.trim()
        if (clean.isBlank()) return null
        return loadCustomers().find {
            it.cardBarcode.equals(clean, ignoreCase = true)
        }
    }

    fun findCustomerByPhone(phone: String): Customer? {
        val clean = phone.replace(Regex("[^0-9]"), "")
        if (clean.isBlank()) return null
        return loadCustomers().find {
            it.phone.replace(Regex("[^0-9]"), "").endsWith(clean.takeLast(9))
        }
    }

    fun searchCustomers(query: String): List<Customer> {
        val q = query.trim()
        if (q.isBlank()) return loadCustomers()
        val digitsOnly = q.replace(Regex("[^0-9]"), "")
        return loadCustomers().filter { c ->
            c.name.contains(q, ignoreCase = true) ||
                (digitsOnly.isNotBlank() && c.phone.contains(digitsOnly)) ||
                c.cardBarcode.contains(q, ignoreCase = true)
        }
    }

    companion object {
        fun defaultSampleCustomers(): List<Customer> = listOf(
            Customer(
                id = 1L,
                name = "Іван Петренко",
                phone = "+380501112233",
                cardBarcode = "990000000001",
                discountPercent = 5.0,
                bonusBalanceKopecks = 25000L
            ),
            Customer(
                id = 2L,
                name = "Олена Ковальчук",
                phone = "+380672223344",
                cardBarcode = "990000000002",
                discountPercent = 10.0,
                bonusBalanceKopecks = 54000L
            ),
            Customer(
                id = 3L,
                name = "Михайло Бойко",
                phone = "+380933334455",
                cardBarcode = "990000000003",
                discountPercent = 3.0,
                bonusBalanceKopecks = 12000L
            )
        )
    }
}
