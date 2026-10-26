// =================================================================================================
// ФРАГМЕНТ СКАНУВАННЯ ТА СПАРЮВАННЯ BLUETOOTH ПРИСТРОЇВ (BLUETOOTH DEVICE LIST FRAGMENT)
//
// Архітектурний шар: Presentation / Hardware Pairing Layer (Fragment + ViewBinding).
// Призначення:
// 1. Сканування ефіру на наявність фіскальних реєстраторів (Datecs FP-320, CMP-10, тощо) та ваг.
// 2. Відображення списку вже спарених пристроїв (Bonded Devices) та нових знайдених пристроїв (Discovery).
// 3. Запит необхідних дозволів середовища Android 12+ (BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION, CAMERA).
// 4. Збереження обраної MAC-адреси в SharedPreferences та передача її у BtViewModel для Compose.
// =================================================================================================

package com.nekorom.kasaapp.btactivity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import checkBtPermission
import com.google.android.material.snackbar.Snackbar
import com.nekorom.bt.BluetoothConstants
import com.nekorom.bt.ItemAdapter
import com.nekorom.bt.ListItem
import com.nekorom.bt.databinding.FragmentListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.NullPointerException

/**
 * Фрагмент списку Bluetooth-пристроїв із підтримкою життєвого циклу адаптера, пошуку та спарювання.
 */
class DeviceListFragment : Fragment(), ItemAdapter.Listener {
    private var preferences: SharedPreferences? = null
    private var bAdapter: BluetoothAdapter? = null
    private lateinit var btLauncher: ActivityResultLauncher<Intent>
    private lateinit var discoveryAdapter: ItemAdapter
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var pLauncher: ActivityResultLauncher<Array<String>>
    val listFindedDevice = mutableListOf<ListItem>()
    val listPariradDevice = mutableListOf<ListItem>()
    private var isAlertDialogShown = false

    private val vm : BtViewModel by activityViewModels()










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
        registerBtLauncher()

        intentFilters()
        checkPermissions()
        initRcViews()

        initBtAdapter()
        bluetoothState()
        sharedPrefCheck()

        // Кнопка примусового запиту на увімкнення Bluetooth
        binding.imBluetoothOn.setOnClickListener {
            btLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        // Перемикач стану Bluetooth
        binding.switch1.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                btLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }

        // Клік по заглушці "Немає спарених пристроїв" для активації BT
        binding.tvEmptyPaired.setOnClickListener {
            btLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            it.visibility = View.GONE
        }

