package com.example.exelioshop.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exelioshop.R
import com.example.exelioshop.databinding.BdlistItemBinding
import com.example.exelioshop.entities.ArticleList

/**
 * =================================================================================================
 * BDListAdapterNew — Сучасний адаптер списку товарів з підтримкою DiffUtil та живої фільтрації.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * 1. Відображення товарів з локальної бази даних Room (сутність ArticleList).
 * 2. Реалізація інтерфейсу Filterable для миттєвого пошуку товарів за назвою або цифровим кодом ID.
 * 3. Використання ListAdapter + DiffUtil для плавної анімації та мінімізації навантаження на UI.
 * =================================================================================================
 */
class BDListAdapterNew(private val listener: Listener) :
    ListAdapter<ArticleList, BDListAdapterNew.ItemHolder>(ItemComparator()),
    Filterable {

    /** Оригінальний (повний) список товарів для відновлення після скидання пошуку */
    private var originalList: List<ArticleList> = currentList.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        holder.setData(getItem(position), listener)
    }

    /**
     * Перевизначений submitList для збереження еталонного невідфільтрованого списку
     */
    override fun submitList(list: List<ArticleList>?) {
        submitList(list, false)
    }

    /**
     * ViewHolder елемента товару бази даних
     */
    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = BdlistItemBinding.bind(view)

        /**
         * Прив'язка реквізитів товару: внутрішній ID, назва, ціна та обробка вибору
         */
        fun setData(article: ArticleList, listener: Listener) = with(binding) {
            id.text = article.id.toString()
            name.text = article.name
            price.text = article.price
            cardV.setOnClickListener {
                listener.onClick(article)
            }
        }

        companion object {
            /** Фабрика створення ViewHolder */
            fun create(parent: ViewGroup): ItemHolder {
                return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.bdlist_item, parent, false))
            }
        }
    }

    /**
     * Компаратор різниць DiffUtil для сутності ArticleList
     */
    class ItemComparator : DiffUtil.ItemCallback<ArticleList>() {
        /** Порівняння за унікальним первинним ключем Room */
        override fun areItemsTheSame(oldItem: ArticleList, newItem: ArticleList): Boolean {
            return oldItem.id == newItem.id
        }

        /** Порівняння значень усіх полів товару */
        override fun areContentsTheSame(oldItem: ArticleList, newItem: ArticleList): Boolean {
            return oldItem == newItem
        }
    }

    /**
     * Інтерфейс вибору товару зі списку
     */
    interface Listener {
        fun onClick(article: ArticleList)
    }

    /**
     * Реалізація фільтрації для пошукового рядка (SearchView / EditText)
     */
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                return FilterResults().apply {
                    values = if (constraint.isNullOrEmpty())
                        originalList
                    else
                        onFilter(originalList, constraint.toString())
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                submitList(results?.values as? List<ArticleList>, true)
            }
        }
    }

    /**
     * Алгоритм фільтрації: перевірка збігу назви або цифрового ID товару
     */
    fun onFilter(list: List<ArticleList>, constraint: String): List<ArticleList> {
        return list.filter {
            it.name.contains(constraint, true) || it.id.toString().contains(constraint, true)
        }
    }

    /**
     * Внутрішнє оновлення списку з урахуванням статусу фільтрації
     */
    private fun submitList(list: List<ArticleList>?, filtered: Boolean) {
        if (!filtered)
            originalList = list ?: listOf()

        super.submitList(list)
    }
}