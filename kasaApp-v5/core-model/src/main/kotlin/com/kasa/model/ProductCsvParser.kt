package com.kasa.model

/**
 * Парсер експорту та імпорту номенклатури товарів у формат CSV (Excel-сумісний).
 * Забезпечує масове завантаження бази та обмін даними з обліковими системами.
 */
object ProductCsvParser {

    private const val HEADER = "PLU,Назва,Ціна,Собівартість,Штрихкоди,Категорія,Ставка_ПДВ,Одиниця,Ваговий,УКТЗЕД,Залишок,Мін_Залишок,Обране"

    /**
     * Експортує список товарів у формат CSV із роздільником кома (,).
     */
    fun exportToCsv(products: List<Product>): String {
        val sb = StringBuilder()
        sb.append(HEADER).append("\n")
        for (p in products) {
            val escapedName = escapeCsvField(p.name)
            val barcodesJoined = p.barcodes.joinToString("|")
            val escapedCategory = escapeCsvField(p.categoryName)
            val line = listOf(
                p.id.toString(),
                escapedName,
                "%.2f".format(p.price).replace(',', '.'),
                "%.2f".format(p.costPrice).replace(',', '.'),
                barcodesJoined,
                escapedCategory,
                p.taxGroup.name,
                p.unitOfMeasurement,
                if (p.isWeighted) "1" else "0",
                p.uktzed,
                "%.3f".format(p.remainder).replace(',', '.'),
                "%.3f".format(p.minRemainder).replace(',', '.'),
                if (p.isFavorite) "1" else "0"
            ).joinToString(",")
            sb.append(line).append("\n")
        }
        return sb.toString()
    }

    /**
     * Імпортує список товарів із тексту CSV (підтримує роздільники кома або крапка з комою).
     */
    fun importFromCsv(csvText: String): List<Product> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val result = mutableListOf<Product>()
        val dataLines = if (lines.first().startsWith("PLU", ignoreCase = true)) lines.drop(1) else lines

        for (line in dataLines) {
            val tokens = parseCsvLine(line)
            if (tokens.size < 3) continue

            val plu = tokens.getOrNull(0)?.toLongOrNull() ?: continue
            val name = tokens.getOrNull(1).orEmpty().trim('"', ' ')
            if (name.isBlank()) continue
            val price = tokens.getOrNull(2)?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            val costPrice = tokens.getOrNull(3)?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            val barcodesRaw = tokens.getOrNull(4).orEmpty()
            val barcodes = barcodesRaw.split('|', ';', ',')
                .map { it.trim().trim('"', ' ') }
                .filter { it.isNotBlank() }
            val category = tokens.getOrNull(5).orEmpty().trim('"', ' ')
            val taxGroupName = tokens.getOrNull(6).orEmpty().trim()
            val taxGroup = TaxGroup.entries.find { it.name.equals(taxGroupName, ignoreCase = true) } ?: TaxGroup.GROUP_A
            val unit = tokens.getOrNull(7)?.trim()?.ifBlank { "шт" } ?: "шт"
            val isWeighted = tokens.getOrNull(8)?.trim() in listOf("1", "true", "так", "ваговий")
            val uktzed = tokens.getOrNull(9).orEmpty().trim()
            val remainder = tokens.getOrNull(10)?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            val minRemainder = tokens.getOrNull(11)?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            val isFavorite = tokens.getOrNull(12)?.trim() in listOf("1", "true", "так")

            result.add(
                Product(
                    id = plu,
                    name = name,
                    price = price,
                    costPrice = costPrice,
                    barcodes = barcodes,
                    categoryName = category,
                    taxGroup = taxGroup,
                    unitOfMeasurement = unit,
                    isWeighted = isWeighted,
                    uktzed = uktzed,
                    remainder = remainder,
                    minRemainder = minRemainder,
                    isFavorite = isFavorite
                )
            )
        }
        return result
    }

    private fun escapeCsvField(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val delimiter = if (line.contains(';') && !line.contains(',')) ';' else ','
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}
