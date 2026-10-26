package com.example.exellio20.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exellio20.databinding.FragmentSettingsUktzBinding
import com.example.exellio20.presentation.BluetoothDBViewModel
import com.example.exellio20.presentation.adapters.UktzAdapter
import com.example.exellio20.roomDB.entities.UktzItem
import dagger.hilt.android.AndroidEntryPoint

/**
 * [SettingsUktzFragment] — екран керування довідником класифікатора УКТЗЕД (Українська класифікація товарів зовнішньоекономічної діяльності).
 * 
 * Згідно із законодавством України, друк 10-значного коду УКТЗЕД у фіскальному чеку є обов'язковим
 * для всіх підакцизних товарів (алкогольні напої, тютюнові вироби, пальне тощо).
 * 
 * Фрагмент забезпечує:
 * - Додавання нових кодів УКТЗЕД з описовою назвою (із автоматичною заміною дублікатів за значенням коду)
 * - Відображення списку кодів у [UktzAdapter]
 * - Редагування та видалення кодів
 */
@AndroidEntryPoint
class SettingsUktzFragment : BaseFragment(), UktzAdapter.Listener {

    private val vm: BluetoothDBViewModel by activityViewModels()
    private lateinit var binding: FragmentSettingsUktzBinding
    private lateinit var adapter: UktzAdapter

    override fun onClickNew() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsUktzBinding.inflate(layoutInflater, container, false)

        // Кнопка збереження/оновлення коду УКТЗЕД
        binding.button2.setOnClickListener {
            if (binding.editTextName.text.toString() != "" &&
                binding.editTextValue.text.toString() != ""
            ) {
                // Видаляємо попередній запис із таким самим числовим значенням коду, щоб уникнути колізій
                vm.deleteUktzFromVal(binding.editTextValue.text.toString())

                // Вставляємо новий або оновлений запис УКТЗЕД
                vm.insertUktz(
                    UktzItem(
                        id = 0,
                        pdvName = binding.editTextName.text.toString(),
                        pdvValue = binding.editTextValue.text.toString()
                    )
                )
            }
        }

        initRcView()
        observer()
        onBackPress()

        return binding.root
    }

    /**
     * Ініціалізація списку [RecyclerView] та адаптера [UktzAdapter].
     */
    private fun initRcView() = with(binding) {
        rcView.layoutManager = LinearLayoutManager(activity)
        adapter = UktzAdapter(this@SettingsUktzFragment)
        rcView.adapter = adapter
    }

    /**
     * Спостереження за списком кодів УКТЗЕД у БД з сортуванням за числовим кодом.
     */
    private fun observer() {
        vm.allUktz.observe(viewLifecycleOwner) {
            adapter.submitList(it.sortedBy { uktz -> uktz.pdvValue })
        }
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
        fun newInstance() = SettingsUktzFragment()
    }

    /**
     * Заповнення форми для редагування вибраного коду УКТЗЕД.
     */
    override fun setValue(uktz: UktzItem) {
        binding.editTextValue.setText(uktz.pdvValue)
        binding.editTextName.setText(uktz.pdvName)
    }

    /**
     * Видалення коду УКТЗЕД з бази даних за ID.
     */
    override fun deleteItem(id: Int) {
        vm.deleteUktz(id)
    }
}