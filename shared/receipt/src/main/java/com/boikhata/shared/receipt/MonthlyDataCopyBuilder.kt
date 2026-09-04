package com.boikhata.shared.receipt

/** P6 local data copy. Returns RFC-4180-compatible CSV text without cloud access. */
object MonthlyDataCopyBuilder {
    data class Row(val section: String, val label: String, val quantity: Int, val amount: Double)

    fun build(shopName: String, year: Int, month: Int, rows: List<Row>): String = buildString {
        appendLine("বই খাতা ডেটা কপি")
        appendLine(csvRow("দোকান", shopName, "", ""))
        appendLine(csvRow("মাস", "$year-$month", "", ""))
        appendLine(csvRow("বিভাগ", "নাম", "পরিমাণ", "টাকা"))
        rows.forEach { row ->
            appendLine(csvRow(row.section, row.label, row.quantity.toString(), "%.2f".format(java.util.Locale.US, row.amount)))
        }
    }

    private fun csvRow(vararg values: String): String = values.joinToString(",") { value ->
        val escaped = value.replace("\"", "\"\"")
        if (escaped.any { it == ',' || it == '\"' || it == '\n' || it == '\r' }) "\"$escaped\"" else escaped
    }
}
