package com.example.exelioshop.fragments.fragmentsscan

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.exelioshop.MainApp
import com.example.exelioshop.classes.MainViewModel
import com.example.exelioshop.databinding.FragmentLogBinding
import com.example.exelioshop.entities.ArticleList


/**
 * =================================================================================================
 * LogFragment — Екран оперативного касового чека та калькуляції суми продажу.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * 1. Слухає подію появи нового штрихкоду (barCode) від камери або Bluetooth-сканера.
 * 2. Виконує пошук товару у завантаженому кеші Room (articleL) за полями barCode1, barCode2, barCode3.
 * 3. При успішному збігу:
 *    - Додає назву та ціну товару у стрічку чека (textViewLog);
 *    - Розраховує накопичувальний підсумок (textViewSum);
 *    - Емітує подію articleToScan для надсилання сигналу реєстрації у фіскальний реєстратор.
 * 4. Забезпечує скидання поточного чека кнопкою buttonReset.
 * =================================================================================================
 */
class LogFragment : Fragment() {
    private lateinit var binding: FragmentLogBinding
    private val dataModelM: MainViewModel by activityViewModels {
        MainViewModel.MainViewModelFactory((context?.applicationContext as MainApp).database)
    }
    /** Локальний кеш товарів з бази даних Room для миттєвого пошуку */
    lateinit var articleL: List<ArticleList>
    /** Знайдений товар за відсканованим штрихкодом */
    var articleIs: ArticleList? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Оновлення кешу списку товарів з бази Room
        dataModelM.allList.observe(viewLifecycleOwner) {
            articleL = it
        }

        // Обробка кожного розпізнаного штрихкоду
        dataModelM.barCode.observe(viewLifecycleOwner) { bar ->
            for (a in articleL) {
                if (a.barCode1 == bar.toString() || a.barCode2 == bar.toString() || a.barCode3 == bar.toString()) {
                    articleIs = a
                }
            }

            // Якщо знайдено товар у локальній базі даних
            if (articleIs != null) {
                // Додавання назви та ціни до стрічки чека
                binding.textViewLog.text = articleIs!!.name + ": " + articleIs!!.price + "\n" + binding.textViewLog.text.toString()
                // Накопичувальний підсумок суми чека
                binding.textViewSum.text = (binding.textViewSum.text.toString().toFloat()
                    .plus(articleIs!!.price.toFloat())).toString()
                // Передача товару в драйвер РРО для фіскального продажу
                dataModelM.articleToScan.value = articleIs
            }
        }

        // Кнопка анулювання/скидання підсумку поточного чека
        binding.buttonReset.setOnClickListener {
            binding.textViewSum.text = "0"
            binding.textViewLog.text = ""
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = LogFragment()
    }
}