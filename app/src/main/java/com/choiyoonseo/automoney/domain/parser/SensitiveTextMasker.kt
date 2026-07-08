package com.choiyoonseo.automoney.domain.parser

object SensitiveTextMasker {
    private val sensitiveNumberPattern = Regex("""\b\d[\d-]{4,}\d\b""")
    private val amountPattern = Regex("""\b\d{6,}\s*(?:won|원)""", RegexOption.IGNORE_CASE)

    fun mask(text: String): String {
        val protectedAmounts = mutableListOf<String>()
        val protectedText = amountPattern.replace(text) { match ->
            val token = "__AMOUNT_${protectedAmounts.size}__"
            protectedAmounts += match.value
            token
        }

        val maskedText = sensitiveNumberPattern.replace(protectedText) { match ->
            val digits = match.value.filter(Char::isDigit)
            if (digits.length < 6) {
                match.value
            } else {
                "****" + digits.takeLast(4)
            }
        }

        return protectedAmounts.foldIndexed(maskedText) { index, current, amount ->
            current.replace("__AMOUNT_${index}__", amount)
        }
    }
}
