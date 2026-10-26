package com.example.exellio20.presentation.fragments

import androidx.fragment.app.Fragment

/**
 * [BaseFragment] — базовий абстрактний клас для всіх фрагментів інтерфейсу касира у проекті Exellio 2.0.
 * 
 * Надає єдиний контракт [onClickNew], що викликається з меню навігації або панелі інструментів
 * головної активності для виконання специфічної дії (наприклад, додавання нового товару, групи або коду УКТЗЕД).
 */
abstract class BaseFragment : Fragment() {
    /**
     * Дія створення нового сутності або первинної взаємодії, специфічна для кожного активного фрагмента.
     */
    abstract fun onClickNew()
}