        // Кнопка запуску сканування ефіру для пошуку нових пристроїв
        binding.buttonEmptySearch.setOnClickListener {
            try {
                if (bAdapter?.isEnabled == true) {
                    bAdapter?.startDiscovery()
                    it.visibility = View.GONE
                    binding.imBluetoothSearch.visibility = View.GONE
                    binding.pbSearch.visibility = View.VISIBLE
                }
            } catch (e: SecurityException) {
                Snackbar.make(binding.root, "${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }

        // Дублююча кнопка пошуку в панелі інструментів
        binding.buttonEmptySearchInBar.setOnClickListener {
            try {
                if (bAdapter?.isEnabled == true) {
                    bAdapter?.startDiscovery()
                    it.visibility = View.GONE
                    binding.imBluetoothSearch.visibility = View.GONE
                    binding.pbSearch.visibility = View.VISIBLE
                }
            } catch (e: SecurityException) {
                Snackbar.make(binding.root, "${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }

        // Фоновий моніторинг увімкненості Bluetooth: попереджає користувача, якщо адаптер вимкнено
        CoroutineScope(Dispatchers.Default).launch {
            var alertDialog: AlertDialog? = null
            while (true) {
                delay(1000)
                CoroutineScope(Dispatchers.Main).launch {
                    if (bAdapter?.isEnabled == false) {
                        changeButtonColor(binding.imBluetoothOn, color = Color.RED)
                        alertDialog = dialog(
                            title = "блютуз виключино",
                            message = "включити блютуз?",
                            positiveOnClick = {
                                btLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            },
                            negativeOnClick = {
                                Toast.makeText(activity, "відміна", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        changeButtonColor(binding.imBluetoothOn, color = Color.GREEN)
                        alertDialog?.dismiss()
                    }
                }
            }
        }

        // Іконка сканування
        binding.imBluetoothSearch.setOnClickListener {
            try {
                if (bAdapter?.isEnabled == true) {
                    bAdapter?.startDiscovery()
                    it.visibility = View.GONE
                    binding.buttonEmptySearch.visibility = View.GONE
                    binding.pbSearch.visibility = View.VISIBLE
                }
            } catch (e: SecurityException) {
                Snackbar.make(binding.root, "${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Відображення системного діалогу із підтвердженням дії.
     */
    private fun dialog(
        title: String,
        message: String,
        positiveOnClick: () -> Unit,
        negativeOnClick: () -> Unit
    ): AlertDialog? {
        val alertDialog: AlertDialog? = activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setTitle(title)
                setMessage(message)
                setPositiveButton("так") { dialog, id ->
                    positiveOnClick.invoke()
                    isAlertDialogShown = false
                }
                setNegativeButton("ні") { dialog, id ->
                    negativeOnClick.invoke()
                    isAlertDialogShown = false
                }
            }
            builder.create()
        }
        if (!isAlertDialogShown) {
            isAlertDialogShown = true
            alertDialog?.show()
        }
        return alertDialog
    }

    private fun initRcViews() = with(binding){
    /**
     * Ініціалізація списків RecyclerView для спарених та знайдених Bluetooth-пристроїв.
     */
    private fun initRcViews() = with(binding){
        rcViewPaired.layoutManager = LinearLayoutManager(requireContext())
        rcViewSearch.layoutManager = LinearLayoutManager(requireContext())

        itemAdapter = ItemAdapter(this@DeviceListFragment, false)
        discoveryAdapter = ItemAdapter(this@DeviceListFragment, true)

        rcViewPaired.adapter = itemAdapter
        rcViewSearch.adapter = discoveryAdapter
    }

    /**
     * Отримання списку вже спарених пристроїв (Bonded devices) з локального Bluetooth-стека.
     */
    private fun getPairedDevices(){
        try {
            val list = ArrayList<ListItem>()
            val deviceList = bAdapter?.bondedDevices as Set<BluetoothDevice>
            deviceList.forEach{
                list.add(
                    ListItem(
                        it,
                        isChecked = preferences?.getString(BluetoothConstants.MAC, "") == it.address
                    )
                )
            }

            binding.tvEmptyPaired.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            itemAdapter.submitList(list)
        } catch (e: SecurityException) {
            // Виключення при відсутності дозволу BLUETOOTH_CONNECT
        }
    }

    /**
     * Ініціалізація системного адаптера Bluetooth.
     */
    private fun initBtAdapter(){
        val bManeger = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bAdapter = bManeger.adapter
    }

    /**
     * Перевірка наявності збереженої в налаштуваннях MAC-адреси РРО та пропозиція підключення.
     */
    private fun sharedPrefCheck(){
        val mac = preferences?.getString(BluetoothConstants.MAC, "")
        if (!mac.isNullOrEmpty() && bAdapter?.isEnabled == true) {
            dialog(
                title = "підєднання",
                message = "під'єднати останній записаний пристрій?",
                positiveOnClick = {
                    itemAdapter.listenerList.forEach {
                        it.onClickMac(mac)
                    }
                },
                negativeOnClick = {}
            )
        }
    }

    /**
     * Оновлення UI відповідно до поточного стану активності адаптера Bluetooth.
     */
    private fun bluetoothState() {
        if (bAdapter?.isEnabled == true) {
            changeButtonColor(binding.imBluetoothOn, color = Color.GREEN)
            getPairedDevices()
        } else {
            changeButtonColor(binding.imBluetoothOn, color = Color.RED)
        }
    }

    /**
     * Зміна кольору статусних індикаторів зв'язку Bluetooth.
     */
    private fun changeButtonColor(button: ImageView, color: Int){
        val drawable = button.drawable
        try {
            DrawableCompat.setTint(drawable, color)
            button.setImageDrawable(drawable)
        } catch (e: NullPointerException) {
        }
        binding.switch1.isChecked = (color == Color.GREEN)
        if (color != Color.GREEN) {
            binding.tvEmptyPaired.visibility = View.VISIBLE
        }
    }

    /**
     * Реєстрація лаунчера активації Bluetooth через системне діалогове вікно.
     */
    private fun registerBtLauncher() {
        btLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                changeButtonColor(binding.imBluetoothOn, color = Color.GREEN)
                getPairedDevices()
            }
        }
    }

    /**
     * Перевірка наявності необхідних дозволів Bluetooth / Location / Camera.
     */
    private fun checkPermissions(){
        if (!checkBtPermission()){
            registerPermissionListener()
            launchBtPermissions()
        }
    }

    /**
     * Запит дозволів: на Android 12+ (API 31+) запитується BLUETOOTH_CONNECT,
     * на старіших версіях — ACCESS_FINE_LOCATION.
     */
    private fun launchBtPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA
                )
            )
        } else {
            pLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA
                )
            )
        }
    }

    /**
     * Реєстрація слухача результатів запиту множинних системних дозволів.
     */
    private fun registerPermissionListener(){
        pLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {}
    }

    /**
     * Збереження MAC-адреси вибраного РРО в SharedPreferences.
     */
    private fun saveMac(mac: String){
        val editor = preferences?.edit()
        editor?.putString(BluetoothConstants.MAC, mac)
        editor?.apply()
        Log.d(":Bt", "mac into shared pref :$mac")
    }

    /**
     * Колбек кліку на елемент списку: зберігає MAC-адресу в SharedPreferences.
     */
    override fun onClick(item: ListItem) {
        saveMac(item.device.address)
    }

    /**
     * Колбек вибору MAC-адреси: передає адресу у BtViewModel для закриття активності та повернення в Compose.
     */
    override fun onClickMac(mac: String) {
        vm.setMac(mac)
    }

    /**
     * Широкомовний приймач системних подій Bluetooth:
     * - ACTION_FOUND: знайдено новий пристрій під час сканування (Discovery).
     * - ACTION_BOND_STATE_CHANGED: змінено стан спарювання пристрою.
     * - ACTION_DISCOVERY_FINISHED: завершено цикл пошуку в ефірі.
     */
    private val bReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val list = mutableSetOf<ListItem>()
                list.addAll(discoveryAdapter.currentList)
                if (device != null) {
                    list.add(ListItem(device, false))
                    listFindedDevice.add(ListItem(device, false))
                    binding.tvEmptySearch.visibility = View.GONE
                }
                discoveryAdapter.submitList(list.toList())
                try {
                    Log.d("MyLogBtM", "Device: ${device?.name}")
                } catch (e: SecurityException) {
                }
            } else if (intent?.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                getPairedDevices()
            } else if (intent?.action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
                binding.imBluetoothSearch.visibility = View.VISIBLE
                binding.pbSearch.visibility = View.GONE
                if (listFindedDevice.isEmpty()) {
                    binding.buttonEmptySearch.visibility = View.VISIBLE
                } else {
                    binding.buttonEmptySearchInBar.visibility = View.VISIBLE
                    listFindedDevice.clear()
                }
            }
        }
    }

    /**
     * Реєстрація IntentFilter для системного BroadcastReceiver.
     */
    private fun intentFilters(){
        val f1 = IntentFilter(BluetoothDevice.ACTION_FOUND)
        val f2 = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        val f3 = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        activity?.registerReceiver(bReceiver, f1)
        activity?.registerReceiver(bReceiver, f2)
        activity?.registerReceiver(bReceiver, f3)
    }
}