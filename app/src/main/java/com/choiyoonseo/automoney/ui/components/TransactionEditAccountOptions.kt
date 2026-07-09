package com.choiyoonseo.automoney.ui.components

import com.choiyoonseo.automoney.domain.assets.canonicalMoneyNameKey
import com.choiyoonseo.automoney.domain.assets.moneyNamesMatch

fun accountOptionsForEdit(
    accountNames: List<String>,
    currentPaymentMethod: String?
): List<String> {
    val registered = accountNames
        .mapNotNull { it.cleanAccountNameOrNull() }
        .distinctBy { it.canonicalMoneyNameKey() }
    val current = currentPaymentMethod.cleanAccountNameOrNull()
    val registeredMatch = current?.let { paymentMethod ->
        registered.firstOrNull { accountName -> moneyNamesMatch(accountName, paymentMethod) }
    }
    return when {
        current == null -> registered
        registeredMatch != null -> registered
        else -> listOf(current) + registered
    }
}

fun accountLabelForEdit(
    paymentMethod: String?,
    accountNames: List<String>
): String? {
    val current = paymentMethod.cleanAccountNameOrNull()
    val registered = accountNames
        .mapNotNull { it.cleanAccountNameOrNull() }
        .distinctBy { it.canonicalMoneyNameKey() }
    return when {
        current == null -> registered.firstOrNull()
        else -> registered.firstOrNull { moneyNamesMatch(it, current) } ?: current
    }
}

private fun String?.cleanAccountNameOrNull(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }
