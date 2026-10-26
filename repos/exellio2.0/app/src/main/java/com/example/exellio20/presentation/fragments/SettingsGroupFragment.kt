package com.example.exellio20.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exellio20.databinding.FragmentSettingsGroupBinding
import com.example.exellio20.presentation.BluetoothDBViewModel
import com.example.exellio20.presentation.adapters.GroupAdapter
import com.example.exellio20.roomDB.entities.GroupItem
import dagger.hilt.android.AndroidEntryPoint

/**
 * [SettingsGroupFragment] — екран керування довідником товарних груп у системі РРО.
 * 
 * Дозволяє:
 * - Створювати та редагувати товарні групи: назва (`groupName`), номер групи в РРО (`groupValue`),
 *   ознака підакцизності (`groupExcise`).
 * - Переглядати список наявних груп через [GroupAdapter].
 * - Видаляти застарілі групи з Room БД.
 * - Прив'язувати прапорець акцизу до групи, що змушує систему автоматично запитувати
 *   сканування акцизної марки при додаванні товарів цієї групи в чек.
 */
@AndroidEntryPoint
class SettingsGroupFragment : BaseFragment(), GroupAdapter.Listener {

    private val vm: BluetoothDBViewModel by activityViewModels()
    private lateinit var binding: FragmentSettingsGroupBinding
    private lateinit var adapter: GroupAdapter

    override fun onClickNew() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsGroupBinding.inflate(inflater, container, false)

        initRcView()

        // Збереження або створення нової товарної групи
        binding.button2.setOnClickListener {
            Toast.makeText(activity, "${binding.checkBox.isChecked}", Toast.LENGTH_SHORT).show()

            if (binding.editTextName.text.toString() != "" &&
                binding.editTextValue.text.toString() != ""
            ) {
                vm.insertGroup(
                    GroupItem(
                        id = 0,
                        groupName = binding.editTextName.text.toString(),
                        groupValue = binding.editTextValue.text.toString(),
                        groupExcise = binding.checkBox.isChecked
                    )
                )
            }
        }

        observer()
        onBackPress()

        return binding.root
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

    /**
     * Ініціалізація списку [RecyclerView] та адаптера [GroupAdapter].
     */
    private fun initRcView() = with(binding) {
        rcView.layoutManager = LinearLayoutManager(activity)
        adapter = GroupAdapter(this@SettingsGroupFragment)
        rcView.adapter = adapter
    }

    /**
     * Спостереження за списком груп у БД з сортуванням за числовим номером групи.
     */
    private fun observer() {
        vm.allGroup.observe(viewLifecycleOwner) {
            adapter.submitList(it.sortedBy { group -> group.groupValue.toInt() })
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsGroupFragment()
    }

    /**
     * Видалення товарної групи з локальної бази даних.
     */
    override fun deleteItem(id: Int) {
        vm.deleteGroup(id)
    }

    /**
     * Заповнення полів форми даними обраної групи для редагування.
     */
    override fun setValue(groupItem: GroupItem) {
        binding.editTextName.setText(groupItem.groupName)
        binding.editTextValue.setText(groupItem.groupValue)
        binding.checkBox.isChecked = groupItem.groupExcise
    }
}