package com.example.exellio20.presentation.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.exellio20.databinding.FragmentSettingsEditItemBinding
import dagger.hilt.android.AndroidEntryPoint

// =========================================================================================
// Константи ключів SharedPreferences для налаштування форми товару
// =========================================================================================
const val APP_EDIT_ITEM_PREFERENCES = "APP_EDIT_ITEM_PREFERENCES"
const val APP_EDIT_ITEM_SECTION = "APP_EDIT_ITEM_SECTION"
const val APP_EDIT_ITEM_TYPE = "APP_EDIT_ITEM_TYPE"
const val APP_EDIT_ITEM_SALES_COUNT = "APP_EDIT_ITEM_SALES_COUNT"
const val APP_EDIT_ITEM_SALES_AMOUNT = "APP_EDIT_ITEM_SALES_AMOUNT"
const val APP_EDIT_ITEM_REMAINDER = "APP_EDIT_ITEM_REMAINDER"
const val APP_EDIT_ITEM_BAR2 = "APP_EDIT_ITEM_BAR2"
const val APP_EDIT_ITEM_BAR3 = "APP_EDIT_ITEM_BAR3"
const val APP_EDIT_ITEM_BAR4 = "APP_EDIT_ITEM_BAR4"
const val APP_EDIT_ITEM_UKTZ = "APP_EDIT_ITEM_UKTZ"
const val APP_EDIT_ITEM_GROUP = "APP_EDIT_ITEM_GROUP"
const val APP_EDIT_ITEM_PDV = "APP_EDIT_ITEM_PDV"

/**
 * [SettingsEditItemFragment] — екран налаштування видимості полів картки товару [EditItem].
 * 
 * Дозволяє адаптувати екран додавання/редагування товару під конкретний тип бізнесу:
 * - Приховувати або показувати поля залишку, секції/відділу, типу ціни
 * - Вмикати/вимикати додаткові поля штрихкодів (2, 3, 4)
 * - Керувати відображенням лічильників продажів (кількість, сума)
 * - Керувати полями ПДВ, товарної групи та коду УКТЗЕД
 * 
 * Усі налаштування зберігаються у SharedPreferences `APP_EDIT_ITEM_PREFERENCES`
 * і безпосередньо застосовуються в методі `EditItem.visibilityState()`.
 */
@AndroidEntryPoint
class SettingsEditItemFragment : BaseFragment() {
    private lateinit var binding: FragmentSettingsEditItemBinding
    private lateinit var preferencesEditItem: SharedPreferences

    override fun onClickNew() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        preferencesEditItem = this.requireActivity().getSharedPreferences(APP_EDIT_ITEM_PREFERENCES, Context.MODE_PRIVATE)

        binding = FragmentSettingsEditItemBinding.inflate(inflater, container, false)
        // Завантаження збереженого стану перемикачів
        setSwitches()
        // Прив'язка слухачів для збереження змін
        getSwitches()
        onBackPress()

        return binding.root
    }

    /**
     * Повернення до меню налаштувань.
     */
    fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FragmentManager.setFragment(SettingsFragment.newInstance(), activity as AppCompatActivity)
            }
        })
    }

    /**
     * Встановлення початкового стану перемикачів (Switch) на основі збережених прапорців у SharedPreferences.
     */
    private fun setSwitches() {
        binding.remainderSwitch.isChecked = preferencesEditItem.getBoolean(APP_EDIT_ITEM_REMAINDER, false)
        binding.sectionSwitch.isChecked = preferencesEditItem.getBoolean(APP_EDIT_ITEM_SECTION, false)
        binding.typeSwitch.isChecked = preferencesEditItem.getBoolean(APP_EDIT_ITEM_TYPE, false)
        binding.bar2.isChecked = preferencesEditItem.getBoolean(APP_EDIT_ITEM_BAR2, false)
        binding.bar3.isChecked = preferencesEditItem.getBoolean(APP_EDIT_ITEM_BAR3, false)
        binding.bar4.isChecked = preferencesEditItem.getBoolean(APP_EDIT_ITEM_BAR4, false)
        binding.salesCountSwitch.isChecked = preferencesEditItem.getBoolean(APP_EDIT_ITEM_SALES_COUNT, false)
        binding.salesAmountSwitch.isChecked = preferencesEditItem.getBoolean(APP_EDIT_ITEM_SALES_AMOUNT, false)
        binding.editUKTZED.isChecked = preferencesEditItem.getBoolean(APP_EDIT_ITEM_UKTZ, false)
        binding.editGroup.isChecked = preferencesEditItem.getBoolean(APP_EDIT_ITEM_GROUP, false)
        binding.spinnerPdv.isChecked = preferencesEditItem.getBoolean(APP_EDIT_ITEM_PDV, false)
    }

    /**
     * Слухачі зміни стану перемикачів із негайним записом у SharedPreferences.
     */
    private fun getSwitches() {
        binding.remainderSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferencesEditItem.edit().putBoolean(APP_EDIT_ITEM_REMAINDER, isChecked).apply()
        }
        binding.sectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferencesEditItem.edit().putBoolean(APP_EDIT_ITEM_SECTION, isChecked).apply()
        }
        binding.typeSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferencesEditItem.edit().putBoolean(APP_EDIT_ITEM_TYPE, isChecked).apply()
        }
        binding.bar2.setOnCheckedChangeListener { _, isChecked ->
            preferencesEditItem.edit().putBoolean(APP_EDIT_ITEM_BAR2, isChecked).apply()
        }
        binding.bar3.setOnCheckedChangeListener { _, isChecked ->
            preferencesEditItem.edit().putBoolean(APP_EDIT_ITEM_BAR3, isChecked).apply()
        }
        binding.bar4.setOnCheckedChangeListener { _, isChecked ->
            preferencesEditItem.edit().putBoolean(APP_EDIT_ITEM_BAR4, isChecked).apply()
        }
        binding.salesCountSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferencesEditItem.edit().putBoolean(APP_EDIT_ITEM_SALES_COUNT, isChecked).apply()
        }
        binding.salesAmountSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferencesEditItem.edit().putBoolean(APP_EDIT_ITEM_SALES_AMOUNT, isChecked).apply()
        }
        binding.editGroup.setOnCheckedChangeListener { _, isChecked ->
            preferencesEditItem.edit().putBoolean(APP_EDIT_ITEM_GROUP, isChecked).apply()
        }
        binding.editUKTZED.setOnCheckedChangeListener { _, isChecked ->
            preferencesEditItem.edit().putBoolean(APP_EDIT_ITEM_UKTZ, isChecked).apply()
        }
        binding.spinnerPdv.setOnCheckedChangeListener { _, isChecked ->
            preferencesEditItem.edit().putBoolean(APP_EDIT_ITEM_PDV, isChecked).apply()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsEditItemFragment()
    }
}