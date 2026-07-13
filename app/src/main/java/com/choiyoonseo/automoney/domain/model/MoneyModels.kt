package com.choiyoonseo.automoney.domain.model

import com.choiyoonseo.automoney.domain.assets.BalanceImpact
import java.time.Instant
import java.time.YearMonth

@JvmInline
value class MoneyAmount(val won: Long) {
    init {
        require(won >= 0) { "MoneyAmount must be zero or positive" }
    }
}

enum class TransactionDirection {
    INCOME,
    EXPENSE,
    NEUTRAL
}

enum class TransactionType(
    val defaultDirection: TransactionDirection,
    val countsAsMonthlyExpense: Boolean
) {
    INCOME(TransactionDirection.INCOME, false),
    EXPENSE(TransactionDirection.EXPENSE, true),
    FIXED_EXPENSE(TransactionDirection.EXPENSE, true),
    SAVING(TransactionDirection.EXPENSE, false),
    INVESTMENT(TransactionDirection.EXPENSE, false),
    TRANSFER(TransactionDirection.NEUTRAL, false),
    WALLET_TOPUP(TransactionDirection.NEUTRAL, false),
    WALLET_SPEND(TransactionDirection.EXPENSE, true),
    SETTLEMENT(TransactionDirection.NEUTRAL, false),
    REFUND(TransactionDirection.NEUTRAL, false),
    EXCLUDED(TransactionDirection.NEUTRAL, false)
}

enum class TransactionStatus {
    AUTO_CONFIRMED,
    NEEDS_REVIEW,
    USER_EDITED,
    EXCLUDED
}

enum class SourceType {
    NOTIFICATION,
    MANUAL,
    IMPORT
}

enum class Category(val displayName: String) {
    FOOD("식비"),
    CAFE_SNACK("카페/간식"),
    TRANSPORT("교통비"),
    SHOPPING("쇼핑"),
    LIVING("생활"),
    HOBBY("취미/여가"),
    BEAUTY("미용"),
    HEALTH("의료/건강"),
    STUDY("학업"),
    EVENT("이벤트"),
    TELECOM("통신"),
    SUBSCRIPTION("구독"),
    SAVING("저축"),
    STOCK("주식"),
    COIN("코인"),
    SALARY("월급"),
    ALLOWANCE("용돈"),
    INVESTMENT_RETURN("투자성과"),
    DISCOUNT("할인"),
    REIMBURSEMENT("환급"),
    OTHER("기타")
}

data class SettlementDetails(
    val peopleCount: Int,
    val myShareWon: Long,
    val receivableHidden: Boolean = false
)

data class MoneyTransaction(
    val id: Long = 0,
    val occurredAt: Instant,
    val amount: MoneyAmount,
    val direction: TransactionDirection,
    val type: TransactionType,
    val category: Category?,
    val paymentMethod: String?,
    val merchant: String?,
    val counterparty: String?,
    val memo: String?,
    val sourceApp: String?,
    val sourceType: SourceType,
    val sourceNotificationHash: String?,
    val status: TransactionStatus,
    val confidence: Double,
    val monthKey: YearMonth,
    val linkedAssetAccountId: Long? = null,
    val balanceImpact: BalanceImpact? = null,
    val customCategoryId: Long? = null,
    val customCategoryName: String? = null,
    val settlementPartyCount: Int? = null,
    val settlementMyShareWon: Long? = null,
    val settlementParentId: Long? = null,
    val settlementTrackingHidden: Boolean = false,
    // Compatibility for the pre-reset partial settlement work; storage normalizes these to the fields above
    val settlementDetails: SettlementDetails? = null,
    val recoveryOfSettlementTransactionId: Long? = null,
    val budgetPlanId: Long? = null
)

fun MoneyTransaction.categoryDisplayName(): String? =
    customCategoryName?.trim()?.takeIf(String::isNotBlank) ?: category?.displayName

data class OpenReviewItem(
    val id: Long,
    val transaction: MoneyTransaction,
    val reason: ReviewReason,
    val createdAt: Instant
)

enum class ReviewReason {
    TRANSFER_UNKNOWN,
    INCOME_UNKNOWN,
    WALLET_TOPUP,
    REFUND_OR_CANCEL,
    DUPLICATE_SUSPECTED,
    LOW_CONFIDENCE_CATEGORY,
    PAYMENT_GATEWAY,
    ACCOUNT_UNMATCHED,
    ACCOUNT_AMBIGUOUS,
    ACCOUNT_MOVEMENT_UNKNOWN,
    BALANCE_MISMATCH
}

data class Rule(
    val id: Long = 0,
    val matchType: RuleMatchType,
    val matchValue: String,
    val action: RuleAction,
    val targetValue: String,
    val enabled: Boolean = true
)

enum class RuleMatchType {
    MERCHANT,
    COUNTERPARTY,
    NOTIFICATION_KEYWORD,
    PAYMENT_METHOD
}

enum class RuleAction {
    SET_CATEGORY,
    SET_TRANSACTION_TYPE,
    EXCLUDE,
    MARK_AS_WALLET_TOPUP,
    MARK_AS_SETTLEMENT
}
