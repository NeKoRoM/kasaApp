
package com.example.exelioshop.fragments.fragmentsscan

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exelioshop.R
import com.example.exelioshop.adapters.LogAdapter
import com.example.exelioshop.classes.VMData
import com.example.exelioshop.databinding.FragmentSettingsBinding
import com.example.exelioshop.databinding.FragmentSettingsScanBinding
import com.example.exelioshop.fragments.BaseFragment

/**
 * =================================================================================================
 * FragmentSettingsScan — Діагностична консоль ручного введення команд та перегляду логів.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * 1. Надає розробнику/інженеру текстове поле введення для надсилання сирих команд протоколу Datecs.
 * 2. Відображає журнал вхідних та вихідних пакетів (LogAdapter) у реальному часі зі скролом донизу.
 * =================================================================================================
 */
class FragmentSettingsScan : BaseFragment() {
    lateinit var binding: FragmentSettingsScanBinding
    private val dataModel: VMData by activityViewModels()
    private val adapter = LogAdapter()

    override fun onClickNew() {
        TODO("Not yet implemented")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Кнопка відправки сирої текстової команди в касу
        binding.button6.setOnClickListener {
            dataModel.logmess.value = binding.editTextTextPersonName2.text.toString()
        }

        // Налаштування списку відображення логів зі зворотним порядком додавання
        binding.rcLogView.layoutManager = LinearLayoutManager(activity?.applicationContext, LinearLayoutManager.VERTICAL, true)
        binding.rcLogView.adapter = adapter

        // Спостереження за новими подіями логу
        dataModel.logItem.observe(activity as LifecycleOwner, {
            adapter.addLog(it)
            binding.rcLogView.smoothScrollToPosition(binding.rcLogView.bottom)
            adapter.notifyDataSetChanged()
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            FragmentSettingsScan().apply {
                arguments = Bundle().apply {
                }
            }
    }
}