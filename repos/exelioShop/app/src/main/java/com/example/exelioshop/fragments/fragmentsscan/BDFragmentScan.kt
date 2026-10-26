package com.example.exelioshop.fragments.fragmentsscan

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

import com.example.exelioshop.databinding.FragmentBDScanBinding
import com.example.exelioshop.db.MainDataBase
import com.example.exelioshop.entities.ArticleList
import com.example.exelioshop.fragments.BaseFragment
import java.util.*
import kotlin.collections.ArrayList


/**
 * =================================================================================================
 * BDFragmentScan — Каталог товарів для швидкого вибору в режимі касового продажу.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * 1. Відображає товари з бази Room з можливістю живого пошуку за назвою/кодом.
 * 2. Головна відмінність від конфігураційного BDFragment:
 *    При натисканні на товар (onClick) він НЕ відкривається для редагування, а відправляється
 *    у потік продажу: dataModelM.articleToScan.value = article, що автоматично транслює
 *    штрихкод товару у фіскальний реєстратор через Bluetooth.
 * =================================================================================================
 */
class BDFragmentScan : BaseFragment(), BDListAdapterNew.Listener {
    private val dataModel: VMData by activityViewModels()
    private val dataModelM: MainViewModel by activityViewModels {
        MainViewModel.MainViewModelFactory((context?.applicationContext as MainApp).database)
    }
    private lateinit var tempArticleList: ArrayList<ArticleList>
    private lateinit var articleListNew: List<ArticleList>

    lateinit var editLauncher: ActivityResultLauncher<Intent>

    private lateinit var binding: FragmentBDScanBinding
    private val adapter = BDListAdapterNew(this@BDFragmentScan)
    val articleList: MutableSet<Article.ArticleData> = mutableSetOf()

    override fun onClickNew() {
        TODO("Not yet implemented")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBDScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Налаштування списку товарів
        binding.rcView.layoutManager = LinearLayoutManager(activity?.applicationContext)
        binding.rcView.adapter = adapter

        // Жива фільтрація у рядку пошуку
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

        // Спостереження за всіма товарами в Room DB
        dataModelM.allList.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            articleListNew = it
        }

        dataModelM.articleNew.observe(activity as LifecycleOwner, {})
    }

    private fun onClickNewItem() {
        editLauncher.launch(Intent(activity, EditItem::class.java))
    }

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
     * Вибір товару для продажу: передача у потік articleToScan для надсилання штрихкоду до каси
     */
    override fun onClick(article: ArticleList) {
        dataModelM.articleToScan.value = article
    }

    companion object {
        @JvmStatic
        fun newInstance() = BDFragmentScan()

        const val NEW_NOTE_KEY = "new_note_key"
    }
}