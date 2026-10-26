package com.example.exellio20.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exellio20.R
import com.example.exellio20.databinding.UktzListItemBinding
import com.example.exellio20.roomDB.entities.UktzItem

/**
 * [UktzAdapter] — адаптер [RecyclerView] для відображення довідника кодів УКТЗЕД.
 * 
 * Використовується на екрані налаштувань УКТЗЕД для перегляду, редагування та видалення
 * кодів класифікатора товарів зовнішньоекономічної діяльності (обов'язкових для підакцизу в чеках РРО).
 */
class UktzAdapter(private val listener: Listener) :
    ListAdapter<UktzItem, UktzAdapter.ItemHolder>(ItemComparator()) {

    /**
     * [ItemHolder] — ViewHolder одного запису коду УКТЗЕД (назва та цифровий код).
     */
    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = UktzListItemBinding.bind(view)

        fun setData(uktzItem: UktzItem, listener: Listener) = with(binding) {
            tvTitle.text = uktzItem.pdvName
            tvValue.text = uktzItem.pdvValue

            // Видалення коду УКТЗЕД
            imDelete.setOnClickListener {
                listener.deleteItem(uktzItem.id)
            }
            // Вибір для редагування
            cardV.setOnClickListener {
                listener.setValue(uktzItem)
            }
        }

        companion object {
            fun create(parent: ViewGroup): ItemHolder {
                return ItemHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.uktz_list_item, parent, false)
                )
            }
        }
    }

    /**
     * [ItemComparator] — порівняння елементів для швидкого оновлення списку.
     */
    class ItemComparator : DiffUtil.ItemCallback<UktzItem>() {
        override fun areItemsTheSame(oldItem: UktzItem, newItem: UktzItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UktzItem, newItem: UktzItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        holder.setData(getItem(position), listener)
    }

    /**
     * Інтерфейс зворотного виклику для взаємодії з кодами УКТЗЕД.
     */
    interface Listener {
        /** Передача коду УКТЗЕД на редагування */
        fun setValue(uktz: UktzItem)
        /** Видалення коду за його ідентифікатором */
        fun deleteItem(id: Int)
    }
}