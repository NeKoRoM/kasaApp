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
 * Адаптер RecyclerView для відображення списків Bluetooth-пристроїв (спарених та виявлених).
 * Шлях у системі: kasaApp3.0/bt/src/main/java/com/nekorom/bt/ItemAdapter.kt
 *
 * Особливість:
 * - Підтримує два режими (adapterType):
 *   1. adapterType == false: Режим спарених пристроїв (Paired Devices).
 *      Відображається CheckBox вибору активного пристрою (радіокнопковий режим single-choice).
 *      При виборі пристрою зберігається його MAC-адреса для автоматичного підключення до РРО.
 *   2. adapterType == true: Режим виявлених пристроїв (Discovered Devices).
 *      CheckBox приховано. Клік по елементу запускає процедуру створення зв'язку (Bluetooth Pairing / Bonding: device.createBond()).
 */
class ItemAdapter(private val listener: Listener, val adapterType: Boolean) :
    ListAdapter<ListItem, ItemAdapter.MyHolder>(Comparator()) {

    /** Список зареєстрованих слухачів подій кліку */
    val listenerList = mutableListOf(listener)

    /** Реєстрація додаткового слухача */
    fun addListener(listener: Listener) {
        listenerList.add(listener)
    }

    /** Посилання на попередньо обраний CheckBox для скидання попереднього вибору */
    private var oldCheckBox: CheckBox? = null

    /**
     * ViewHolder для окремого елемента Bluetooth-пристрою у списку.
     */
    class MyHolder(
        view: View,
        private val adapter: ItemAdapter,
        private val listenerList: List<Listener>,
        val adapterType: Boolean
    ) : RecyclerView.ViewHolder(view) {

        private val binding = ListItemBinding.bind(view)
        private var item1: ListItem? = null

        init {
            // Обробка кліку безпосередньо по CheckBox
            binding.checkBox.setOnClickListener {
                item1?.let { it1 ->
                    listenerList.forEach { listener ->
                        listener.onClick(device = it1)
                        listener.onClickMac(it1.device.address)
                    }
                }
                adapter.selectedCheckBox(it as CheckBox)
            }

            // Обробка кліку по всій площині рядка
            itemView.setOnClickListener {
                if (adapterType) {
                    // Режим виявлених пристроїв: запускаємо сполучення (Pairing/Bonding)
                    try {
                        item1?.device?.createBond()
                    } catch (e: SecurityException) {
                        // Відсутній дозвіл BLUETOOTH_ADMIN/CONNECT
                    }
                } else {
                    // Режим спарених пристроїв: вибираємо пристрій як активний РРО/ваги
                    item1?.let { it1 ->
                        listenerList.forEach { listener ->
                            listener.onClick(device = it1)
                            listener.onClickMac(it1.device.address)
                        }
                    }
                    adapter.selectedCheckBox(binding.checkBox)
                }
            }
        }

        /**
         * Прив'язка даних пристрою до UI елементів.
         */
        fun bind(item: ListItem) = with(binding) {
            // Відображаємо чекбокс лише для спарених пристроїв
            checkBox.visibility = if (adapterType) View.GONE else View.VISIBLE
            item1 = item

            try {
                name.text = item.device.name ?: "Unknown"
                mac.text = item.device.address
            } catch (e: SecurityException) {
                // Обробка відсутності Bluetooth дозволів на Android 12+
            }

            // Якщо пристрій збігається зі збереженим раніше — робимо чекбокс активним
            if (item.isChecked)
                adapter.selectedCheckBox(binding.checkBox)
        }
    }

    /**
     * Оптимізований механізм порівняння елементів DiffUtil для плавного оновлення списку.
     */
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
        return MyHolder(view, this, listenerList, adapterType)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Забезпечує радіокнопкову логіку: при виборі одного CheckBox попередній скидається.
     */
    fun selectedCheckBox(checkBox: CheckBox) {
        oldCheckBox?.isChecked = false
        oldCheckBox = checkBox
        oldCheckBox?.isChecked = true
    }

    /**
     * Інтерфейс зворотного виклику при виборі пристрою користувачем.
     */
    interface Listener {
        fun onClick(device: ListItem)
        fun onClickMac(mac: String)
    }
}