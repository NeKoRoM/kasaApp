package com.example.exellio20.presentation.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exellio20.databinding.FragmentScannerListBinding
import com.example.exellio20.domain.usecases.CodeOfArticleToCodeForKasaUseCase
import com.example.exellio20.presentation.BluetoothDBViewModel
import com.example.exellio20.presentation.adapters.ArticleAdapter
import com.example.exellio20.presentation.adapters.UserActionListener
import com.example.exellio20.roomDB.entities.ArticleItem
import com.example.exellio20.roomDB.entities.GroupItem
import dagger.hilt.android.AndroidEntryPoint

/**
 * [ScannerListFragment] — екран вибору товарів із каталогу під час оформлення чека або сканування.
 * 
 * Ключова фіскальна бізнес-логіка:
 * 1. Відображає товари, у яких ціна більша за 0 (`withoutPrice0Article`), з можливістю пошуку через [SearchView].
 * 2. При кліку на товар ([onItemClick]):
 *    - Перевіряє приналежність товару до підакцизної групи (`group.groupExcise`). Якщо товар підакцизний
 *      (наприклад, алкоголь чи тютюн), система автоматично активує камеру-сканер (`startScanFrag = true`),
 *      вимагаючи від касира обов'язкового сканування акцизної марки відповідно до законодавства України.
 *    - За допомогою [CodeOfArticleToCodeForKasaUseCase] генерує синтезований EAN-13 штрихкод товару
 *      з кількістю "1" і відправляє його на касу через [BluetoothDBViewModel.codeSend].
 */
@AndroidEntryPoint
class ScannerListFragment : Fragment(), UserActionListener {
    private lateinit var binding: FragmentScannerListBinding

    private val vm: BluetoothDBViewModel by activityViewModels()

    private lateinit var articleAdapter: ArticleAdapter
    private var groupData = mutableListOf<GroupItem>()

    // Юзкейс генерації штрихкоду товару для РРО
    private val barCode = CodeOfArticleToCodeForKasaUseCase()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentScannerListBinding.inflate(layoutInflater, container, false)
        initRcView()
        onBackPress()

        return binding.root
    }

    /**
     * Обробка натискання кнопки "Назад" — повернення до базового сканера.
     */
    fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    FragmentManager.setFragment(
                        ScannerBaseFragment.newInstance(),
                        activity as AppCompatActivity
                    )
                }
            }
        )
    }

    /**
     * Ініціалізація списку [RecyclerView], пошукового рядка [SearchView] та спостерігачів БД.
     */
    private fun initRcView() = with(binding) {
        rcView.layoutManager = LinearLayoutManager(activity)
        articleAdapter = ArticleAdapter(this@ScannerListFragment)
        rcView.adapter = articleAdapter

        binding.searchView.setOnClickListener {
            searchView.isIconified = false
        }

        // Фільтрація каталогу товарів у реальному часі за назвою або кодом PLU
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                articleAdapter.filter.filter(newText)
                return true
            }
        })

        // Спостереження за списком товарів з ненульовою ціною
        vm.withoutPrice0Article.observe(viewLifecycleOwner) {
            articleAdapter.submitList(it)
        }

        // Спостереження за товарними групами для перевірки ознаки акцизу
        vm.allGroup.observe(viewLifecycleOwner) {
            groupData.clear()
            groupData.addAll(it)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ScannerListFragment()
    }

    /**
     * Обробка вибору товару зі списку:
     * - Якщо товар належить до акцизної групи — активує оптичний сканер для сканування акцизної марки.
     * - Формує та відправляє код товару для реєстрації продажу на РРО.
     */
    override fun onItemClick(article: ArticleItem) {
        for (group in groupData) {
            if (group.groupValue == article.group && group.groupExcise) {
                // Вимога сканування акцизної марки згідно із законодавством
                vm.startScanFrag.value = true
            }
        }

        // Генерація штрихкоду та відправка у чергу команд РРО
        vm.codeSend(barCode.execute(article.id.toString(), "1", ""))
    }
}