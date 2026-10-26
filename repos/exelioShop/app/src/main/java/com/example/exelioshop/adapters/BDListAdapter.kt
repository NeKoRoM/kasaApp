package com.example.exelioshop.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.exelioshop.R
import com.example.exelioshop.classes.Article
import com.example.exelioshop.databinding.BdlistItemBinding

/**
 * =================================================================================================
 * BDListAdapter — Адаптер відображення списку товарів (артикулів), отриманих з РРО Datecs.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * Працює напряму з моделлю Article.ArticleData, розпарсеною з сирої відповіді протоколу Datecs.
 * Відображає код товару (PLU), найменування та ціну в списку для подальшого вибору або редагування.
 * =================================================================================================
 */
class BDListAdapter(val listener: Listener) : RecyclerView.Adapter<BDListAdapter.BDListHolder>() {

    /** Локальний список артикулів */
    val bdlist = ArrayList<Article.ArticleData>()

    /**
     * ViewHolder для рядка артикулу в списку
     */
    class BDListHolder(item: View) : RecyclerView.ViewHolder(item) {
        val binding = BdlistItemBinding.bind(item)

        /**
         * Прив'язка реквізитів артикулу: PLU-код, назва, ціна та слухач кліку
         */
        fun bind(bdlist: Article.ArticleData, listener: Listener) = with(binding) {
            id.text = bdlist.code
            name.text = bdlist.name
            price.text = bdlist.price
            cardV.setOnClickListener {
                listener.onClick(bdlist)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BDListHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bdlist_item, parent, false)
        return BDListHolder(view)
    }

    override fun onBindViewHolder(holder: BDListHolder, position: Int) {
        holder.bind(bdlist[position], listener)
    }

    override fun getItemCount(): Int {
        return bdlist.size
    }

    /**
     * Додавання нового артикулу до списку з оновленням UI
     */
    fun addList(article: Article.ArticleData) {
        bdlist.add(article)
        notifyDataSetChanged()
    }

    /**
     * Інтерфейс кліку по артикулу (наприклад для відкриття картки редагування)
     */
    interface Listener {
        fun onClick(article: Article.ArticleData)
    }
}