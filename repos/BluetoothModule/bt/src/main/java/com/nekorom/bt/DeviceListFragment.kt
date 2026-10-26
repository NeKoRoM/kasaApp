package com.nekorom.bt

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import changeButtonColor
import checkBtPermission
import com.google.android.material.snackbar.Snackbar
import com.nekorom.bt.databinding.FragmentListBinding

/**
 * Фрагмент інтерфейсу користувача для керування Bluetooth-пристроями.
 * Шлях у системі: BluetoothModule/bt/src/main/java/com/nekorom/bt/DeviceListFragment.kt
 *
 * Архітектурна роль:
 * 1. Відображає два паралельних списки:
 *    - Спарені пристрої (Paired Devices): збережені в системі РРО та ваги з CheckBox вибору активного пристрою.
 *    - Виявлені пристрої (Discovered Devices): нові пристрої, знайдені в радіусі дії Bluetooth під час сканування.
 * 2. Реєструє системний BroadcastReceiver для динамічного виявлення пристроїв (ACTION_FOUND),
 *    відстеження зміни стану зв'язування (ACTION_BOND_STATE_CHANGED) та завершення сканування (ACTION_DISCOVERY_FINISHED).
 * 3. Зберігає вибрану MAC-адресу в SharedPreferences для подальшого автопідключення.
 */
class DeviceListFragment : Fragment(), ItemAdapter.Listener {
    /** Сховище налаштувань для збереження MAC-адреси обраного касового апарата */
    private var preferences: SharedPreferences? = null

    /** Системний Bluetooth адаптер */
    private var bAdapter: BluetoothAdapter? = null

    /** Лаунчер системного діалогу увімкнення Bluetooth */
    private lateinit var btLauncher: ActivityResultLauncher<Intent>

    /** Адаптер для списку знайдених у процесі сканування пристроїв */
    private lateinit var discoveryAdapter: ItemAdapter

    /** Адаптер для списку вже спарених пристроїв */
    private lateinit var itemAdapter: ItemAdapter

    /** Лаунчер запиту системних дозволів (Location, Bluetooth Connect) */
    private lateinit var pLauncher: ActivityResultLauncher<Array<String>>

    /** ViewBinding макета фрагмента */
    private lateinit var binding: FragmentListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        preferences = activity?.getSharedPreferences(BluetoothConstants.PREFERENCES, Context.MODE_PRIVATE)
        binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        intentFilters()
        checkPermissions()
        initRcViews()
        registerBtLauncher()
        initBtAdapter()
        bluetoothState()

