package com.example.exelioshop.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exelioshop.EditItem
import com.example.exelioshop.MainApp
import com.example.exelioshop.adapters.BDListAdapter
import com.example.exelioshop.adapters.BDListAdapterNew
import com.example.exelioshop.classes.Article
import com.example.exelioshop.classes.MainViewModel
import com.example.exelioshop.classes.VMData
import com.example.exelioshop.databinding.FragmentBDBinding
import com.example.exelioshop.db.MainDataBase
import com.example.exelioshop.entities.ArticleList


/**
 * =================================================================================================
 * BDFragment — Екран каталогу товарів Room з пошуком та редагуванням (режим конфігурації).
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * 1. Відображення списку всіх артикулів з локальної бази даних Room через BDListAdapterNew.
 * 2. Жива фільтрація каталогу за назвою або кодом у SearchView (через Filterable адаптера).
 * 3. Додавання нового товару (button5 -> EditItem) або редагування існуючого по кліку на картку.
 * 4. Передача відредагованого товару у ViewModel (articleNew) для подальшої синхронізації з РРО.
 * =================================================================================================
 */
class BDFragment : BaseFragment(), BDListAdapterNew.Listener {
    private val dataModel: VMData by activityViewModels()
    private val dataModelM: MainViewModel by activityViewModels {
        MainViewModel.MainViewModelFactory((context?.applicationContext as MainApp).database)
    }

    /** Лаунчер переходу до екрану створення/редагування товару EditItem */
    lateinit var editLauncher: ActivityResultLauncher<Intent>

    private lateinit var binding: FragmentBDBinding
    private val adapter = BDListAdapterNew(this@BDFragment)
    val articleList: MutableSet<Article.ArticleData> = mutableSetOf()

    override fun onClickNew() {
        TODO("Not yet implemented")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBDBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onEditResult()
        // Кнопка відкриття форми створення нового товару
        binding.button5.setOnClickListener { onClickNewItem() }

        // Налаштування списку товарів RecyclerView
        binding.rcView.layoutManager = LinearLayoutManager(activity?.applicationContext)
        binding.rcView.adapter = adapter

        // Налаштування живого пошуку у рядку SearchView
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d("MyLog", "$newText")
                adapter.filter.filter(newText)
                return true
            }
        })

        // Спостереження за базою даних Room: оновлення адаптера новими/зміненими товарами
        dataModelM.allList.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        dataModelM.articleNew.observe(activity as LifecycleOwner, {})
    }

    /**
     * Відкриття екрану EditItem для створення нового артикулу
     */
    private fun onClickNewItem() {
        editLauncher.launch(Intent(activity, EditItem::class.java))
    }

    /**
     * Реєстрація обробника повернення з екрану EditItem:
     * Передача отриманої моделі ArticleData у ViewModel для відправки до РРО
     */
    private fun onEditResult() {
        editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                val itemToKasa: Article.ArticleData =
                    it.data?.getSerializableExtra("articleToSend") as Article.ArticleData
                dataModelM.articleNew.value = itemToKasa
            }
        }
    }

    /**
     * Клік по картці товару в каталозі: відкриття EditItem з попереднім заповненням полів
     */
    override fun onClick(article: ArticleList) {
        val intent = Intent(activity, EditItem::class.java).apply {
            putExtra("to_edit", article)
        }
        editLauncher.launch(intent)
    }

    companion object {
        @JvmStatic
        fun newInstance() = BDFragment()

        const val NEW_NOTE_KEY = "new_note_key"
    }
}