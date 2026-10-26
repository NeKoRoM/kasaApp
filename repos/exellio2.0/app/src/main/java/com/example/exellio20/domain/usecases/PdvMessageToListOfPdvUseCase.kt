package com.example.exellio20.domain.usecases

import com.example.exellio20.roomDB.entities.PdvItem

/**
 * ====================================================================================================
 * ЮЗКЕЙС РОЗБОРУ СТАВОК ПДВ ТА АКЦИЗУ (PDV MESSAGE TO LIST OF PDV USE CASE)
 * ====================================================================================================
 *
 * Перетворює текстову табуляторну відповідь касового апарата Exellio (команда 42 або 83)
 * у список структурованих об'єктів [PdvItem] для збереження в базу даних.
 *
 * СТРУКТУРА ПАКЕТА СТАВОК (8 ПОДАТКОВИХ ГРУП А..К):
 * Для кожної податкової групи (А, Б, В, Г, Х, М, Н, К) передаються 3 поля:
 * 1. `state` — ознака активності ставки (1 - активна, 0 - не використовується).
 * 2. `nds` — ставка ПДВ у відсотках (наприклад, "20.00", "7.00", "0.00").
 * 3. `excise` — ставка акцизного податку (наприклад, "5.00" або "0.00").
 */
class PdvMessageToListOfPdvUseCase {

    /**
     * Парсить відповідь РРО та повертає список лише активних податкових груп.
     *
     * @param message Рядок відповіді від РРО з розділювачами `\t`.
     * @return Список сутностей [PdvItem].
     */
    fun execute(message: String): List<PdvItem> {
        val pdvList = mutableListOf<PdvItem>()

        val dane = mutableListOf<Int>()
        for ((index, value) in message.withIndex()) {
            if (value.toString() == "\t") {
                dane.add(index)
            }
        }

        var readWrite = ""

        var stateA = ""
        var ndsA = ""
        var exciseA = ""

        var stateB = ""
        var ndsB = ""
        var exciseB = ""

        var stateV = ""
        var ndsV = ""
        var exciseV = ""

        var stateG = ""
        var ndsG = ""
        var exciseG = ""

        var stateH = ""
        var ndsH = ""
        var exciseH = ""

        var stateM = ""
        var ndsM = ""
        var exciseM = ""

        var stateN = ""
        var ndsN = ""
        var exciseN = ""

        var stateK = ""
        var ndsK = ""
        var exciseK = ""

        for ((index, value) in message.withIndex()) {
            if (index > dane[0] && index < dane[1])
                readWrite += value
            else if (index > dane[1] && index < dane[2])
                stateA += value
            else if (index > dane[2] && index < dane[3])
                ndsA += value
            else if (index > dane[3] && index < dane[4])
                exciseA += value
            else if (index > dane[4] && index < dane[5])
                stateB += value
            else if (index > dane[5] && index < dane[6])
                ndsB += value
            else if (index > dane[6] && index < dane[7])
                exciseB += value
            else if (index > dane[7] && index < dane[8])
                stateV += value
            else if (index > dane[8] && index < dane[9])
                ndsV += value
            else if (index > dane[9] && index < dane[10])
                exciseV += value
            else if (index > dane[10] && index < dane[11])
                stateG += value
            else if (index > dane[11] && index < dane[12])
                ndsG += value
            else if (index > dane[12] && index < dane[13])
                exciseG += value
            else if (index > dane[13] && index < dane[14])
                stateH += value
            else if (index > dane[14] && index < dane[15])
                ndsH += value
            else if (index > dane[15] && index < dane[16])
                exciseH += value
            else if (index > dane[16] && index < dane[17])
                stateM += value
            else if (index > dane[17] && index < dane[18])
                ndsM += value
            else if (index > dane[18] && index < dane[19])
                exciseM += value
            else if (index > dane[19] && index < dane[20])
                stateN += value
            else if (index > dane[20] && index < dane[21])
                ndsN += value
            else if (index > dane[21] && index < dane[22])
                exciseN += value
            else if (index > dane[22] && index < dane[23])
                stateK += value
            else if (index > dane[23] && index < dane[24])
                ndsK += value
            else if (index > dane[24] && index < dane[25])
                exciseK += value
        }

        // Додавання активних податкових груп у вихідний список
        if (stateA != "0") {
            pdvList.add(PdvItem(id = 1, pdvName = "A", pdvNds = ndsA, pdvExcise = exciseA))
        }
        if (stateB != "0") {
            pdvList.add(PdvItem(id = 2, pdvName = "Б", pdvNds = ndsB, pdvExcise = exciseB))
        }
        if (stateV != "0") {
            pdvList.add(PdvItem(id = 3, pdvName = "В", pdvNds = ndsV, pdvExcise = exciseV))
        }
        if (stateG != "0") {
            pdvList.add(PdvItem(id = 4, pdvName = "Г", pdvNds = ndsG, pdvExcise = exciseG))
        }
        if (stateH != "0") {
            pdvList.add(PdvItem(id = 5, pdvName = "Х", pdvNds = ndsH, pdvExcise = exciseH))
        }
        if (stateM != "0") {
            pdvList.add(PdvItem(id = 6, pdvName = "М", pdvNds = ndsM, pdvExcise = exciseM))
        }
        if (stateN != "0") {
            pdvList.add(PdvItem(id = 7, pdvName = "Н", pdvNds = ndsN, pdvExcise = exciseN))
        }
        if (stateK != "0") {
            pdvList.add(PdvItem(id = 8, pdvName = "K", pdvNds = ndsK, pdvExcise = exciseK))
        }
        return pdvList
    }
}

