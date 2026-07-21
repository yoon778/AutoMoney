package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.model.TransactionType
import java.security.MessageDigest

fun notificationIdentityHash(snapshot: NotificationSnapshot): String {
    val identity = snapshot.notificationKey
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let { key ->
            listOf(snapshot.packageName, key, snapshot.postedAt.toEpochMilli().toString())
                .joinToString("|")
        }
        ?: listOf(
            snapshot.packageName,
            snapshot.postedAt.toEpochMilli().toString(),
            sha256(snapshot.combinedText)
        ).joinToString("|")

    return sha256(identity)
}

fun notificationEventIdentityHash(
    snapshot: NotificationSnapshot,
    draft: TransactionDraft
): String = sha256(
    listOf(
        notificationIdentityHash(snapshot),
        draft.type.name,
        draft.direction.name,
        draft.amount.won.toString(),
        sha256(snapshot.financialEventDiscriminator(draft))
    ).joinToString("|")
)

private fun NotificationSnapshot.financialEventDiscriminator(draft: TransactionDraft): String {
    val keywords = when (draft.type) {
        TransactionType.REFUND -> listOf("취소", "환불")
        TransactionType.INCOME -> listOf("입금", "받음", "받았")
        TransactionType.TRANSFER -> listOf("이체", "송금", "출금", "ATM")
        TransactionType.EXPENSE,
        TransactionType.FIXED_EXPENSE,
        TransactionType.WALLET_SPEND -> listOf("결제", "승인", "사용", "출금")
        TransactionType.WALLET_TOPUP -> listOf("충전")
        else -> emptyList()
    }
    val eventLine = sequenceOf(text, bigText, title)
        .filterNotNull()
        .flatMap { it.lineSequence() }
        .map(String::trim)
        .firstOrNull { line ->
            keywords.any { line.contains(it, ignoreCase = true) } &&
                EVENT_AMOUNT_REGEX.findAll(line).any { match ->
                    match.groupValues[1].replace(",", "").toLongOrNull() == draft.amount.won
                }
        }
        ?: combinedText

    var normalized = eventLine.lowercase()
    if (draft.type in EXPENSE_TYPES) {
        normalized = SECONDARY_DETAIL_REGEX.replace(normalized, "")
    }
    normalized = BALANCE_DETAIL_REGEX.replace(normalized, "")
    normalized = EVENT_UPDATE_NOISE.fold(normalized) { value, noise ->
        value.replace(noise, "", ignoreCase = true)
    }
    normalized = EVENT_AMOUNT_REGEX.replace(normalized, "")
    normalized = EVENT_ACTION_KEYWORDS.fold(normalized) { value, keyword ->
        value.replace(keyword, "", ignoreCase = true)
    }
    return normalized.replace(EVENT_PUNCTUATION_REGEX, " ")
        .replace(WHITESPACE_REGEX, " ")
        .trim()
}

private val EVENT_AMOUNT_REGEX = Regex("""([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\s*원""")
private val EVENT_UPDATE_NOISE = listOf("승인완료", "처리완료", "완료", "처리됨", "됐어요", "되었습니다")
private val EVENT_ACTION_KEYWORDS = listOf(
    "결제", "승인", "사용", "취소", "환불", "입금", "받음", "받았", "이체", "송금", "출금", "ATM", "충전"
)
private val EXPENSE_TYPES = setOf(
    TransactionType.EXPENSE,
    TransactionType.FIXED_EXPENSE,
    TransactionType.WALLET_SPEND
)
private val SECONDARY_DETAIL_REGEX = Regex("""(?:[/·,]\s*)?(?:캐시백|환급|적립).*$""")
private val BALANCE_DETAIL_REGEX = Regex("""(?:잔액|잔고)\s*[:：]?.*$""")
private val EVENT_PUNCTUATION_REGEX = Regex("""[/·,:：\[\]()]+""")
private val WHITESPACE_REGEX = Regex("""\s+""")

private fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
