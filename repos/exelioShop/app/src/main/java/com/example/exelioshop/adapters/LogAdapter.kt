package com.example.exelioshop.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.exelioshop.R
import com.example.exelioshop.databinding.LogItemBinding
import com.example.exelioshop.domain.models.LogInf

/**
 * =================================================================================================
 * LogAdapter — Адаптер відображення списку діагностичних подій та протокольних логів.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * Відображає стрічку подій (команди, відповіді, статуси підключення та таймаути) у LogFragment
 * для налагодження та моніторингу взаємодії з фіскальним реєстратором або сканером.
 * =================================================================================================
 */
class LogAdapter : RecyclerView.Adapter<LogAdapter.LogHolder>() {
    /** Локальний список записів логу */
    val logList = ArrayList<LogInf>()

    /**
     * ViewHolder для одного повідомлення логу
     */
    class LogHolder(item: View) : RecyclerView.ViewHolder(item) {
        val binding = LogItemBinding.bind(item)
        fun bind(log: LogInf) = with(binding) {
            tvName.text = log.body
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.log_item, parent, false)
        return LogHolder(view)
    }

    override fun onBindViewHolder(holder: LogHolder, position: Int) {
        holder.bind(logList[position])
    }

    override fun getItemCount(): Int {
        return logList.size
    }

    /**
     * Додавання нового повідомлення до кінця журналу
     */
    fun addLog(log: LogInf) {
        logList.add(log)
    }

    /**
     * Повна заміна списку логів новим набором даних
     */
    fun replace(log: ArrayList<LogInf>) {
        logList.clear()
        logList.addAll(log)
    }
}