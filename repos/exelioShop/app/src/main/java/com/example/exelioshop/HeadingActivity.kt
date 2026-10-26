package com.example.exelioshop

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.exelioshop.databinding.ActivityHeadingBinding
import com.example.exelioshop.entities.ArticleList
import java.io.Serializable

/**
 * =================================================================================================
 * HeadingActivity — Екран редагування рядків заголовка (кліше) фіскального чека.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * Дозволяє користувачу налаштовувати текстові рядки фіскального заголовка чека РРО
 * (наприклад: назва торгової точки, адреса магазину, податковий номер / ЄДРПОУ, привітання).
 * При збереженні повертає List<String> через ActivityResult для подальшого запису в РРО.
 * =================================================================================================
 */
class HeadingActivity : AppCompatActivity() {

    /** Вхідний список існуючих рядків заголовка */
    var headingL: List<String>? = null

    lateinit var binding: ActivityHeadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityHeadingBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        getHeading()
        setHEading()

        // Кнопка збереження: збір 7 рядків у список та повернення результату
        binding.button3.setOnClickListener {
            binding.apply {
                val listH = listOf<String>(
                    editText1.text.toString(),
                    editText2.text.toString(),
                    editText3.text.toString(),
                    editText4.text.toString(),
                    editText5.text.toString(),
                    editText6.text.toString(),
                    editText7.text.toString(),
                )
                val editIntent = Intent().apply {
                    putExtra("ListH", listH as Serializable)
                }
                setResult(RESULT_OK, editIntent)
                finish()
            }
        }
    }

    /**
     * Заповнення полів форми існуючими рядками кліше
     */
    private fun setHEading() = with(binding) {
        if (!headingL.isNullOrEmpty()) {
            editText1.setText(headingL!![0])
            editText2.setText(headingL!![1])
            editText3.setText(headingL!![2])
            editText4.setText(headingL!![3])
            editText5.setText(headingL!![4])
            editText6.setText(headingL!![5])
            editText7.setText(headingL!![6])
        }
    }

    /**
     * Отримання переданого ззовні списку рядків із Intent
     */
    private fun getHeading() {
        headingL = intent.getSerializableExtra("to_heading") as? List<String>
        Log.d("MyLog", intent.getSerializableExtra("to_heading").toString())
    }
}