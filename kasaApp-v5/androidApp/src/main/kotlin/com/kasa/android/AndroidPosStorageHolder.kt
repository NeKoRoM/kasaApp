package com.kasa.android

import com.kasa.feature.pos.CashDrawerState
import com.kasa.feature.pos.FileJsonCashDrawerStorage
import com.kasa.feature.pos.FileJsonCustomerStorage
import com.kasa.feature.pos.FileJsonHeldReceiptStorage
import com.kasa.feature.pos.FileJsonReceiptJournalStorage
import com.kasa.feature.pos.FileJsonShiftStorage
import com.kasa.feature.sync.FileJsonProductStorage
import com.kasa.model.CashierShift
import com.kasa.model.Customer
import com.kasa.model.HeldReceipt
import com.kasa.model.Product
import com.kasa.model.Receipt
import com.kasa.model.SampleProductCatalog
import java.io.File

/**
 * Менеджер персистентних локальних JSON-сховищ Android POS-термінала:
 * - Каталог товарів (catalog.json);
 * - Журнал проведених фіскальних чеків (receipts_journal.json);
 * - Черга відкладених чеків касира (held_receipts.json);
 * - Стан грошової скриньки та внесення/вилучення (cash_drawer.json);
 * - База постійних клієнтів програми лояльності (customers.json);
 * - Журнал робочих змін та Z-звітів касира (shifts_journal.json).
 *
 * Забезпечує збереження та відновлення бізнес-стану після перезапуску додатку на смартфоні.
 */
class AndroidPosStorageHolder(filesDir: File) {

    val productStorage: FileJsonProductStorage = FileJsonProductStorage(File(filesDir, "catalog.json"))
    val journalStorage: FileJsonReceiptJournalStorage = FileJsonReceiptJournalStorage(File(filesDir, "receipts_journal.json"))
    val heldStorage: FileJsonHeldReceiptStorage = FileJsonHeldReceiptStorage(File(filesDir, "held_receipts.json"))
    val drawerStorage: FileJsonCashDrawerStorage = FileJsonCashDrawerStorage(File(filesDir, "cash_drawer.json"))
    val customerStorage: FileJsonCustomerStorage = FileJsonCustomerStorage(File(filesDir, "customers.json"))
    val shiftStorage: FileJsonShiftStorage = FileJsonShiftStorage(File(filesDir, "shifts_journal.json"))

    /**
     * Завантажує збережений каталог або ініціалізує демонстраційні товари при першому запуску.
     */
    fun loadInitialProducts(): List<Product> {
        val loaded = productStorage.loadCatalog()
        if (loaded.isNotEmpty()) {
            return loaded
        }
        val samples = SampleProductCatalog.createSampleCatalog()
        productStorage.saveCatalog(samples)
        return samples
    }

    /**
     * Завантажує список покупців програми лояльності або наповнює базовими тестовими клієнтами.
     */
    fun loadInitialCustomers(): List<Customer> {
        val loaded = customerStorage.loadCustomers()
        if (loaded.isNotEmpty()) {
            return loaded
        }
        val defaults = FileJsonCustomerStorage.defaultSampleCustomers()
        customerStorage.saveCustomers(defaults)
        return defaults
    }

    /**
     * Відновлює стан відкритої касової зміни (якщо зміна не була закрита Z-звітом та не застаріла >24 год).
     */
    fun loadInitialShift(): CashierShift? {
        val current = shiftStorage.loadCurrentShift() ?: return null
        if (current.isExpired24h) {
            shiftStorage.saveCurrentShift(null)
            return null
        }
        return current
    }

    /**
     * Відновлює баланс готівки у скриньці каси.
     */
    fun loadInitialDrawerBalance(): Long {
        return drawerStorage.loadDrawerState().balanceKopecks
    }

    /**
     * Завантажує список відкладених чеків.
     */
    fun loadInitialHeldReceipts(): List<HeldReceipt> {
        return heldStorage.loadHeldReceipts()
    }

    /**
     * Завантажує історію проведених чеків для журналу касира.
     */
    fun loadJournal(): List<Receipt> {
        return journalStorage.loadJournal()
    }

    /**
     * Зберігає оновлений баланс грошової скриньки.
     */
    fun saveDrawerBalance(newBalanceKopecks: Long) {
        drawerStorage.saveDrawerState(CashDrawerState(newBalanceKopecks, System.currentTimeMillis()))
    }
}
