// =================================================================================================
// В'ЮМОДЕЛЬ ВИБОРУ BLUETOOTH ПРИСТРОЮ (BLUETOOTH SELECTION VIEWMODEL)
//
// Архітектурний шар: Presentation Layer (MVVM ViewModel).
// Призначення:
// Забезпечує збереження стану та передачу вибраної MAC-адреси Bluetooth пристрою
// між DeviceListFragment та батьківською BtActivity.
// =================================================================================================

package com.nekorom.kasaapp.btactivity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel для збереження стану вибору пристрою в [BtActivity].
 */
class BtViewModel: ViewModel() {

    // Поточне ім'я вибраного пристрою
    val currentName: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    // Приватне джерело обраної MAC-адреси
    private val _mac = MutableLiveData<String>()

    // Публічний потік даних MAC-адреси для спостереження з Activity
    val mac : LiveData<String> = _mac

    /**
     * Оновлює значення обраної MAC-адреси при натисканні на елемент списку пристроїв.
     */
    fun setMac(mac: String){
        _mac.value = mac
    }
}