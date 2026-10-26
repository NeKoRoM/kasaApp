package com.example.exellio20.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import com.example.exellio20.databinding.FragmentSettingsKasaBinding
import com.example.exellio20.presentation.BluetoothDBViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * [SettingsKasaFragment] — екран прямого керування фіскальними функціями РРО Datecs/Exellio.
 * 
 * Виконує базові апаратні команди протоколу Datecs:
 * - Друк X-звіту (денний звіт без обнулення регістрів): `"0045X\t"` (команда 0x45 / 69).
 * - Друк Z-звіту (фіскальний денний звіт із записом у фіскальну пам'ять та обнуленням оперативних регістрів): `"0045Z\t"`.
 * - Зчитування поточного часу та дати з годинника реального часу (RTC) апарата: `"003>"`.
 * - Синхронізація годинника РРО із системним часом Android-пристрою у форматі `dd-MM-yy HH:mm:ss`: `"003=$formattedDateTime\t"`.
 * - Подача діагностичного звукового сигналу (біп): `"0053"`.
 */
@AndroidEntryPoint
class SettingsKasaFragment : BaseFragment() {

    private val vm: BluetoothDBViewModel by activityViewModels()
    lateinit var binding: FragmentSettingsKasaBinding

    override fun onClickNew() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsKasaBinding.inflate(layoutInflater, container, false)

        // Друк проміжного фіскального X-звіту (без обнулення)
        binding.xZvitButton.setOnClickListener {
            vm.sendCommand("0045X\t")
        }

        // Друк підсумкового денного Z-звіту з обнуленням зміни
        binding.zZvitButton.setOnClickListener {
            vm.sendCommand("0045Z\t")
        }

        // Запит поточної дати та часу з РРО
        binding.checkTimeButton.setOnClickListener {
            vm.sendCommand("003>")
        }

        // Синхронізація годинника РРО з поточним часом планшета/телефона
        binding.setTimeButton.setOnClickListener {
            val currentDate = Date()
            val dateFormat = SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.getDefault())
            val formattedDateTime = dateFormat.format(currentDate)

            // Відправка команди встановлення дати та часу
            vm.sendCommand("003=$formattedDateTime\t")
        }

        // Відображення прочитаної дати/часу з каси
        vm.dateKasa.observe(viewLifecycleOwner) { kasaDate ->
            binding.checkDataTv.text = kasaDate
        }

        // Діагностичний звуковий сигнал РРО
        binding.pdvButton.setOnClickListener {
            vm.sendCommand("0053")
        }

        onBackPress()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * Повернення до головного меню налаштувань.
     */
    fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FragmentManager.setFragment(SettingsFragment.newInstance(), activity as AppCompatActivity)
            }
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsKasaFragment()
    }
}
