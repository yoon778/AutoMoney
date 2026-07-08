package com.choiyoonseo.automoney.ui.model

import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.OpenReviewItem
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class ReviewItemMapperTest {

    @Test
    fun openWalletTopupReviewMapsToUsageInputCard() {
        val card = openReviewItemsToCards(
            listOf(
                OpenReviewItem(
                    id = 7,
                    transaction = transaction(
                        type = TransactionType.WALLET_TOPUP,
                        direction = TransactionDirection.NEUTRAL,
                        merchant = "네이버페이",
                        counterparty = null,
                        memo = "네이버페이 충전"
                    ),
                    reason = ReviewReason.WALLET_TOPUP,
                    createdAt = Instant.parse("2026-07-01T01:00:00Z")
                )
            )
        ).single()

        assertThat(card.id).isEqualTo("review-7")
        assertThat(card.reviewItemId).isEqualTo(7)
        assertThat(card.title).isEqualTo("네이버페이 충전")
        assertThat(card.kind).isEqualTo(ReviewCardKind.WALLET_TOPUP)
        assertThat(card.primaryAction).isEqualTo("사용액 입력")
        assertThat(card.secondaryAction).isEqualTo("아직 안 씀")
        assertThat(card.sourceTransaction?.type).isEqualTo(TransactionType.WALLET_TOPUP)
    }

    @Test
    fun openTransferReviewMapsToSplitCard() {
        val card = openReviewItemsToCards(
            listOf(
                OpenReviewItem(
                    id = 9,
                    transaction = transaction(
                        type = TransactionType.TRANSFER,
                        direction = TransactionDirection.NEUTRAL,
                        merchant = null,
                        counterparty = "김민수",
                        memo = "송금 목적 확인 필요"
                    ),
                    reason = ReviewReason.TRANSFER_UNKNOWN,
                    createdAt = Instant.parse("2026-07-01T01:00:00Z")
                )
            )
        ).single()

        assertThat(card.title).isEqualTo("김민수에게 송금")
        assertThat(card.kind).isEqualTo(ReviewCardKind.TRANSFER)
        assertThat(card.primaryAction).isEqualTo("N분의1")
        assertThat(card.secondaryAction).isEqualTo("내 지출 아님")
        assertThat(card.editAction).isEqualTo("수정")
    }

    @Test
    fun openTransferReviewShowsSourceAccountAndFallbackTitle() {
        val card = openReviewItemsToCards(
            listOf(
                OpenReviewItem(
                    id = 10,
                    transaction = transaction(
                        type = TransactionType.TRANSFER,
                        direction = TransactionDirection.NEUTRAL,
                        merchant = "",
                        counterparty = "",
                        memo = "\uce5c\uad6c \uc815\uc0b0",
                        paymentMethod = "KB"
                    ),
                    reason = ReviewReason.TRANSFER_UNKNOWN,
                    createdAt = Instant.parse("2026-07-01T01:00:00Z")
                )
            )
        ).single()

        assertThat(card.title).isEqualTo("\uce5c\uad6c \uc815\uc0b0")
        assertThat(card.detailLines).contains("\ucd9c\uae08 \uacc4\uc88c KB")
    }

    @Test
    fun openReviewItemAttachesSourceAppInfoFromNotificationPackage() {
        val card = openReviewItemsToCards(
            listOf(
                OpenReviewItem(
                    id = 12,
                    transaction = transaction(
                        type = TransactionType.TRANSFER,
                        direction = TransactionDirection.NEUTRAL,
                        merchant = "",
                        counterparty = "",
                        memo = "\uc1a1\uae08 \ud655\uc778",
                        paymentMethod = "KB",
                        sourceApp = "com.kbstar.kbbank"
                    ),
                    reason = ReviewReason.TRANSFER_UNKNOWN,
                    createdAt = Instant.parse("2026-07-01T01:00:00Z")
                )
            )
        ).single()

        assertThat(card.sourceApp).isEqualTo(
            SourceAppUi(
                packageName = "com.kbstar.kbbank",
                displayName = "\uad6d\ubbfc\uc740\ud589",
                badgeText = "KB"
            )
        )
    }

    @Test
    fun openIncomeReviewMapsToMemoCard() {
        val card = openReviewItemsToCards(
            listOf(
                OpenReviewItem(
                    id = 11,
                    transaction = transaction(
                        type = TransactionType.INCOME,
                        direction = TransactionDirection.INCOME,
                        merchant = null,
                        counterparty = "김민수",
                        memo = "입금 확인 필요"
                    ),
                    reason = ReviewReason.INCOME_UNKNOWN,
                    createdAt = Instant.parse("2026-07-01T01:00:00Z")
                )
            )
        ).single()

        assertThat(card.title).isEqualTo("김민수")
        assertThat(card.kind).isEqualTo(ReviewCardKind.OTHER)
        assertThat(card.primaryAction).isEqualTo("입금 메모")
        assertThat(card.secondaryAction).isEqualTo("제외")
    }

    private fun transaction(
        type: TransactionType,
        direction: TransactionDirection,
        merchant: String?,
        counterparty: String?,
        memo: String?,
        paymentMethod: String? = "Toss",
        sourceApp: String = "viva.republica.toss"
    ) = MoneyTransaction(
        id = 42,
        occurredAt = Instant.parse("2026-07-01T01:00:00Z"),
        amount = MoneyAmount(10000),
        direction = direction,
        type = type,
        category = null,
        paymentMethod = paymentMethod,
        merchant = merchant,
        counterparty = counterparty,
        memo = memo,
        sourceApp = sourceApp,
        sourceType = SourceType.NOTIFICATION,
        sourceNotificationHash = "hash",
        status = TransactionStatus.NEEDS_REVIEW,
        confidence = 0.8,
        monthKey = YearMonth.of(2026, 7)
    )
}
