package com.nekorom.bluetoothmodule

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nekorom.bluetoothmodule.databinding.FragmentMainBinding
import com.nekorom.bt.BluetoothConstants
import com.nekorom.bt.bluetooth.BluetoothController

/**
 * Головний фрагмент демонстраційного та тестового застосунку роботи з електронними вагами.
 * Шлях у системі: BluetoothModule/app/src/main/java/com/nekorom/bluetoothmodule/MainFragment.kt
 *
 * Архітектурна роль:
 * 1. Тестування протоколу зв'язку з торговими електронними вагами через Bluetooth Classic (SPP).
 * 2. Відправка команди тарування (Tare): рядок "T\r\n" ("T\u000D\u000A").
 * 3. Безперервний парсинг потоку даних ваги за допомогою регулярного виразу Regex("-?\\d+\\.?\\d*"):
 *    - Вилучає десяткове число ваги (з урахуванням знака мінус та дробової частини).
 *    - Застосовує бізнес-правило фасування залежно від маси порції (наприклад, поріг 100 г для інгредієнтів).
 * 4. Візуальна індикація стану підключення (зелений/червоний статус).
 */
class MainFragment : Fragment(), BluetoothController.Listener {

    /** Системний адаптер Bluetooth */
    private lateinit var btAdapter: BluetoothAdapter

    /** Контролер зв'язку */
    lateinit var bluetoothController: BluetoothController

    /** ViewBinding макета fragment_main */
    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBtAdapter()

        // Зчитування збереженої MAC-адреси ваг із SharedPreferences
        val pref = activity?.getSharedPreferences(BluetoothConstants.PREFERENCES, Context.MODE_PRIVATE)
        val mac = pref?.getString(BluetoothConstants.MAC, "")

        bluetoothController = BluetoothController(btAdapter)

        // Перехід до списку виявлених та спарених пристроїв
        binding.bList.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_deviceListFragment)
        }

        // Кнопка підключення до ваг за збереженою MAC-адресою
        binding.bConnect.setOnClickListener {
            bluetoothController.connect(mac ?: "", this)
        }

        // Кнопка тарування ваг: команда "T\r\n"
        binding.sendSome.setOnClickListener {
            // Приклад команди Datecs для РРО:
            // bluetoothController.sendMessage("\u00010035A00??IDnumber\t\t\t\u0005051=\u0003")

            // Команда тарування ваг (Tare):
            bluetoothController.sendMessage("T\u000D\u000A")
        }
    }

    /**
     * Отримання системного сервісу BluetoothManager.
     */
    private fun initBtAdapter() {
        val bManager = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = bManager.adapter
    }

    /**
     * Зворотний виклик при отриманні повідомлень з Bluetooth сокета ваг або зміни стану підключення.
     */
    override fun onReceive(message: String) {
        activity?.runOnUiThread {
            Log.d("btMess", message)
            when (message) {
                BluetoothController.BLUETOOTH_CONNECTED -> {
                    // З'єднання встановлено: фарбуємо кнопку в зелений колір
                    binding.bConnect.backgroundTintList = AppCompatResources
                        .getColorStateList(requireContext(), R.color.green)
                    binding.tvStatus.text = message
                }
                BluetoothController.BLUETOOTH_NOT_CONNECTED -> {
                    // З'єднання втрачено: фарбуємо кнопку в червоний колір
                    binding.bConnect.backgroundTintList = AppCompatResources
                        .getColorStateList(requireContext(), R.color.red)
                    binding.tvStatus.text = message
                }
                else -> {
                    // Отримано потік даних від вагового контролера
                    binding.tvStatus.text = message.substring(3, message.length)
                    Log.d("btMess", message)

                    // Вилучення числових значень ваги через регулярний вираз
                    val input = message
                    val regex = Regex("-?\\d+\\.?\\d*") // Регулярний вираз для знаходження десяткових чисел

                    val matches = regex.findAll(input)
                    val digitsOnly = StringBuilder()

                    for (match in matches) {
                        digitsOnly.append(match.value)
                    }

                    // Логіка класифікації товару залежно від ваги
                    if (digitsOnly.toString().toDouble() > 100) {
                        binding.tvNext.text = "amoniak"
                    } else {
                        binding.tvNext.text = "mąka"
                    }
                }
            }
        }
    }
}