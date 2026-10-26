package com.nekorom.bt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nekorom.bt.databinding.ListItemBinding

/**
 * Адаптер списку Bluetooth-пристроїв у модулі BluetoothModule.
 * Шлях у системі: BluetoothModule/bt/src/main/java/com/nekorom/bt/ItemAdapter.kt
 *
 * Підтримує два режими:
 * - adapterType == false: спарені пристрої з CheckBox (вибір активного пристрою).
 * - adapterType == true: виявлені пристрої без CheckBox (при кліку виконується device.createBond()).
 */
class ItemAdapter(private val listener: Listener, val adapterType: Boolean) :
    ListAdapter<ListItem, ItemAdapter.MyHolder>(Comparator()) {

    private var oldCheckBox: CheckBox? = null

    /**
     * ViewHolder для рядка списку пристрою.
     */
    class MyHolder(
        view: View,
        private val adapter: ItemAdapter,
        private val listener: Listener,
        val adapterType: Boolean
    ) : RecyclerView.ViewHolder(view) {

        private val binding = ListItemBinding.bind(view)
        private var item1: ListItem? = null

        init {
            // Клік по чекбоксу для вибору
            binding.checkBox.setOnClickListener {
                item1?.let { it1 -> listener.onClick(device = it1) }
                adapter.selectedCheckBox(it as CheckBox)
            }

            // Клік по всьому елементу: створення зв'язку або вибір
            itemView.setOnClickListener {
                if (adapterType) {
                    try {
                        item1?.device?.createBond()
                    } catch (e: SecurityException) {
                        // Немає дозволу BLUETOOTH_ADMIN/CONNECT
                    }
                } else {
                    item1?.let { it1 -> listener.onClick(device = it1) }
                    adapter.selectedCheckBox(binding.checkBox)
                }
            }
        }

        fun bind(item: ListItem) = with(binding) {
            checkBox.visibility = if (adapterType) View.GONE else View.VISIBLE
            item1 = item

            try {
                name.text = item.device.name ?: "Unknown Device"
                mac.text = item.device.address
            } catch (e: SecurityException) {
            }

            if (item.isChecked)
                adapter.selectedCheckBox(binding.checkBox)
        }
    }

    class Comparator : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem.device.address == newItem.device.address
        }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return MyHolder(view, this, listener, adapterType)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Сингл-селект для чекбоксів (радіокнопкова логіка)
     */
    fun selectedCheckBox(checkBox: CheckBox) {
        oldCheckBox?.isChecked = false
        oldCheckBox = checkBox
        oldCheckBox?.isChecked = true
    }

    interface Listener {
        fun onClick(device: ListItem)
    }
}