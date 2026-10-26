package com.example.exellio20.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.exellio20.databinding.FragmentExampleBinding
import com.example.exellio20.presentation.BluetoothDBViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * [ExampleFragment] — шаблонний (еталонний) фрагмент для швидкого створення нових екранів у проекті Exellio 2.0.
 * 
 * Демонструє стандартну інтеграцію:
 * - Наслідування від [BaseFragment] з реалізацією обов'язкового контракту [onClickNew]
 * - Впровадження залежностей через Hilt `@AndroidEntryPoint`
 * - Підключення спільної [BluetoothDBViewModel]
 * - Роботу з ViewBinding `FragmentExampleBinding`
 * - Фабричний метод створення [newInstance]
 */
@AndroidEntryPoint
class ExampleFragment : BaseFragment() {

    private val vm: BluetoothDBViewModel by viewModels()

    lateinit var binding: FragmentExampleBinding

    override fun onClickNew() {
        // Заглушка обов'язкової дії для створення нової сутності
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExampleBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    companion object {
        /**
         * Фабричний метод для створення нового екземпляра фрагмента.
         */
        @JvmStatic
        fun newInstance() = ExampleFragment()
    }
}
