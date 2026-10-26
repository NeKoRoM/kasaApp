package com.example.exellio20.presentation.fragments

import androidx.appcompat.app.AppCompatActivity
import com.example.exellio20.R

/**
 * [FragmentManager] — синглтон-навігатор для перемикання головних екранів (фрагментів) застосунку.
 * 
 * Керує підміною поточного [BaseFragment] у контейнері розмітки `R.id.placeHolder` в [AppCompatActivity].
 * Зберігає посилання на активний екран у [currentFrag] для контролю життєвого циклу
 * та обробки подій апаратного сканера або оновлення даних.
 */
object FragmentManager {
    /** Посилання на поточний активний фрагмент екранної навігації */
    var currentFrag: BaseFragment? = null

    /**
     * Заміна вмісту контейнера `placeHolder` новим фрагментом.
     * 
     * @param newFrag новий екран, що наслідує [BaseFragment]
     * @param activity батьківська активність, що надає `supportFragmentManager`
     */
    fun setFragment(newFrag: BaseFragment, activity: AppCompatActivity) {
        val transaction = activity.supportFragmentManager.beginTransaction()
        transaction.replace(R.id.placeHolder, newFrag)
        transaction.commit()
        currentFrag = newFrag
    }
}