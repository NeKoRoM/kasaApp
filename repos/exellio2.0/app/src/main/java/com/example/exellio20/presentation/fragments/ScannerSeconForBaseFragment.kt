package com.example.exellio20.presentation.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.exellio20.databinding.FragmentScannerSeconForBaseBinding
import com.example.exellio20.presentation.BluetoothDBViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * [ScannerSeconForBaseFragment] — дочірній фрагмент ручного введення штрихкоду або PLU товару з клавіатури.
 * 
 * Доповнює роботу оптичного сканера камери [ScannerCodeFragment]:
 * якщо штрихкод пошкоджено або сканер не може його розпізнати, касир вводить цифровий код вручну
 * у поле `editTextText2` і натискає кнопку підтвердження для передачі у [BluetoothDBViewModel.codeSend].
 */
@AndroidEntryPoint
class ScannerSeconForBaseFragment : Fragment() {
    private lateinit var binding: FragmentScannerSeconForBaseBinding
    private val vm: BluetoothDBViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentScannerSeconForBaseBinding.inflate(layoutInflater, container, false)

        // Обробник ручного відправлення введеного коду товару/штрихкоду
        binding.button3.setOnClickListener {
            val enteredCode = binding.editTextText2.text.toString()
            vm.codeSend(enteredCode)

            if (vm.startScanFrag.value == true) {
                vm.startListFrag.value = true
                vm.startScanFrag.value = false
            }
        }

        return binding.root
    }

    companion object {
        /** Фабричний метод створення фрагмента */
        fun newInstance() = ScannerSeconForBaseFragment()
    }
}