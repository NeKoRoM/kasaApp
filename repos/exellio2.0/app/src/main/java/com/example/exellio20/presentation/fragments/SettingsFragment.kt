package com.example.exellio20.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import com.example.exellio20.databinding.FragmentSettingsBinding
import com.example.exellio20.presentation.BluetoothDBViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * [SettingsFragment] — головний хаб (меню вибору) розділів налаштувань фіскального комплексу.
 * 
 * Надає навігацію до 4 спеціалізованих екранів конфігурації:
 * 1. [SettingsKasaFragment] — апаратні налаштування каси (податкові ставки ПДВ/акцизу, службові реквізити)
 * 2. [SettingsEditItemFragment] — налаштування видимості полів картки товару (спрощений/повний інтерфейс)
 * 3. [SettingsGroupFragment] — довідник товарних та фіскальних груп
 * 4. [SettingsUktzFragment] — довідник кодів УКТЗЕД для підакцизних товарів
 */
@AndroidEntryPoint
class SettingsFragment : BaseFragment() {
    lateinit var binding: FragmentSettingsBinding
    private val vm: BluetoothDBViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(layoutInflater, container, false)

        // Навігація до налаштувань параметрів та ПДВ каси
        binding.kasaSettingsButton.setOnClickListener {
            FragmentManager.setFragment(SettingsKasaFragment(), this.activity as AppCompatActivity)
        }

        // Навігація до налаштувань відображення полів форми товару
        binding.editItemSetButton.setOnClickListener {
            FragmentManager.setFragment(
                SettingsEditItemFragment(),
                this.activity as AppCompatActivity
            )
        }

        // Навігація до довідника товарних груп
        binding.pdvButton.setOnClickListener {
            FragmentManager.setFragment(
                SettingsGroupFragment(),
                this.activity as AppCompatActivity
            )
        }

        // Навігація до довідника кодів УКТЗЕД
        binding.uktzButton.setOnClickListener {
            FragmentManager.setFragment(
                SettingsUktzFragment(),
                this.activity as AppCompatActivity
            )
        }

        onBackPress()

        return binding.root
    }

    /**
     * Повернення до головного екрана касира (сканера) при натисканні "Назад".
     */
    fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FragmentManager.setFragment(ScannerBaseFragment.newInstance(), activity as AppCompatActivity)
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onClickNew() {
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}