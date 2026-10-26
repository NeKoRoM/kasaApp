package com.example.exelioshop.fragments

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import com.example.exelioshop.MainApp
import com.example.exelioshop.HeadingActivity
import com.example.exelioshop.classes.MainViewModel
import com.example.exelioshop.databinding.FragmentSettingsBinding
import java.io.Serializable
import java.util.*


/**
 * =================================================================================================
 * FragmentSettings — Екран службових фіскальних налаштувань та операцій касового апарата.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * 1. Формування фіскальних звітів РРО:
 *    - X-Звіт (кнопка buttonX -> команда "EX\t") — проміжний денний звіт без обнулення;
 *    - Z-Звіт (кнопка buttonZ -> команда "EZ\t") — підсумковий фіскальний звіт з обнуленням зміни.
 * 2. Синхронізація годинника реального часу (RTC) каси:
 *    - Запит часу РРО (buttonTimeRead -> команда ">");
 *    - Встановлення поточного часу смартфона (buttonTimeSet -> команда "={dd-MM-yy HH:mm:ss}\t").
 * 3. Конфігурація 8 рядків фіскального кліше (заголовка чека):
 *    - Зчитування існуючих рядків (buttonReadHeading -> команда "Header\t0\t\t");
 *    - Редагування та запис через HeadingActivity (buttonSetHeading).
 * =================================================================================================
 */
class FragmentSettings : BaseFragment() {

    private var headingL = mutableListOf<String>()
    private val dataModelM: MainViewModel by activityViewModels {
        MainViewModel.MainViewModelFactory((context?.applicationContext as MainApp).database)
    }
    lateinit var headingLauncher: ActivityResultLauncher<Intent>

    lateinit var binding: FragmentSettingsBinding
    override fun onClickNew() {
        TODO("Not yet implemented")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onEditResult()

        // Кнопка відкриття HeadingActivity для редагування рядків кліше
        binding.buttonSetHeading.setOnClickListener {
            val intent = Intent(activity, HeadingActivity::class.java).apply {
                putExtra("to_heading", headingL.toList() as Serializable)
            }
            headingLauncher.launch(intent)
            Log.d("MyLog", headingL.toString())
        }

        // Відправка команди друку денного X-звіту без обнулення
        binding.buttonX.setOnClickListener {
            dataModelM.settingToKasa.value = "EX\t"
        }

        // Відправка команди друку Z-звіту (фіскальне закриття зміни з накопиченням)
        binding.buttonZ.setOnClickListener {
            dataModelM.settingToKasa.value = "EZ\t"
        }

        // Синхронізація RTC годинника РРО з системним часом пристрою
        binding.buttonTimeSet.setOnClickListener {
            dataModelM.settingToKasa.value = "=" + getCurrentTime() + "\t"
        }

        // Запит поточного внутрішнього часу з фіскального модуля
        binding.buttonTimeRead.setOnClickListener {
            dataModelM.settingToKasa.value = ">"
            Log.d("MyLog", headingL.toString())
        }

        // Старт покрокового вивантаження 8 рядків кліше з пам'яті РРО
        binding.buttonReadHeading.setOnClickListener {
            dataModelM.settingToKasa.value = "Header\t0\t\t"
            binding.textView4.text = ""
        }

        // Відображення прочитаного часу на екрані
        dataModelM.timeToSmart.observe(activity as LifecycleOwner, {
            binding.textViewTime.text = it
        })

        // Оновлення тексту відображення зчитаних рядків кліше
        dataModelM.listHeading.observe(activity as LifecycleOwner, {
            headingL.clear()
            for (i in it) {
                binding.textView4.text = binding.textView4.text.toString() + i + "\n"
                headingL.add(i)
            }
            Log.d("MyLog", headingL.toString())
        })
    }

    /**
     * Отримання відредагованого списку рядків кліше з HeadingActivity та відправка у чергу запису РРО
     */
    private fun onEditResult() {
        headingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                val newHL = it.data?.getSerializableExtra("ListH") as List<String>
                dataModelM.listHeadingToSet.value = newHL
            }
        }
    }

    /**
     * Форматування поточного системного часу у формат Datecs РРО: "dd-MM-yy HH:mm:ss"
     */
    fun getCurrentTime(): String {
        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(currentDate)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            FragmentSettings().apply {
                arguments = Bundle().apply {
                }
            }
    }
}