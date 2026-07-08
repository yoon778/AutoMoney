package com.choiyoonseo.automoney.ui.components

fun accountOptionsForEdit(
    accountNames: List<String>,
    currentPaymentMethod: String?
): List<String> {
    val registered = accountNames
        .mapNotNull { it.cleanAccountNameOrNull() }
        .distinct()
    val current = currentPaymentMethod.cleanAccountNameOrNull()
    return when {
        current == null -> registered
        current in registered -> registered
        else -> listOf(current) + registered
    }
}

fun accountLabelForEdit(
    paymentMethod: String?,
    accountNames: List<String>
): String? =
    accountOptionsForEdit(accountNames, paymentMethod).firstOrNull()

private fun String?.cleanAccountNameOrNull(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }
