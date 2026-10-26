package com.example.exellio20.presentation.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.exellio20.R
import com.example.exellio20.databinding.FragmentScannerConnectedBinding

/**
 * [ScannerConnectedFragment] — екран підключеного сканера товарів.
 * 
 * Компонує два незалежні субекранні модулі в єдиний робочий простір:
 * 1. [ScannerCodeFragment] (у `child_fragment_container`) — панель відображення та ручного/оптичного введення штрихкоду
 * 2. [ScannerSeconForBaseFragment] (у `child_fragment_container2`) — детальна картка розпізнаного товару та кнопки дій
 */
class ScannerConnectedFragment : Fragment() {
    private lateinit var binding: FragmentScannerConnectedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentScannerConnectedBinding.inflate(layoutInflater, container, false)

        // Верхній дочірній фрагмент: поле штрихкоду та швидкі дії
        val childFragment: Fragment = ScannerCodeFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.child_fragment_container, childFragment)
            .commit()

        // Нижній дочірній фрагмент: картка знайденого товару
        val childFragment2: Fragment = ScannerSeconForBaseFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.child_fragment_container2, childFragment2)
            .commit()

        return binding.root
    }

    companion object {
        /** Фабричний метод для інстанціювання фрагмента */
        @JvmStatic
        fun newInstance() = ScannerConnectedFragment()
    }
}