        // Кнопка примусового запиту на увімкнення Bluetooth адаптера
        binding.imBluetoothOn.setOnClickListener {
            btLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        // Кнопка запуску сканування ефіру на наявність нових РРО/ваг
        binding.imBluetoothSearch.setOnClickListener {
            try {
                if (bAdapter?.isEnabled == true) {
                    bAdapter?.startDiscovery()
                    it.visibility = View.GONE
                    binding.pbSearch.visibility = View.VISIBLE
                }
            } catch (e: SecurityException) {
                Snackbar.make(binding.root, "${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Ініціалізація двох RecyclerView: для спарених пристроїв та для результатів сканування.
     */
    private fun initRcViews() = with(binding) {
        rcViewPaired.layoutManager = LinearLayoutManager(requireContext())
        rcViewSearch.layoutManager = LinearLayoutManager(requireContext())

        // itemAdapter: режим спарених (false - показує CheckBox вибору)
        itemAdapter = ItemAdapter(this@DeviceListFragment, false)
        // discoveryAdapter: режим виявлених (true - при кліку запускає bonding)
        discoveryAdapter = ItemAdapter(this@DeviceListFragment, true)

        rcViewPaired.adapter = itemAdapter
        rcViewSearch.adapter = discoveryAdapter
    }

    /**
     * Завантаження переліку вже спарених пристроїв із системного BluetoothAdapter.
     */
    private fun getPairedDevices() {
        try {
            val list = ArrayList<ListItem>()
            val deviceList = bAdapter?.bondedDevices as Set<BluetoothDevice>
            deviceList.forEach {
                list.add(
                    ListItem(
                        it,
                        isChecked = preferences?.getString(BluetoothConstants.MAC, "") == it.address
                    )
                )
            }

            // Показ напису "список порожній", якщо немає спарених пристроїв
            binding.tvEmptyPaired.visibility =
                if (list.isEmpty()) View.VISIBLE else View.GONE
            itemAdapter.submitList(list)
        } catch (e: SecurityException) {
            // Обробка відсутності доступу BLUETOOTH_CONNECT
        }
    }

    /**
     * Отримання посилання на системний BluetoothManager та BluetoothAdapter.
     */
    private fun initBtAdapter() {
        val bManager = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bAdapter = bManager.adapter
    }

    /**
     * Перевірка поточного стану активності Bluetooth при старті екрану.
     */
    private fun bluetoothState() {
        if (bAdapter?.isEnabled == true) {
            changeButtonColor(binding.imBluetoothOn, color = Color.GREEN)
            getPairedDevices()
        }
    }

    /**
     * Реєстрація колбека обробки результату діалогу ввімкнення Bluetooth.
     */
    private fun registerBtLauncher() {
        btLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                changeButtonColor(binding.imBluetoothOn, color = Color.GREEN)
                getPairedDevices()
                Snackbar.make(binding.root, "блютуз ON", Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(binding.root, "блютуз OFF", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Перевірка наявності дозволів і запуск системного запиту за потреби.
     */
    private fun checkPermissions() {
        if (!checkBtPermission()) {
            registerPermissionListener()
            launchBtPermissions()
        }
    }

    /**
     * Запит необхідного набору дозволів залежно від версії ОС Android (до API 31 чи після).
     */
    private fun launchBtPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            pLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    /**
     * Реєстрація лаунчера для отримання результату системного запиту прав доступу.
     */
    private fun registerPermissionListener() {
        pLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {}
    }

    /**
     * Збереження MAC-адреси вибраного пристрою в SharedPreferences.
     */
    private fun saveMac(mac: String) {
        val editor = preferences?.edit()
        editor?.putString(BluetoothConstants.MAC, mac)
        editor?.apply()
    }

    /**
     * Обробник кліку по елементу спареного списку: зберігає MAC обраного РРО.
     */
    override fun onClick(item: ListItem) {
        saveMac(item.device.address)
    }

    /**
     * Системний BroadcastReceiver для динамічного оновлення стану Bluetooth:
     * - ACTION_FOUND: знайдено новий пристрій під час сканування.
     * - ACTION_BOND_STATE_CHANGED: пристрій успішно спарено.
     * - ACTION_DISCOVERY_FINISHED: пошук завершено.
     */
    private val bReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val list = mutableSetOf<ListItem>()
                list.addAll(discoveryAdapter.currentList)
                if (device != null) list.add(ListItem(device, false))
                discoveryAdapter.submitList(list.toList())
                try {
                    Log.d("MyLogBtM", "Device: ${device?.name}")
                } catch (e: SecurityException) {
                }
            } else if (intent?.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                // Після спарення оновлюємо список спарених пристроїв
                getPairedDevices()
            } else if (intent?.action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
                // Повертаємо кнопку пошуку в активний стан
                binding.imBluetoothSearch.visibility = View.VISIBLE
                binding.pbSearch.visibility = View.GONE
            }
        }
    }

    /**
     * Реєстрація фільтрів інтенту для BroadcastReceiver.
     */
    private fun intentFilters() {
        val f1 = IntentFilter(BluetoothDevice.ACTION_FOUND)
        val f2 = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        val f3 = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        activity?.registerReceiver(bReceiver, f1)
        activity?.registerReceiver(bReceiver, f2)
        activity?.registerReceiver(bReceiver, f3)
    }
}