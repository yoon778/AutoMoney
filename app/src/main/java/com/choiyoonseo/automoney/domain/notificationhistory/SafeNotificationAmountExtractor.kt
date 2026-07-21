package com.choiyoonseo.automoney.domain.notificationhistory

import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot

class SafeNotificationAmountExtractor {
    fun extract(snapshot: NotificationSnapshot): Long? {
        val candidates = snapshot.combinedText
            .lineSequence()
            .filter { line ->
                actionWords.any(line::contains) && balanceWords.none(line::contains)
            }
            .flatMap { line ->
                amountPattern.findAll(line).mapNotNull { match ->
                    match.groupValues[1].replace(",", "").toLongOrNull()
                }
            }
            .filter { it > 0 }
            .distinct()
            .toList()
        return candidates.singleOrNull()
    }
}

private val actionWords = listOf(
    "결제",
    "승인",
    "사용",
    "입금",
    "출금",
    "이체",
    "취소",
    "환불",
    "환급",
    "예수금"
)
private val balanceWords = listOf("잔액", "잔고")
private val amountPattern = Regex("([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\\s*원")
