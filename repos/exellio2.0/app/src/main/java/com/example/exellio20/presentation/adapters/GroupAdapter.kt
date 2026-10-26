package com.example.exellio20.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exellio20.R
import com.example.exellio20.databinding.GroupListItemBinding
import com.example.exellio20.roomDB.entities.GroupItem

/**
 * [GroupAdapter] — адаптер [RecyclerView] для відображення та керування товарними групами у налаштуваннях РРО.
 * 
 * Використовує [ListAdapter] з [DiffUtil] для плавної анімації оновлень списку груп.
 * Кожен елемент відображає:
 * - Номер групи в касі (`groupValue`)
 * - Назву групи (`groupName`)
 * - Мітку "акцизна група" при увімкненому прапорці `groupExcise`
 * - Кнопку видалення та клік по картці для редагування через інтерфейс [Listener].
 */
class GroupAdapter(private val listener: Listener) :
    ListAdapter<GroupItem, GroupAdapter.ItemHolder>(ItemComparator()) {

    /**
     * [ItemHolder] — ViewHolder елемента списку товарної групи.
     */
    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = GroupListItemBinding.bind(view)

        /**
         * Прив'язка даних [GroupItem] до UI компонентів віджета.
         */
        fun setData(groupItem: GroupItem, listener: Listener) = with(binding) {
            tvTitle.text = groupItem.groupName
            tvValue.text = groupItem.groupValue

            // Індикація підакцизності товарної групи (алкоголь, тютюн тощо)
            if (groupItem.groupExcise) {
                tvState.visibility = View.VISIBLE
                tvState.text = "акцизна група"
            } else {
                tvState.visibility = View.GONE
            }

            // Видалення товарної групи
            imDelete.setOnClickListener {
                listener.deleteItem(groupItem.id)
            }

            // Вибір групи для редагування або застосування
            cardV.setOnClickListener {
                listener.setValue(groupItem)
            }
        }

        companion object {
            /** Фабричний метод створення ViewHolder з розмітки XML */
            fun create(parent: ViewGroup): ItemHolder {
                return ItemHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.group_list_item, parent, false)
                )
            }
        }
    }

    /**
     * [ItemComparator] — колбек порівняння елементів списку для оптимізації рендерингу RecyclerView.
     */
    class ItemComparator : DiffUtil.ItemCallback<GroupItem>() {
        override fun areItemsTheSame(oldItem: GroupItem, newItem: GroupItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GroupItem, newItem: GroupItem): Boolean {
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
     * Інтерфейс зворотного виклику для взаємодії з подіями елементів списку товарних груп.
     */
    interface Listener {
        /** Видалення групи за ID */
        fun deleteItem(id: Int)
        /** Вибір або редагування групи */
        fun setValue(groupItem: GroupItem)
    }
}