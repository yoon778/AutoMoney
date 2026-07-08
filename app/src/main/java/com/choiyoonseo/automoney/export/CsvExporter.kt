package com.choiyoonseo.automoney.export

import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import java.time.ZoneId

class CsvExporter {
    fun export(transactions: List<MoneyTransaction>): String {
        val header = "날짜,금액,분류,결제 수단,메모,월"
        val rows = transactions.map { transaction ->
            listOf(
                transaction.occurredAt.atZone(ZoneId.of("Asia/Seoul")).toLocalDate().toString(),
                transaction.amount.won.toString(),
                transaction.category?.displayName.orEmpty(),
                transaction.paymentMethod.orEmpty(),
                transaction.memo.orEmpty(),
                transaction.monthKey.toString()
            ).joinToString(",") { escapeCsv(it) }
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
