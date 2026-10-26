package com.example.exelioshop

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exelioshop.adapters.BDListAdapter
import com.example.exelioshop.bluetooth.BtConnection
import com.example.exelioshop.classes.Article
import com.example.exelioshop.classes.VMData
import com.example.exelioshop.databinding.ActivityBdlistBinding

/**
 * =================================================================================================
 * BDList — Екран відображення повного каталогу товарів, вивантажених з каси через Datecs протокол.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * Приймає множину товарів Set<Article.ArticleData> через Intent ("item") після завершення
 * читання таблиці PLU з РРО і рендерить їх у списку RecyclerView за допомогою BDListAdapter.
 * =================================================================================================
 */
class BDList() : AppCompatActivity(), BDListAdapter.Listener {

    lateinit var btConnection: BtConnection
    private val dataModel: VMData by viewModels()
    lateinit var binding: ActivityBdlistBinding
    private val adapter = BDListAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBdlistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        binding.button1.setOnClickListener {
            val kr = "kR\t1\t"
        }
    }

    /**
     * Ініціалізація адаптера та наповнення списку товарами з вхідного Intent
     */
    private fun init() {
        val item = intent.getSerializableExtra("item") as Set<*>

        binding.apply {
            rcView.layoutManager = LinearLayoutManager(this@BDList)
            rcView.adapter = adapter

            for (n in item)
                adapter.addList(n as Article.ArticleData)
        }
    }

    /**
     * Обробка вибору товару зі списку
     */
    override fun onClick(article: Article.ArticleData) {
        Toast.makeText(this, "ok${article.name}", Toast.LENGTH_SHORT).show()
    }
}