package com.choiyoonseo.automoney.domain.review

import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType

enum class ReviewResolution(val defaultMemo: String) {
    CONFIRM("사용자 확인"),
    SETTLEMENT("N분의1 정산으로 확인"),
    ACCOUNT_TRANSFER("내 계좌 이동으로 확인"),
    REFUND("환불/취소로 확인"),
    EXCLUDE("통계 제외")
}

fun MoneyTransaction.resolveReview(
    resolution: ReviewResolution,
    userMemo: String?
): MoneyTransaction {
    val resolved = when (resolution) {
        ReviewResolution.CONFIRM -> copy(
            status = TransactionStatus.USER_EDITED,
            confidence = 1.0
        )

        ReviewResolution.SETTLEMENT -> copy(
            direction = TransactionDirection.NEUTRAL,
            type = TransactionType.SETTLEMENT,
            status = TransactionStatus.USER_EDITED,
            confidence = 1.0
        )

        ReviewResolution.ACCOUNT_TRANSFER -> copy(
            direction = TransactionDirection.NEUTRAL,
            type = TransactionType.TRANSFER,
            status = TransactionStatus.USER_EDITED,
            confidence = 1.0
        )

        ReviewResolution.REFUND -> copy(
            direction = TransactionDirection.NEUTRAL,
            type = TransactionType.REFUND,
            status = TransactionStatus.USER_EDITED,
            confidence = 1.0
        )

        ReviewResolution.EXCLUDE -> copy(
            direction = TransactionDirection.NEUTRAL,
            type = TransactionType.EXCLUDED,
            category = null,
            status = TransactionStatus.EXCLUDED,
            confidence = 1.0
        )
    }

    return resolved.copy(
        memo = appendReviewMemo(
            current = memo,
            actionMemo = resolution.defaultMemo,
            userMemo = userMemo
        )
    )
}

private fun appendReviewMemo(
    current: String?,
    actionMemo: String,
    userMemo: String?
): String {
    val cleanUserMemo = userMemo?.trim()?.takeIf { it.isNotBlank() }
    return listOfNotNull(
        current?.takeIf { it.isNotBlank() },
        actionMemo,
        cleanUserMemo
    ).joinToString(" · ")
}
