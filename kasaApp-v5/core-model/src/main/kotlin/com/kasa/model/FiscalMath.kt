package com.kasa.model

/**
 * Нормативна фіскальна математика відповідно до Податкового кодексу України
 * та Постанови Правління НБУ № 25 «Про оптимізацію обігу монет дрібних номіналів».
 * Усі розрахунки виконуються виключно в цілих копійках (Long).
 */
object FiscalMath {

    /**
     * Роздрібний акцизний податок 5% (включений у кінцеву вартість підакцизного товару):
     * Акциз = Вартість * 5 / 105
     */
    fun calculateExciseKopecks(totalKopecks: Long): Long {
        if (totalKopecks <= 0L) return 0L
        return Math.round(totalKopecks * 5.0 / 105.0)
    }

    /**
     * ПДВ 20% від бази оподаткування:
     * Якщо товар підакцизний, база = Вартість - Акциз.
     * ПДВ = База * 20 / 120 (або База / 6).
     */
    fun calculateVat20Kopecks(totalKopecks: Long, hasExcise: Boolean): Long {
        if (totalKopecks <= 0L) return 0L
        val baseWithoutExcise = if (hasExcise) {
            totalKopecks - calculateExciseKopecks(totalKopecks)
        } else {
            totalKopecks
        }
        return Math.round(baseWithoutExcise * 20.0 / 120.0)
    }

    /**
     * Пільгова ставка ПДВ 7% (медикаменти та вироби медичного призначення):
     * ПДВ = Вартість * 7 / 107
     */
    fun calculateVat7Kopecks(totalKopecks: Long): Long {
        if (totalKopecks <= 0L) return 0L
        return Math.round(totalKopecks * 7.0 / 107.0)
    }

    /**
     * Розрахунок податку для конкретної податкової групи.
     */
    fun calculateTaxKopecks(totalKopecks: Long, taxGroup: TaxGroup): Long {
        return when (taxGroup) {
            TaxGroup.GROUP_A -> calculateVat20Kopecks(totalKopecks, hasExcise = false)
            TaxGroup.GROUP_A_EXCISE -> calculateVat20Kopecks(totalKopecks, hasExcise = true)
            TaxGroup.GROUP_V -> calculateVat7Kopecks(totalKopecks)
            TaxGroup.GROUP_D_EXCISE -> 0L
            TaxGroup.GROUP_B,
            TaxGroup.GROUP_G,
            TaxGroup.GROUP_D -> 0L
        }
    }

    /**
     * Округлення загальної суми готівкового розрахунку (Постанова Правління НБУ № 25):
     * - від 1 до 4 копійок — зменшується в бік зменшення до найближчого значення, що закінчується на 0.
     * - від 5 до 9 копійок — збільшується в бік збільшення до найближчого значення, що закінчується на 0.
     *
     * Для безготівкових розрахунків (банківська картка) округлення суворо заборонено!
     */
    fun roundCashPayment(totalKopecks: Long): Long {
        if (totalKopecks == 0L) return 0L
        val sign = if (totalKopecks >= 0) 1 else -1
        val abs = Math.abs(totalKopecks)
        val lastDigit = (abs % 10).toInt()
        val base = abs - lastDigit
        val rounded = when {
            lastDigit in 1..4 -> base
            lastDigit in 5..9 -> base + 10
            else -> abs
        }
        return rounded * sign
    }
}
