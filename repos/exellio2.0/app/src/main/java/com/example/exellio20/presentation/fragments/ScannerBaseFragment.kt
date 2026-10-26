package com.example.exellio20.presentation.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import com.example.exellio20.R
import com.example.exellio20.presentation.BluetoothDBViewModel

/**
 * [ScannerBaseFragment] — базовий контейнерний фрагмент для режиму сканування та перевірки товарів.
 * 
 * Керує відображенням дочірніх фрагментів у контейнері `R.id.child_fragment_container`:
 * - [ScannerConnectedFragment] — екран підключеного сканера та роботи зі штрихкодами
 * - [ScannerListFragment] — екран списку/журналу відсканованих позицій
 * 
 * Перемикання здійснюється як через верхнє меню (`scan_menu`), так і реактивно через події
 * у [BluetoothDBViewModel] (`startScanFrag`, `startListFrag`).
 */
class ScannerBaseFragment : BaseFragment() {

    // Спільна ViewModel з рівня Activity
    private val vm: BluetoothDBViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Скидаємо прапорці переходу між вкладками
        vm.startListFrag.value = false
        vm.startScanFrag.value = false

        // За замовчуванням відображаємо екран зв'язку зі сканером
        val childFragment: Fragment = ScannerConnectedFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.child_fragment_container, childFragment)
            .commit()

        // Підключаємо спостерігачі для програмного перемикання екранів
        replace()

        // Обробка системної кнопки "Назад" — згортання та закриття застосунку
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    activity?.moveTaskToBack(true)
                    activity?.finish()
                    System.exit(0)
                }
            }
        )

        return inflater.inflate(R.layout.fragment_scanner_base, container, false)
    }

    override fun onClickNew() {
    }

    lateinit var menuGlobal: Menu
    lateinit var menuinflater: MenuInflater

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menuinflater = inflater
        menuGlobal = menu
        inflater.inflate(R.menu.scan_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)

        val tab1 = menu.findItem(R.id.tab1)
        val tab2 = menu.findItem(R.id.tab2)

        menu.findItem(R.id.tab1)?.isChecked = true
        tab1.isVisible = false
        tab2.isVisible = true

        // Вкладка 1: перехід до режиму інтерактивного сканера
        menu.findItem(R.id.tab1)?.setOnMenuItemClickListener { menuItem ->
            val childFragment: Fragment = ScannerConnectedFragment()
            childFragmentManager.beginTransaction()
                .replace(R.id.child_fragment_container, childFragment)
                .commit()
            menuItem.isChecked = true
            tab1.isVisible = false
            tab2.isVisible = true
            vm.startListFrag.value = false
            vm.startScanFrag.value = false
            true
        }

        // Вкладка 2: перехід до журналу відсканованих товарів
        menu.findItem(R.id.tab2)?.setOnMenuItemClickListener { menuItem ->
            val childFragment: Fragment = ScannerListFragment()
            childFragmentManager.beginTransaction()
                .replace(R.id.child_fragment_container, childFragment)
                .commit()
            menuItem.isChecked = true
            tab2.isVisible = false
            tab1.isVisible = true
            vm.startListFrag.value = false
            vm.startScanFrag.value = false
            true
        }
    }




    companion object {

        @JvmStatic
        fun newInstance() =
            ScannerBaseFragment()
    }
    /**
     * Спостереження за реактивними запитами перемикання дочірніх фрагментів з ViewModel:
     * - `startScanFrag`: відображає [ScannerConnectedFragment]
     * - `startListFrag`: відображає [ScannerListFragment]
     */
    fun replace() {
        vm.startScanFrag.observe(viewLifecycleOwner) {
            if (it) {
                val childFragment: Fragment = ScannerConnectedFragment()
                childFragmentManager.beginTransaction()
                    .replace(R.id.child_fragment_container, childFragment)
                    .commit()
            }
        }

        vm.startListFrag.observe(viewLifecycleOwner) {
            if (it) {
                val childFragment: Fragment = ScannerListFragment()
                childFragmentManager.beginTransaction()
                    .replace(R.id.child_fragment_container, childFragment)
                    .commit()
                vm.startListFrag.value = false
            }
        }
    }
}
