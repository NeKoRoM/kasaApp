package com.example.exellio20.presentation

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.exellio20.bluetooth.SelectDevice
import com.example.exellio20.databinding.ActivityLogin2Binding
import com.example.exellio20.models.ListItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

// =========================================================================================
// Константи налаштувань SharedPreferences для авторизації та контролю сесії
// =========================================================================================

/** Назва сховища SharedPreferences для облікових даних користувача */
const val APP_LOGIN_PREFERENCES = "APP_LOGIN_PREFERENCES"
/** Ключ для збереження згенерованого Email (на основі заводського номера РРО) */
const val PREF_LOGIN_EMAIL = "PREF_LOGIN_EMAIL"
/** Ключ для збереження пароля (на основі MAC-адреси Bluetooth РРО) */
const val PREF_LOGIN_PASSWORD = "PREF_LOGIN_PASSWORD"

/** Назва сховища SharedPreferences для збереження дати/часу активності сесії */
const val APP_DEY_PREFERENCES = "APP_DEY_PREFERENCES"
/** Ключ часу наступного обов'язкового виходу (force logout) */
const val APP_DEY_VALUE = "APP_DEY_VALUE"

/**
 * [LoginActivity] — екран автентифікації та прив'язки фіскального термінала до хмарного бекенду Firebase.
 * 
 * Архітектурна особливість Exellio 2.0:
 * Авторизація прив'язана до фізичного фіскального реєстратора (РРО):
 * 1. Користувач обирає Bluetooth-пристрій (касу Datecs/Exellio).
 * 2. Застосунок підключається та запитує заводський номер каси командою `"00??IDnumber\t\t\t"`.
 * 3. Логін і пароль у Firebase генеруються автоматично на основі комбінації MAC-адреси
 *    та серійного номера апарата (наприклад, `id.mac@mcs.ua`).
 * 4. Це гарантує, що касир або планшет працює виключно зі своєю зареєстрованою фіскальною касою.
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogin2Binding
    private lateinit var auth: FirebaseAuth

    // SharedPreferences для збереження сесії, облікових даних та MAC-адреси
    private lateinit var preferencesDay: SharedPreferences
    private lateinit var preferencesLogin: SharedPreferences
    private lateinit var preferencesMac: SharedPreferences

    // Лаунчери для дозволів та результатів вибору Bluetooth пристрою
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var actListLauncher: ActivityResultLauncher<Intent>

    private var registerPass = ""
    private var registerMail = ""

    // Спільна ViewModel зв'язку з касою через Bluetooth та БД
    private val vm: BluetoothDBViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityLogin2Binding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        // 1. Ініціалізація та перевірка системних дозволів (Bluetooth, геолокація, камера)
        permissionInit()
        // 2. Ініціалізація SharedPreferences
        initSharedPreferences()
        // 3. Перевірка додаткових параметрів запуску (перереєстрація на нову касу)
        getExtr()

        // 4. Реєстрація слухача результату вибору Bluetooth пристрою зі списку
        onBtListResult()
        // 5. Спостереження за отриманням MAC та ID каси для генерації акаунта Firebase
        getDataForLoginObserve()
        // 6. Перевірка тривалості сесії (автоматичний логаут при завершенні терміну)
        timeLogOut()
        // 7. Спостереження за помилками зв'язку (каса не в режимі програмування)
        observers()

        // Кнопка вибору Bluetooth-каси
        binding.button.setOnClickListener {
            actListLauncher.launch(Intent(this@LoginActivity, SelectDevice::class.java))
        }

        // Кнопка входу за збереженими раніше обліковими даними з SharedPreferences
        binding.buttonLogInSP.setOnClickListener {
            if (preferencesLogin.getString(PREF_LOGIN_EMAIL, "").toString() != "" &&
                preferencesLogin.getString(PREF_LOGIN_PASSWORD, "").toString() != ""
            ) {
                Log.d(
                    "MyLogSh",
                    preferencesLogin.getString(PREF_LOGIN_EMAIL, "").toString() + ";" +
                            preferencesLogin.getString(PREF_LOGIN_PASSWORD, "").toString()
                )

                signIn(
                    "${preferencesLogin.getString(PREF_LOGIN_EMAIL, "").toString()}.${preferencesLogin.getString(PREF_LOGIN_PASSWORD, "").toString()}",
                    preferencesLogin.getString(PREF_LOGIN_PASSWORD, "").toString()
                )
            }
        }

        setContentView(binding.root)
    }

    /**
     * Перевіряє, чи не надійшов запит переприв'язки на нову касу з MainActivity.
     */
    private fun getExtr() {
        val strState = intent.getSerializableExtra("to_new_kasa") as? String
        if (strState == "to_new_kasa") {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("не робочий акаунт")
            builder.setMessage("ви перейшли на нову касу, зареєструйтесь заново")
            builder.setPositiveButton("зареєструватись") { dialog, _ ->
                actListLauncher.launch(Intent(this@LoginActivity, SelectDevice::class.java))
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.show()
        }
    }

    /**
     * Контроль тривалості сесії роботи: примусовий розрив Firebase-сесії кожні X хвилин/днів
     * для гарантування безпеки фіскальних транзакцій.
     */
    private fun timeLogOut() {
        val today = Calendar.getInstance()
        val todayPlus = Calendar.getInstance()
        todayPlus.add(Calendar.MINUTE, 1)

        val savedDay = Calendar.getInstance().apply {
            timeInMillis = preferencesDay.getLong(APP_DEY_VALUE, 0)
        }
        if (today.timeInMillis >= savedDay.timeInMillis) {
            preferencesDay.edit().putLong(APP_DEY_VALUE, todayPlus.timeInMillis).apply()
            Firebase.auth.signOut()
        }
    }

    /**
     * Ініціалізація екземплярів SharedPreferences.
     */
    private fun initSharedPreferences() {
        preferencesLogin = getSharedPreferences(APP_LOGIN_PREFERENCES, Context.MODE_PRIVATE)
        preferencesDay = getSharedPreferences(APP_DEY_PREFERENCES, Context.MODE_PRIVATE)
        preferencesMac = getSharedPreferences(APP_BLUETOOTH_PREFERENCES, Context.MODE_PRIVATE)
    }

    /**
     * Налаштування спостереження за LiveData для автоматичної побудови хмарного акаунта:
     * - Після вибору каси та отримання її MAC надсилається запит ідентифікатора РРО `"00??IDnumber\t\t\t"`.
     * - Після отримання заводського номера каси формуються облікові дані та створюється акаунт у Firebase.
     * - Після успішної реєстрації надсилається підтверджувальний біп на касу `"0053"`.
     */
    private fun getDataForLoginObserve() {
        vm.btMac.observe(this) {
            registerPass = it
            // Запит ідентифікаційного заводського номера РРО
            vm.sendCommand("00??IDnumber\t\t\t")
            preferencesMac.edit().putString(PREF_CONST_MAC, it).apply()
            preferencesLogin.edit().putString(PREF_LOGIN_PASSWORD, it).apply()
        }

        vm.idKasa.observe(this) {
            // Формуємо детермінований пароль з очищеної MAC-адреси + домен
            preferencesLogin.edit().putString(
                PREF_LOGIN_PASSWORD,
                "${preferencesLogin.getString(PREF_LOGIN_PASSWORD, "").toString().lowercase().replace(":", "")}@mcs.ua"
            ).apply()
            // Формуємо логін із заводського номера каси
            preferencesLogin.edit().putString(PREF_LOGIN_EMAIL, it.lowercase().replace("000", "")).apply()

            // Створюємо або авторизуємо обліковий запис у Firebase
            createAccount(
                "${preferencesLogin.getString(PREF_LOGIN_EMAIL, "").toString()}.${preferencesLogin.getString(PREF_LOGIN_PASSWORD, "").toString()}",
                preferencesLogin.getString(PREF_LOGIN_PASSWORD, "").toString()
            )
            // Сигналізуємо касиру звуковим сигналом на РРО
            vm.sendCommand("0053")
        }
    }

    /**
     * Спостереження за подіями помилок або відсутності відповіді від РРО.
     */
    private fun observers() {
        vm.btNotAnswer.observe(this) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Нема відповіді")
            builder.setMessage("каса не в режимі програмування перейдіть в режим програмування")
            builder.setPositiveButton("ДОБРЕ") { dialog, _ ->
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.show()
        }
    }

    override fun onStart() {
        super.onStart()
        // Перевіряємо наявність активної сесії користувача у Firebase
        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUI(currentUser)
        } else {
            binding.buttonLogInSP.visibility = View.VISIBLE
        }
    }

    /**
     * Обробка вибору каси у списку діалогового вікна `SelectDevice`.
     */
    private fun onBtListResult() {
        actListLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val listItem = it.data?.getSerializableExtra(SelectDevice.DEVICE_KEY) as ListItem
                    // Запуск Bluetooth-з'єднання з обраним РРО
                    vm.btConnect(listItem.mac)
                }
            }
    }

    /**
     * Створення нового користувача у Firebase Authentication.
     * При невдачі (наприклад, якщо акаунт вже існує) виконується спроба звичайного входу [signIn].
     */
    private fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    vm.btConnect(preferencesMac.getString(PREF_CONST_MAC, "").toString())
                    updateUI(user)
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    signIn(email, password)
                    updateUI(null)
                }
            }
    }

    /**
     * Авторизація існуючого користувача у Firebase Authentication.
     */
    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    updateUI(null)
                }
            }
    }

    /**
     * Перехід до головного робочого вікна касира [MainActivity] після успішної автентифікації.
     */
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
        }
    }

    // =========================================================================================
    // Обробка системних дозволів Android (Bluetooth, Camera, Location)
    // =========================================================================================

    /**
     * Ініціалізація та перевірка необхідних дозволів рантайму.
     */
    private fun permissionInit() {
        if (!checkPermission(context = this)) {
            registerPermissionListener()
            launchBtPermission()
        }
    }

    /**
     * Запит дозволів відповідно до версії Android (Android 12+ API 31 вимагає BLUETOOTH_CONNECT).
     */
    private fun launchBtPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA
                )
            )
        }
    }

    /**
     * Реєстрація слухача зворотного виклику для запиту дозволів.
     */
    private fun registerPermissionListener() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            // Результат запиту дозволів
        }
    }

    /**
     * Перевірка статусу надання всіх критичних дозволів.
     */
    fun checkPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private const val TAG = "EmailPassword"
    }
}