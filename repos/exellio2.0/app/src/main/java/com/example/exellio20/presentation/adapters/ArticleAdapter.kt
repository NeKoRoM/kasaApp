package com.example.exellio20.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exellio20.R
import com.example.exellio20.databinding.ArticleListItemBinding
import com.example.exellio20.roomDB.entities.ArticleItem

/**
 * [UserActionListener] — інтерфейс зворотного виклику при взаємодії користувача з елементом товару.
 */
interface UserActionListener {
    /**
     * Клік по товару у списку (відкриття на редагування або додавання в чек).
     * 
     * @param article вибраний товар
     */
    fun onItemClick(article: ArticleItem)
}

/**
 * [ArticleAdapter] — адаптер [RecyclerView] для відображення каталогу товарів (артикулів) з підтримкою пошуку.
 * 
 * Особливості реалізації:
 * - Базується на [ListAdapter] з [DiffUtil] для швидкої асинхронної диференціації списків.
 * - Реалізує [Filterable] для миттєвої фільтрації товарів за назвою або фіскальним кодом (PLU).
 * - Зберігає еталонну копію списку `originalList` для коректного скидання фільтрів пошуку.
 */
class ArticleAdapter(private val listener: UserActionListener) :
    ListAdapter<ArticleItem, ArticleAdapter.ItemHolder>(ItemComparator()),
    Filterable {

    /** Оригінальний невідфільтрований список товарів для скидання пошукового запиту */
    private var originalList: List<ArticleItem> = currentList.toList()

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        holder.setData(getItem(position), listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder.create(parent = parent)
    }

    /**
     * [ItemHolder] — ViewHolder елемента списку товару (PLU, назва, ціна).
     */
    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ArticleListItemBinding.bind(view)

        fun setData(article: ArticleItem, listener: UserActionListener) = with(binding) {
            itemId.text = article.id.toString()
            itemName.text = article.name
            itemPrice.text = article.price
            cardV.setOnClickListener {
                listener.onItemClick(article)
            }
        }

        companion object {
            fun create(parent: ViewGroup): ItemHolder {
                return ItemHolder(
                    LayoutInflater
                        .from(parent.context)
                        .inflate(R.layout.article_list_item, parent, false)
                )
            }
        }
    }

    /**
     * [ItemComparator] — порівняння елементів для оптимізації рендерингу за допомогою DiffUtil.
     */
    class ItemComparator : DiffUtil.ItemCallback<ArticleItem>() {
        override fun areItemsTheSame(oldItem: ArticleItem, newItem: ArticleItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ArticleItem, newItem: ArticleItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun submitList(list: List<ArticleItem>?) {
        submitList(list, false)
    }

    /**
     * Реалізація фільтрації для рядка пошуку:
     * Пошук проводиться за збігом у назві товару або в його цифровому коді PLU.
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
                submitList(results?.values as? List<ArticleItem>, true)
            }
        }
    }

    /**
     * Алгоритм фільтрації: перевірка входження підрядка у назву (ігноруючи регістр) або у код PLU.
     */
    fun onFilter(list: List<ArticleItem>, constraint: String): List<ArticleItem> {
        return list.filter {
            it.name.contains(constraint, true) || it.id.toString().contains(constraint, true)
        }
    }

    /**
     * Перевизначений submitList з прапорцем `filtered`:
     * Якщо список прийшов з джерела даних (не результат фільтра), зберігаємо його копію в `originalList`.
     */
    private fun submitList(list: List<ArticleItem>?, filtered: Boolean) {
        if (!filtered)
            originalList = list ?: listOf()

        super.submitList(list)
    }
}
