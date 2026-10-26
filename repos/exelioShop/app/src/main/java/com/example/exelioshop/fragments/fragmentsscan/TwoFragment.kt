package com.example.exelioshop.fragments.fragmentsscan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.exelioshop.fragments.BaseFragment


/**
 * =================================================================================================
 * TwoFragment — Комбінований спліт-екран швидкого продажу товарів касиром.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * Об'єднує на одному екрані два дочірні компоненти через childFragmentManager:
 * 1. child_fragment_container  -> ScannerFragmentScan (камера оптичного розпізнавання штрихкодів);
 * 2. child_fragment_container2 -> LogFragment (стрічка чека: назви товарів, ціни, накопичувальний підсумок).
 * =================================================================================================
 */
class TwoFragment : BaseFragment() {
    override fun onClickNew() {
        TODO("Not yet implemented")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * Створення розмітки та вбудовування двох дочірніх фрагментів у відповідні контейнери
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Контейнер 1: вікно камери зі сканером ZBar
        val childFragment: Fragment = ScannerFragmentScan()
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(com.example.exelioshop.R.id.child_fragment_container, childFragment).commit()

        // Контейнер 2: касовий чек з накопиченням суми та списком позицій
        val childFragment2: Fragment = LogFragment()
        val transaction2 = childFragmentManager.beginTransaction()
        transaction2.replace(com.example.exelioshop.R.id.child_fragment_container2, childFragment2).commit()

        return inflater.inflate(com.example.exelioshop.R.layout.fragment_two, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance() = TwoFragment()
    }
}