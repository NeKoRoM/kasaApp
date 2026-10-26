package com.example.exelioshop.fragments

import androidx.appcompat.app.AppCompatActivity
import com.example.exelioshop.R

/**
 * =================================================================================================
 * FragmentManager — Об'єкт-синглтон швидкого перемикання фрагментів у POS-контейнері.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * Виконує заміну (FragmentTransaction.replace) активного фрагмента у розмітці R.id.placeHolder1
 * та зберігає посилання на поточний екран у currentFrag.
 * =================================================================================================
 */
object FragmentManager {
    /** Посилання на поточний активний фрагмент */
    var currentFrag: BaseFragment? = null

    /**
     * Заміна поточного фрагмента новим екземпляром
     * @param newFrag Новий фрагмент, що наслідує BaseFragment
     * @param activity Поточна активність (MainActivity)
     */
    fun setFragment(newFrag: BaseFragment, activity: AppCompatActivity) {
        val transaction = activity.supportFragmentManager.beginTransaction()
        transaction.replace(R.id.placeHolder1, newFrag)
        transaction.commit()
        currentFrag = newFrag
    }
}
