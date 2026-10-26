package com.example.exelioshop.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exelioshop.dataclass.ListItem
import com.example.exelioshop.R
import com.example.exelioshop.databinding.ListItemBinding

/**
 * =================================================================================================
 * RcAdapter — Адаптер списку Bluetooth-пристроїв (РРО / сканери) на базі ListAdapter.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * Відображає знайдені або спарені Bluetooth-пристрої у діалозі/екрані вибору (SelectDevice).
 * Використовує DiffUtil для ефективного оновлення списку без зайвих перемальовувань.
 * =================================================================================================
 */
class RcAdapter(private val listener: Listener) : ListAdapter<ListItem, RcAdapter.ItemHolder>(
    ItemComparator()
) {

    /**
     * ViewHolder елемента пристрою (назва та MAC-адреса)
     */
    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemBinding.bind(view)

        /**
         * Прив'язка даних пристрою та обробника натискання
         */
        fun setData(item: ListItem, listener: Listener) = with(binding) {
            tvName.text = item.name
            tvMac.text = item.mac
            itemView.setOnClickListener {
                listener.onClick(item)
            }
        }

        companion object {
            /** Фабричний метод створення ViewHolder з розмітки list_item.xml */
            fun create(parent: ViewGroup): ItemHolder {
                return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))
            }
        }
    }

    /**
     * Компаратор DiffUtil для перевірки унікальності та еквівалентності пристроїв
     */
    class ItemComparator : DiffUtil.ItemCallback<ListItem>() {
        /** Порівняння за унікальною MAC-адресою пристрою */
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem.mac == newItem.mac
        }

        /** Порівняння за вмістом усіх полів */
        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
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
     * Інтерфейс зворотного виклику для обробки вибору пристрою зі списку
     */
    interface Listener {
        fun onClick(item: ListItem)
    }
}