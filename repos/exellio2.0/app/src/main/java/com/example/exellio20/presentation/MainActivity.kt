package com.example.exellio20.presentation

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.exellio20.R
import com.example.exellio20.bluetooth.SelectDevice
import com.example.exellio20.databinding.ActivityMainBinding
import com.example.exellio20.models.ListItem
import com.example.exellio20.presentation.fragments.ArticleListFragment
import com.example.exellio20.presentation.fragments.FragmentManager
import com.example.exellio20.presentation.fragments.ScannerBaseFragment
import com.example.exellio20.presentation.fragments.SettingsFragment
import com.example.exellio20.presentation.fragments.SettingsKasaFragment
import dagger.hilt.android.AndroidEntryPoint

const val APP_BLUETOOTH_PREFERENCES = "APP_BLUETOOTH_PREFERENCES"
const val PREF_CONST_MAC = "PREF_CONST_MAC"

/**
 * ====================================================================================================
 * ГОЛОВНА АКТИВНІСТЬ ДОДАТКУ EXELLIO 2.0 (MAIN ACTIVITY)
 * ====================================================================================================
 *
 * Хостова активність інтерфейсу Android для роботи з касовими апаратами Exellio/Datecs:
 * 1. Керує бічним меню [DrawerLayout] з навігацією між сканером, списком товарів та налаштуваннями.
 * 2. Утримує активний екран увімкненим ([WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON])
 *    та фіксує портретну орієнтацію для POS-терміналу.
 * 3. Зберігає та автоматично відновлює підключення до збереженої MAC-адреси РРО через [SharedPreferences].
 * 4. Обробляє діалоги помилок підключення та переходу каси в режим програмування.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var actListLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var preferences: SharedPreferences
    private lateinit var logPreferences: SharedPreferences

    val status = false
    private val vm: BluetoothDBViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initSharedPreferences()
        onBtListResult()
        menuInit()

        // Встановлення початкового фрагмента сканування
        FragmentManager.setFragment(ScannerBaseFragment.newInstance(), this@MainActivity)

        observers()

        // Запобігання вимкненню екрана під час роботи касира
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Блокування портретної орієнтації
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
    }

    /**
     * Підписка на події від [BluetoothDBViewModel]: сповіщення про помилки, запити вибору пристрою.
     */
    private fun observers() {
        vm.btProblemMSG.observe(this) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Нема відповіді")
            builder.setMessage("виникли проблеми зі зєднанням")
            builder.setPositiveButton("bluetooth") { dialog, _ ->
                vm.startBtActivity.value = "No_Answer"
                dialog.dismiss()
            }
            builder.setNegativeButton("ЗАКРИТИ") { dialog, _ ->
                dialog.dismiss()
            }
            val dialog = builder.create()
            if (FragmentManager.currentFrag !is ArticleListFragment) {
                dialog.show()
            }
        }

        vm.startBtActivity.observe(this) {
            preferences.edit().putString(PREF_CONST_MAC, "").apply()
            actListLauncher.launch(Intent(this, SelectDevice::class.java))
        }

        vm.btProblemMSG.observe(this) {
            Log.i("MyLogBtNoConn", it)
        }

        // Попередження, коли РРО не перебуває в режимі програмування для вивантаження бази
        vm.btNotAnswer.observe(this) {
            val builder = AlertDialog.Builder(this@MainActivity)
            if (it == "No_Answer") {
                builder.setTitle("Нема відповіді")
                builder.setMessage("каса не в режимі програмування перейдіть в режим програмування")
                builder.setPositiveButton("ДОБРЕ") { dialog, _ ->
                    dialog.dismiss()
                }
                val dialog = builder.create()
                if (FragmentManager.currentFrag is SettingsKasaFragment) {
                    dialog.show()
                }
            }
        }
    }

    /**
     * Ініціалізація збереження MAC-адреси та автоматичне перепідключення при старті.
     */
    private fun initSharedPreferences() {
        logPreferences = getSharedPreferences(APP_LOGIN_PREFERENCES, Context.MODE_PRIVATE)
        preferences = getSharedPreferences(APP_BLUETOOTH_PREFERENCES, Context.MODE_PRIVATE)

        val savedMac = preferences.getString(PREF_CONST_MAC, "").toString()
        if (savedMac.isNotEmpty()) {
            vm.btConnect(savedMac)
        }

        vm.btMac.observe(this) {
            preferences.edit().putString(PREF_CONST_MAC, it).apply()
        }
    }

    /**
     * Налаштування бічної панелі навігації (Navigation Drawer).
     */
    private fun menuInit() = with(binding) {
        toggle = ActionBarDrawerToggle(this@MainActivity, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.firstMenuItem -> {
                    FragmentManager.setFragment(ScannerBaseFragment.newInstance(), this@MainActivity)
                    Toast.makeText(this@MainActivity, "list", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.secondMenuItem -> {
                    FragmentManager.setFragment(ArticleListFragment.newInstance(), this@MainActivity)
                    Toast.makeText(this@MainActivity, "list", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.thirdMenuItem -> {
                    FragmentManager.setFragment(SettingsFragment.newInstance(), this@MainActivity)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    Toast.makeText(this@MainActivity, "third", Toast.LENGTH_SHORT).show()
                }
                R.id.bluetoothCon -> {
                    preferences.edit().putString(PREF_CONST_MAC, "").apply()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    actListLauncher.launch(Intent(this@MainActivity, SelectDevice::class.java))
                }
            }
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Обробка результату повернення з вікна вибору Bluetooth-пристрою [SelectDevice].
     */
    private fun onBtListResult() {
        actListLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val listItem = it.data?.getSerializableExtra(SelectDevice.DEVICE_KEY) as ListItem
                    vm.btConnect(listItem.mac)
                }
            }
    }
}