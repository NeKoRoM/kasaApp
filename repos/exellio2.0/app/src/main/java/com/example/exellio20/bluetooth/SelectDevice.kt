package com.example.exellio20.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exelioshop.adapters.RcAdapter
import com.example.exellio20.databinding.ActivitySelectDeviceBinding
import com.example.exellio20.models.ListItem
import com.example.exellio20.presentation.BluetoothDBViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * ====================================================================================================
 * АКТИВНІСТЬ ВИБОРУ BLUETOOTH-ПРИСТРОЮ (SELECT DEVICE ACTIVITY - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Відповідає за вибір спареного касового апарата, бездротових ваг або сканера штрихкодів:
 * 1. Отримує список спарених (bonded) Bluetooth-пристроїв через [BluetoothAdapter.bondedDevices].
 * 2. Відображає пристрої у списку RecyclerView через адаптер [RcAdapter].
 * 3. При натисканні повертає вибраний об'єкт [ListItem] із MAC-адресою у викликаючу активність.
 */
@AndroidEntryPoint
class SelectDevice : AppCompatActivity(), RcAdapter.Listener {
    private lateinit var binding: ActivitySelectDeviceBinding
    private var btAdapter: BluetoothAdapter? = null
    private lateinit var adapter: RcAdapter
    private val vm: BluetoothDBViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivitySelectDeviceBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        init()
    }

    /**
     * Ініціалізація адаптера Bluetooth та налаштування RecyclerView списку.
     */
    private fun init() {
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = btManager.adapter
        adapter = RcAdapter(this)
        binding.rcView.layoutManager = LinearLayoutManager(this)
        binding.rcView.adapter = adapter
        getPairedDevices()
    }

    /**
     * Зчитує спарені пристрої Android та завантажує їх в адаптер списку.
     */
    @SuppressLint("MissingPermission")
    private fun getPairedDevices() {
        val pairedDevices: Set<BluetoothDevice>? = btAdapter?.bondedDevices
        val tempList = ArrayList<ListItem>()
        pairedDevices?.forEach {
            tempList.add(ListItem(it.name ?: "Unknown", it.address))
        }
        adapter.submitList(tempList)
    }

    companion object {
        const val DEVICE_KEY = "device_key"
    }

    /**
     * Обробка вибору пристрою зі списку: повернення результату з об'єктом [ListItem].
     */
    override fun onClick(item: ListItem) {
        val i = Intent().apply {
            putExtra(DEVICE_KEY, item)
        }
        setResult(RESULT_OK, i)
        finish()
    }
}