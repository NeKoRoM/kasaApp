// =================================================================================================
// USE CASE: ПАРСИНГ ТАБЛИЦІ ПОДАТКОВИХ СТАВОК (ПДВ + АКЦИЗ) З РРО DATECS / EXELLIO
//
// Архітектурний шар: Domain Layer (Use Case).
// Призначення:
// Десеріалізує таблицю податкових ставок касового апарата (відповідь на команду 83 / 0x53 PDV_REQUEST).
// Законодавство України передбачає до 8 фіскальних податкових груп, які позначаються літерами:
// 'А' - ПДВ основна ставка (зазвичай 20%)
// 'Б' - ПДВ ставка для фармпрепаратів / спецрежим (7%)
// 'В' - ПДВ нульова ставка (0%)
// 'Г' - Без ПДВ (звільнені від оподаткування)
// 'Х', 'М', 'Н', 'К' - Додаткові / акцизні групи (роздрібний акциз 5% на тютюн/алкоголь)
//
// Формат кожного блоку податкової групи у відповіді РРО (3 поля через '\t'):
// 1. Статус активності групи (state: "0" - вимкнена, "1" - активна)
// 2. Ставка ПДВ (nds: відсоток помножений на 100, наприклад "2000" = 20.00%)
// 3. Ставка акцизного збору (excise: відсоток помножений на 100, наприклад "500" = 5.00%)
// =================================================================================================

package com.nekorom.kasaapp.domain.usecase

import com.example.exellio20.roomDB.entities.PdvItem

/**
 * Прецедент вилучення активних податкових ставок із конфігураційного пакету РРО.
 */
class PdvMessageToListOfPdvUseCase {

    /**
     * Парсить відповідь РРО та повертає список лише активних податкових груп ([state] != "0").
     *
     * @param message Сирий текстовий рядок відповіді на команду 83 від Datecs/Exellio.
     * @param userId Ідентифікатор касира/користувача для ізоляції записів у Room DB.
     * @return Список активних податкових ставок [PdvItem].
     */
    fun execute(message: String, userId : Int): List<PdvItem> {
        val pdvList = mutableListOf<PdvItem>()

        // Крок 1: Збір індексів розділювачів табуляції '\t'
        val dane = mutableListOf<Int>()
        for ((index, value) in message.withIndex()) {
            if (value.toString() == "\t") {
                dane.add(index)
            }
        }

        // Крок 2: Змінні для полів відповіді РРО
        var readWrite = ""

        // Податкова група 'А'
        var stateA = ""
        var ndsA = ""
        var exciseA = ""

        // Податкова група 'Б'
        var stateB = ""
        var ndsB = ""
        var exciseB = ""

        // Податкова група 'В'
        var stateV = ""
        var ndsV = ""
        var exciseV = ""

        // Податкова група 'Г'
        var stateG = ""
        var ndsG = ""
        var exciseG = ""

        // Податкова група 'Х'
        var stateH = ""
        var ndsH = ""
        var exciseH = ""

        // Податкова група 'М'
        var stateM = ""
        var ndsM = ""
        var exciseM = ""

        // Податкова група 'Н'
        var stateN = ""
        var ndsN = ""
        var exciseN = ""

        // Податкова група 'К'
        var stateK = ""
        var ndsK = ""
        var exciseK = ""

        // Крок 3: Посимвольний розподіл параметрів між табуляціями
        for ((index, value) in message.withIndex()) {
            try {
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
            } catch (e: Exception) {
                // При збої формату повертаємо вже сформовану частину списку
                return pdvList
            }
        }

        // Крок 4: Формування сутностей PdvItem лише для активованих у РРО груп
        if (stateA != "0") {
            pdvList.add(PdvItem(pdvId = 1, pdvName = "A", pdvNds = ndsA, pdvExcise = exciseA, userId = userId))
        }
        if (stateB != "0") {
            pdvList.add(PdvItem(pdvId = 2, pdvName = "Б", pdvNds = ndsB, pdvExcise = exciseB, userId = userId))
        }
        if (stateV != "0") {
            pdvList.add(PdvItem(pdvId = 3, pdvName = "В", pdvNds = ndsV, pdvExcise = exciseV, userId = userId))
        }
        if (stateG != "0") {
            pdvList.add(PdvItem(pdvId = 4, pdvName = "Г", pdvNds = ndsG, pdvExcise = exciseG, userId = userId))
        }
        if (stateH != "0") {
            pdvList.add(PdvItem(pdvId = 5, pdvName = "Х", pdvNds = ndsH, pdvExcise = exciseH, userId = userId))
        }
        if (stateM != "0") {
            pdvList.add(PdvItem(pdvId = 6, pdvName = "М", pdvNds = ndsM, pdvExcise = exciseM, userId = userId))
        }
        if (stateN != "0") {
            pdvList.add(PdvItem(pdvId = 7, pdvName = "Н", pdvNds = ndsN, pdvExcise = exciseN, userId = userId))
        }
        if (stateK != "0") {
            // Примітка: pdvName для 8-ї групи у вихідному коді вказано як "A" замість "К"
            pdvList.add(PdvItem(pdvId = 8, pdvName = "A", pdvNds = ndsK, pdvExcise = exciseK, userId = userId))
        }
        return pdvList
    }
}
