package com.example.exelioshop.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exellio20.R
import com.example.exellio20.databinding.ListItemBinding
import com.example.exellio20.models.ListItem

/**
 * ====================================================================================================
 * АДАПТЕР СПИСКУ BLUETOOTH ПРИСТРОЇВ (RECYCLER VIEW ADAPTER - EXELLIO 2.0)
 * ====================================================================================================
 *
 * Використовує [ListAdapter] з [DiffUtil] для оптимізованого рендерингу знайдених Bluetooth-пристроїв.
 * Відображає назву пристрою та його апаратну MAC-адресу.
 *
 * @param listener Обробник вибору пристрою зі списку.
 */
class RcAdapter(private val listener: Listener) : ListAdapter<ListItem, RcAdapter.ItemHolder>(
    ItemComparator()
) {

    /**
     * ViewHolder елемента списку із прив'язкою даних до ViewBinding.
     */
    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemBinding.bind(view)

        fun setData(item: ListItem, listener: Listener) = with(binding) {
            tvName.text = item.name
            tvMac.text = item.mac
            itemView.setOnClickListener {
                listener.onClick(item)
            }
        }

        companion object {
            fun create(parent: ViewGroup): ItemHolder {
                return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))
            }
        }
    }

    /**
     * Компаратор різниць для оптимізованого оновлення RecyclerView.
     */
    class ItemComparator : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem.mac == newItem.mac
        }

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
     * Інтерфейс кліку на елемент списку пристроїв.
     */
    interface Listener {
        fun onClick(item: ListItem)
    }
}