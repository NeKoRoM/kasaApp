package com.example.exelioshop.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exelioshop.dataclass.ListItem
import com.example.exelioshop.adapters.RcAdapter
import com.example.exelioshop.databinding.ActivitySelectDeviceBinding

/**
 * [SelectDevice] — екран вибору спареного Bluetooth-пристрою (фіскального РРО або сканера).
 * 
 * Отримує перелік прив'язаних пристроїв (`btAdapter.bondedDevices`) та відображає їх у списку [RcAdapter].
 * При виборі касиром пристрою пакує [ListItem] в `Intent` із ключем [DEVICE_KEY] та повертає статус `RESULT_OK`.
 */
class SelectDevice : AppCompatActivity(), RcAdapter.Listener {
    private lateinit var binding: ActivitySelectDeviceBinding
    private var btAdapter: BluetoothAdapter? = null
    private lateinit var adapter: RcAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivitySelectDeviceBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        init()
    }

    /**
     * Ініціалізація BluetoothAdapter, списку RecyclerView та зчитування спарених пристроїв.
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
     * Зчитування списку вже спарених (bonded) Bluetooth-пристроїв із системи Android.
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
        /** Ключ передачі даних обраного пристрою в Intent результату */
        const val DEVICE_KEY = "device_key"
    }

    /**
     * Обробка вибору конкретного Bluetooth-пристрою касиром.
     */
    override fun onClick(item: ListItem) {
        val i = Intent().apply {
            putExtra(DEVICE_KEY, item)
        }
        setResult(RESULT_OK, i)
        finish()
    }
}