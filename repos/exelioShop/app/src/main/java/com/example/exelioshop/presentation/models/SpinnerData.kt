package com.example.exelioshop.presentation.models

/**
 * [SpinnerData] — допоміжна модель даних для заповнення випадаючих списків (Spinner) у формах.
 * 
 * Зв'язує видиму користувачеві назву пункту ([title]) із його внутрішнім фіскальним значенням ([containsValue]),
 * що відправляється в протокол каси Datecs (наприклад: "програмована ціна" -> "0", "вільна ціна" -> "1").
 * 
 * @property title назва пункту, яка відображається у випадаючому списку
 * @property containsValue внутрішнє значення параметра протоколу
 */
data class SpinnerData(
    val title: String,
    val containsValue: String
) {
    override fun toString(): String {
        return title
    }
}
