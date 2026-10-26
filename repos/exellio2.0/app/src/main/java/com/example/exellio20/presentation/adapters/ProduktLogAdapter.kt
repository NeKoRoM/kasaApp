package com.example.exellio20.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.exellio20.R

/**
 * [ProductLogBar] — модель запису журналу розпізнаних штрихкодів під час сканування товарів.
 * 
 * @property name назва знайденого товару
 * @property barcode відсканований штрихкод (EAN-13, ваговий штрихкод тощо)
 */
data class ProductLogBar(val name: String, val barcode: String)

/**
 * [ProductLogAdapter] — адаптер [RecyclerView] для відображення хронологічного журналу сканування штрихкодів.
 * 
 * Використовується в екрані сканера для відображення касиру останніх розпізнаних позицій.
 * Нові елементи вставляються на початок списку (`position = 0`) для миттєвої візуальної реакції.
 */
class ProductLogAdapter(private val productLogList: MutableList<ProductLogBar>) :
    RecyclerView.Adapter<ProductLogAdapter.ProductLogViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_log, parent, false)
        return ProductLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductLogViewHolder, position: Int) {
        val productLog = productLogList[position]
        holder.bind(productLog)
    }

    override fun getItemCount(): Int {
        return productLogList.size
    }

    /**
     * ViewHolder одного рядка журналу сканування: назва товару та розпізнаний штрихкод.
     */
    inner class ProductLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val barcodeTextView: TextView = itemView.findViewById(R.id.barcodeTextView)

        fun bind(productLog: ProductLogBar) {
            nameTextView.text = productLog.name
            barcodeTextView.text = productLog.barcode
        }
    }

    /**
     * Додавання нового розпізнаного товару на початок журналу сканування з анімацією вставки.
     * 
     * @param productLog об'єкт з назвою та штрихкодом товару
     */
    fun addItem(productLog: ProductLogBar) {
        productLogList.add(0, productLog)
        notifyItemInserted(0)
    }

    /**
     * Повне очищення журналу сканування сесії.
     */
    fun clearItems() {
        productLogList.clear()
        notifyDataSetChanged()
    }
}