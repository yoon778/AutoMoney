package com.choiyoonseo.automoney.ui.model

import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.OpenReviewItem
import com.choiyoonseo.automoney.domain.model.ReviewReason

fun openReviewItemsToCards(items: List<OpenReviewItem>): List<ReviewCardUi> =
    items.map { item ->
        val transaction = item.transaction
        val accountLines = transaction.accountDetailLines()
        when (item.reason) {
            ReviewReason.WALLET_TOPUP -> ReviewCardUi(
                id = "review-${item.id}",
                title = transaction.walletTopupTitle(),
                message = "\ucda9\uc804 \uc54c\ub9bc\ub9cc \uc788\uace0 \uc2e4\uc81c \uacb0\uc81c \uc54c\ub9bc\uc740 \uc5c6\uc744 \uc218 \uc788\uc5b4\uc694. \uc0ac\uc6a9\ud55c \uae08\uc561\ub9cc \uc9c0\ucd9c\ub85c \uae30\ub85d\ud574\uc694.",
                amountWon = transaction.amount.won,
                tag = "\ucda9\uc804",
                iconText = "\ucda9",
                primaryAction = "\uc0ac\uc6a9\uc561 \uc785\ub825",
                secondaryAction = "\uc544\uc9c1 \uc548 \uc500",
                editAction = "\uc218\uc815",
                detailLines = accountLines + listOf(
                    "\ucda9\uc804\uc561 ${formatWon(transaction.amount.won)}",
                    "\uc0ac\uc6a9\uc561 \uc785\ub825 \uc2dc \uc2e4\uc81c \uc9c0\ucd9c\ub9cc \uae30\ub85d",
                    "\ub0a8\uc740 \ucda9\uc804 \uc794\uc561\uc740 \ubcf4\ub958"
                ),
                kind = ReviewCardKind.WALLET_TOPUP,
                reviewItemId = item.id,
                sourceTransaction = transaction,
                sourceApp = sourceAppUiForPackage(transaction.sourceApp)
            )

            ReviewReason.TRANSFER_UNKNOWN -> ReviewCardUi(
                id = "review-${item.id}",
                title = transaction.transferReviewTitle(),
                message = "\uce5c\uad6c\uac00 \uba3c\uc800 \uacb0\uc81c\ud55c \ub3c8\uc778\uc9c0, \uacc4\uc88c \uc774\ub3d9\uc778\uc9c0 \ud655\uc778\uc774 \ud544\uc694\ud574\uc694.",
                amountWon = transaction.amount.won,
                tag = "\uc1a1\uae08",
                iconText = "\uc1a1",
                primaryAction = "N\ubd84\uc7581",
                secondaryAction = "\ub0b4 \uc9c0\ucd9c \uc544\ub2d8",
                editAction = "\uc218\uc815",
                detailLines = accountLines,
                kind = ReviewCardKind.TRANSFER,
                reviewItemId = item.id,
                sourceTransaction = transaction,
                sourceApp = sourceAppUiForPackage(transaction.sourceApp)
            )

            ReviewReason.REFUND_OR_CANCEL -> ReviewCardUi(
                id = "review-${item.id}",
                title = "${transaction.reviewName("\ud658\ubd88")} \ud655\uc778",
                message = "\ud658\ubd88/\ucde8\uc18c \uae08\uc561\uc778\uc9c0 \ud655\uc778\uc774 \ud544\uc694\ud574\uc694.",
                amountWon = transaction.amount.won,
                tag = "\ud658\ubd88",
                iconText = "\ud658",
                primaryAction = "\ud658\ubd88 \ucc98\ub9ac",
                secondaryAction = "\ubcf4\ub958",
                editAction = "\uc218\uc815",
                detailLines = accountLines,
                kind = ReviewCardKind.REFUND,
                reviewItemId = item.id,
                sourceTransaction = transaction,
                sourceApp = sourceAppUiForPackage(transaction.sourceApp)
            )

            ReviewReason.INCOME_UNKNOWN -> ReviewCardUi(
                id = "review-${item.id}",
                title = transaction.reviewName("\uc785\uae08 \ud655\uc778"),
                message = "\uc785\uae08 \ub0b4\uc5ed\uc774 \ub9de\ub294\uc9c0 \ud655\uc778\ud574 \uc8fc\uc138\uc694.",
                amountWon = transaction.amount.won,
                tag = "\uc785\uae08",
                iconText = "\uc785",
                primaryAction = "\uc785\uae08 \ud655\uc778",
                secondaryAction = "\uc0ad\uc81c",
                editAction = "\uc218\uc815",
                detailLines = accountLines,
                kind = ReviewCardKind.OTHER,
                reviewItemId = item.id,
                sourceTransaction = transaction,
                sourceApp = sourceAppUiForPackage(transaction.sourceApp)
            )

            ReviewReason.ACCOUNT_UNMATCHED -> ReviewCardUi(
                id = "review-${item.id}",
                title = transaction.reviewName("\uacc4\uc88c \ud655\uc778"),
                message = "\uac70\ub798\ub294 \uc800\uc7a5\ub410\uc9c0\ub9cc \ucd9c\uae08 \uacc4\uc88c\ub97c \uc790\uc0b0 \ubaa9\ub85d\uc5d0\uc11c \ucc3e\uc9c0 \ubabb\ud588\uc5b4\uc694.",
                amountWon = transaction.amount.won,
                tag = "\uacc4\uc88c",
                iconText = "\uacc4",
                primaryAction = "\uacc4\uc88c \ud655\uc778",
                secondaryAction = "\uc81c\uc678",
                editAction = "\uc218\uc815",
                detailLines = accountLines,
                kind = ReviewCardKind.OTHER,
                reviewItemId = item.id,
                sourceTransaction = transaction,
                sourceApp = sourceAppUiForPackage(transaction.sourceApp)
            )

            ReviewReason.DUPLICATE_SUSPECTED,
            ReviewReason.LOW_CONFIDENCE_CATEGORY,
            ReviewReason.PAYMENT_GATEWAY,
            ReviewReason.ACCOUNT_AMBIGUOUS,
            ReviewReason.ACCOUNT_MOVEMENT_UNKNOWN,
            ReviewReason.BALANCE_MISMATCH -> ReviewCardUi(
                id = "review-${item.id}",
                title = transaction.reviewName("\uac70\ub798 \ud655\uc778"),
                message = "\uc790\ub3d9 \ubd84\ub958 \ud655\uc2e0\uc774 \ub0ae\uc544\uc11c \ud655\uc778\uc774 \ud544\uc694\ud574\uc694.",
                amountWon = transaction.amount.won,
                tag = "\ud655\uc778",
                iconText = "\ud655",
                primaryAction = "\ub0b4 \uc9c0\ucd9c",
                secondaryAction = "\uc81c\uc678",
                editAction = "\uc218\uc815",
                detailLines = accountLines,
                kind = ReviewCardKind.OTHER,
                reviewItemId = item.id,
                sourceTransaction = transaction,
                sourceApp = sourceAppUiForPackage(transaction.sourceApp)
            )
        }
    }

private fun MoneyTransaction.transferReviewTitle(): String {
    val person = counterparty.cleanOrNull() ?: merchant.cleanOrNull()
    return if (person != null) {
        "${person}\uc5d0\uac8c \uc1a1\uae08"
    } else {
        memo.cleanOrNull() ?: "\uc1a1\uae08 \ud655\uc778"
    }
}

private fun MoneyTransaction.walletTopupTitle(): String {
    val name = merchant.cleanOrNull()
        ?: memo.cleanOrNull()?.removeSuffix(" \ucda9\uc804")
        ?: "\ud398\uc774"
    return if (name.endsWith("\ucda9\uc804")) name else "$name \ucda9\uc804"
}

private fun MoneyTransaction.reviewName(default: String): String =
    merchant.cleanOrNull() ?: counterparty.cleanOrNull() ?: memo.cleanOrNull() ?: default

private fun MoneyTransaction.accountDetailLines(): List<String> =
    paymentMethod.cleanOrNull()?.let { listOf("\ucd9c\uae08 \uacc4\uc88c $it") } ?: emptyList()

private fun String?.cleanOrNull(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }
