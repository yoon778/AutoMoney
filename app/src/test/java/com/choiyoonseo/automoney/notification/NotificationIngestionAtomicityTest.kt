package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.data.repository.DuplicateNotificationException
import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.data.repository.NotificationSaveResult
import com.choiyoonseo.automoney.domain.assets.AccountMovementDirection
import com.choiyoonseo.automoney.domain.assets.BankAccountHint
import com.choiyoonseo.automoney.domain.assets.BankEventKind
import com.choiyoonseo.automoney.domain.assets.BankProvider
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.OpenReviewItem
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.Rule
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.domain.parser.GenericFinanceNotificationParser
import com.choiyoonseo.automoney.domain.parser.NotificationParser
import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import com.choiyoonseo.automoney.domain.parser.ParseResult
import com.choiyoonseo.automoney.domain.parser.TransactionDraft
import com.choiyoonseo.automoney.domain.rules.CategorizationEngine
import com.choiyoonseo.automoney.domain.rules.DuplicateDetector
import com.choiyoonseo.automoney.domain.refund.RefundMatchDecision
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NotificationIngestionAtomicityTest {
    @Test
    fun sameAndroidNotificationCanContainDistinctPaymentAndCashbackEvents() = runTest {
        val repository = RecordingMoneyRepository()
        val useCase = NotificationIngestionUseCase(
            parser = ThrowingParser,
            genericParser = GenericFinanceNotificationParser(),
            categorizationEngine = CategorizationEngine(),
            duplicateDetector = DuplicateDetector(),
            repository = repository
        )
        val postedAt = Instant.parse("2026-07-21T01:00:00Z")
        val payment = NotificationSnapshot(
            packageName = "com.kbankwith.smartbank",
            title = "케이뱅크",
            text = "스타벅스 6,000원 결제 완료",
            bigText = null,
            postedAt = postedAt,
            notificationKey = "shared-kbank-key"
        )
        val cashback = payment.copy(
            text = "캐시백 6원 입금",
            bigText = "스타벅스 6,000원 결제 완료"
        )

        val paymentResult = useCase.ingest(payment, NotificationSourceAccess.SELECTED_UNVERIFIED)
        val cashbackResult = useCase.ingest(cashback, NotificationSourceAccess.SELECTED_UNVERIFIED)

        assertThat(paymentResult).isEqualTo(
            IngestionResult.Saved(TransactionType.EXPENSE, ReviewReason.LOW_CONFIDENCE_CATEGORY, 1)
        )
        assertThat(cashbackResult).isEqualTo(
            IngestionResult.Saved(TransactionType.INCOME, ReviewReason.INCOME_UNKNOWN, 1)
        )
        assertThat(repository.savedTransactions.map { it.amount.won })
            .containsExactly(6_000L, 6L)
            .inOrder()
        assertThat(repository.savedTransactions.mapNotNull { it.sourceNotificationHash }.distinct())
            .hasSize(2)
    }

    @Test
    fun wordingUpdateForSameFinancialEventRemainsDuplicate() = runTest {
        val repository = RecordingMoneyRepository()
        val useCase = NotificationIngestionUseCase(
            parser = ThrowingParser,
            genericParser = GenericFinanceNotificationParser(),
            categorizationEngine = CategorizationEngine(),
            duplicateDetector = DuplicateDetector(),
            repository = repository
        )
        val first = NotificationSnapshot(
            packageName = "com.kbankwith.smartbank",
            title = "케이뱅크",
            text = "스타벅스 6,000원 결제",
            bigText = null,
            postedAt = Instant.parse("2026-07-21T01:00:00Z"),
            notificationKey = "shared-kbank-key"
        )
        val wordingUpdate = first.copy(text = "스타벅스 6,000원 결제 / 캐시백 6원 적립")

        assertThat(useCase.ingest(first, NotificationSourceAccess.SELECTED_UNVERIFIED))
            .isInstanceOf(IngestionResult.Saved::class.java)
        assertThat(useCase.ingest(wordingUpdate, NotificationSourceAccess.SELECTED_UNVERIFIED))
            .isEqualTo(IngestionResult.Duplicate(TransactionType.EXPENSE))
        assertThat(repository.savedTransactions).hasSize(1)
    }

    @Test
    fun sameAmountAndTypeAtDifferentMerchantsRemainDistinctEvents() = runTest {
        val repository = RecordingMoneyRepository()
        val useCase = NotificationIngestionUseCase(
            parser = ThrowingParser,
            genericParser = GenericFinanceNotificationParser(),
            categorizationEngine = CategorizationEngine(),
            duplicateDetector = DuplicateDetector(),
            repository = repository
        )
        val first = NotificationSnapshot(
            packageName = "com.kbankwith.smartbank",
            title = "케이뱅크",
            text = "스타벅스 6,000원 결제",
            bigText = null,
            postedAt = Instant.parse("2026-07-21T01:00:00Z"),
            notificationKey = "shared-kbank-key"
        )
        val second = first.copy(text = "편의점 6,000원 결제")

        assertThat(useCase.ingest(first, NotificationSourceAccess.SELECTED_UNVERIFIED))
            .isInstanceOf(IngestionResult.Saved::class.java)
        assertThat(useCase.ingest(second, NotificationSourceAccess.SELECTED_UNVERIFIED))
            .isInstanceOf(IngestionResult.Saved::class.java)
        assertThat(repository.savedTransactions.mapNotNull { it.sourceNotificationHash }.distinct())
            .hasSize(2)
    }

    @Test
    fun legacyNotificationHashStillBlocksSameFinancialEventAfterUpgrade() = runTest {
        val repository = RecordingMoneyRepository()
        val parser = GenericFinanceNotificationParser()
        val useCase = NotificationIngestionUseCase(
            parser = ThrowingParser,
            genericParser = parser,
            categorizationEngine = CategorizationEngine(),
            duplicateDetector = DuplicateDetector(),
            repository = repository
        )
        val notification = NotificationSnapshot(
            packageName = "com.kbankwith.smartbank",
            title = "케이뱅크",
            text = "스타벅스 6,000원 결제",
            bigText = null,
            postedAt = Instant.parse("2026-07-21T01:00:00Z"),
            notificationKey = "legacy-kbank-key"
        )
        val legacyDraft = (parser.parse(notification) as ParseResult.Parsed).draft
        repository.savedTransactions += legacyDraft.toLegacyTransaction()

        val result = useCase.ingest(notification, NotificationSourceAccess.SELECTED_UNVERIFIED)

        assertThat(result).isEqualTo(IngestionResult.Duplicate(TransactionType.EXPENSE))
        assertThat(repository.savedTransactions).hasSize(1)
    }

    @Test
    fun reviewTransactionsUseAtomicNotificationSave() = runTest {
        val repository = RecordingMoneyRepository()
        val useCase = NotificationIngestionUseCase(
            parser = StaticParser(reviewDraft().copy(bankAccountHint = accountHint())),
            genericParser = ThrowingParser,
            categorizationEngine = CategorizationEngine(),
            duplicateDetector = DuplicateDetector(),
            repository = repository
        )

        val result = useCase.ingest(snapshot(), NotificationSourceAccess.TRUSTED)

        assertThat(result).isInstanceOf(IngestionResult.Saved::class.java)
        assertThat(repository.saveNotificationCalls).isEqualTo(1)
        assertThat(repository.savedAccountHint).isEqualTo(accountHint())
        assertThat(repository.savedReviewReason).isEqualTo(ReviewReason.TRANSFER_UNKNOWN)
        assertThat(repository.saveWithReviewCalls).isEqualTo(0)
        assertThat(repository.saveTransactionCalls).isEqualTo(0)
        assertThat(repository.createReviewItemCalls).isEqualTo(0)
    }

    @Test
    fun duplicateInsertRaceReturnsDuplicateResult() = runTest {
        val repository = RecordingMoneyRepository(throwDuplicateOnSave = true)
        val useCase = NotificationIngestionUseCase(
            parser = StaticParser(expenseDraft()),
            genericParser = ThrowingParser,
            categorizationEngine = CategorizationEngine(),
            duplicateDetector = DuplicateDetector(),
            repository = repository
        )

        val result = useCase.ingest(snapshot(), NotificationSourceAccess.TRUSTED)

        assertThat(result).isEqualTo(IngestionResult.Duplicate(TransactionType.EXPENSE))
    }

    @Test
    fun lowConfidenceAutoConfirmedDraftUsesReviewSave() = runTest {
        val repository = RecordingMoneyRepository()
        val useCase = NotificationIngestionUseCase(
            parser = StaticParser(expenseDraft().copy(confidence = 0.45)),
            genericParser = ThrowingParser,
            categorizationEngine = CategorizationEngine(),
            duplicateDetector = DuplicateDetector(),
            repository = repository
        )

        val result = useCase.ingest(snapshot(), NotificationSourceAccess.TRUSTED)

        assertThat(result).isEqualTo(
            IngestionResult.Saved(TransactionType.EXPENSE, ReviewReason.LOW_CONFIDENCE_CATEGORY, 1)
        )
        assertThat(repository.saveNotificationCalls).isEqualTo(1)
        assertThat(repository.savedReviewReason).isEqualTo(ReviewReason.LOW_CONFIDENCE_CATEGORY)
        assertThat(repository.saveWithReviewCalls).isEqualTo(0)
        assertThat(repository.saveTransactionCalls).isEqualTo(0)
    }

    @Test
    fun ingestionReturnsRepositoryEffectiveReason() = runTest {
        val repository = RecordingMoneyRepository(
            notificationResult = NotificationSaveResult(
                transactionId = 1,
                reviewReason = ReviewReason.ACCOUNT_AMBIGUOUS
            )
        )
        val useCase = NotificationIngestionUseCase(
            parser = StaticParser(expenseDraft()),
            genericParser = ThrowingParser,
            categorizationEngine = CategorizationEngine(),
            duplicateDetector = DuplicateDetector(),
            repository = repository
        )

        val result = useCase.ingest(snapshot(), NotificationSourceAccess.TRUSTED)

        assertThat(result).isEqualTo(
            IngestionResult.Saved(TransactionType.EXPENSE, ReviewReason.ACCOUNT_AMBIGUOUS, 1)
        )
    }

    @Test
    fun unverifiedSourceForcesReviewAndRemovesRawDetails() = runTest {
        val repository = RecordingMoneyRepository()
        val useCase = NotificationIngestionUseCase(
            parser = StaticParser(expenseDraft()),
            genericParser = StaticParser(expenseDraft()),
            categorizationEngine = CategorizationEngine(),
            duplicateDetector = DuplicateDetector(),
            repository = repository
        )

        val result = useCase.ingest(snapshot(), NotificationSourceAccess.SELECTED_UNVERIFIED)

        assertThat(result).isEqualTo(
            IngestionResult.Saved(TransactionType.EXPENSE, ReviewReason.LOW_CONFIDENCE_CATEGORY, 1)
        )
        assertThat(repository.savedTransaction?.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(repository.savedTransaction?.merchant).isNull()
        assertThat(repository.savedTransaction?.counterparty).isNull()
        assertThat(repository.savedTransaction?.memo).isNull()
    }

    @Test
    fun blockedSourceDoesNotParseOrSave() = runTest {
        val repository = RecordingMoneyRepository()
        val useCase = NotificationIngestionUseCase(
            parser = ThrowingParser,
            genericParser = ThrowingParser,
            categorizationEngine = CategorizationEngine(),
            duplicateDetector = DuplicateDetector(),
            repository = repository
        )

        val result = useCase.ingest(snapshot(), NotificationSourceAccess.BLOCKED)

        assertThat(result).isEqualTo(IngestionResult.Ignored("blocked source"))
        assertThat(repository.saveNotificationCalls).isEqualTo(0)
    }

    @Test
    fun savedRefundRunsAutomaticLinkingAndClearsReviewReason() = runTest {
        val repository = RecordingMoneyRepository()
        val autoLinkedIds = mutableListOf<Long>()
        val useCase = NotificationIngestionUseCase(
            parser = StaticParser(refundDraft()),
            genericParser = ThrowingParser,
            categorizationEngine = CategorizationEngine(),
            duplicateDetector = DuplicateDetector(),
            repository = repository,
            refundAutoLink = { refundId ->
                autoLinkedIds += refundId
                RefundMatchDecision.Match(paymentId = 99)
            }
        )

        val result = useCase.ingest(snapshot(), NotificationSourceAccess.TRUSTED)

        assertThat(result).isEqualTo(IngestionResult.Saved(TransactionType.REFUND, null, 1))
        assertThat(autoLinkedIds).containsExactly(1L)
    }

    @Test
    fun automaticLinkFailureKeepsSavedRefundInReview() = runTest {
        val repository = RecordingMoneyRepository()
        val useCase = NotificationIngestionUseCase(
            parser = StaticParser(refundDraft()),
            genericParser = ThrowingParser,
            categorizationEngine = CategorizationEngine(),
            duplicateDetector = DuplicateDetector(),
            repository = repository,
            refundAutoLink = { error("link failed") }
        )

        val result = useCase.ingest(snapshot(), NotificationSourceAccess.TRUSTED)

        assertThat(result).isEqualTo(
            IngestionResult.Saved(TransactionType.REFUND, ReviewReason.REFUND_OR_CANCEL, 1)
        )
        assertThat(repository.savedTransactions).hasSize(1)
    }
}

private object ThrowingParser : NotificationParser {
    override fun canParse(snapshot: NotificationSnapshot): Boolean = error("must not parse")
    override fun parse(snapshot: NotificationSnapshot): ParseResult = error("must not parse")
}

private class StaticParser(
    private val draft: TransactionDraft
) : NotificationParser {
    override fun canParse(snapshot: NotificationSnapshot): Boolean = true
    override fun parse(snapshot: NotificationSnapshot): ParseResult = ParseResult.Parsed(draft)
}

private class RecordingMoneyRepository(
    private val throwDuplicateOnSave: Boolean = false,
    private val notificationResult: NotificationSaveResult? = null
) : MoneyRepository {
    var saveNotificationCalls = 0
    var saveWithReviewCalls = 0
    var saveTransactionCalls = 0
    var createReviewItemCalls = 0
    var savedAccountHint: BankAccountHint? = null
    var savedReviewReason: ReviewReason? = null
    var savedTransaction: MoneyTransaction? = null
    val savedTransactions = mutableListOf<MoneyTransaction>()

    override suspend fun recentNotificationTransactions(limit: Int): List<MoneyTransaction> =
        savedTransactions.takeLast(limit)
    override fun observeTransactionsForMonth(month: YearMonth): Flow<List<MoneyTransaction>> = flowOf(emptyList())
    override fun observeOpenReviewCount(): Flow<Int> = flowOf(0)
    override fun observeOpenReviewItems(): Flow<List<OpenReviewItem>> = flowOf(emptyList())
    override suspend fun enabledRules(): List<Rule> = emptyList()

    override suspend fun saveTransaction(transaction: MoneyTransaction): Long {
        saveTransactionCalls += 1
        if (throwDuplicateOnSave) {
            throw DuplicateNotificationException(transaction.sourceNotificationHash)
        }
        return 1
    }

    override suspend fun saveTransactionWithReview(
        transaction: MoneyTransaction,
        reason: ReviewReason
    ): Long {
        saveWithReviewCalls += 1
        return 1
    }

    override suspend fun saveNotificationTransaction(
        transaction: MoneyTransaction,
        accountHint: BankAccountHint?,
        reviewReason: ReviewReason?
    ): NotificationSaveResult {
        saveNotificationCalls += 1
        savedTransaction = transaction
        savedAccountHint = accountHint
        savedReviewReason = reviewReason
        if (throwDuplicateOnSave) {
            throw DuplicateNotificationException(transaction.sourceNotificationHash)
        }
        if (savedTransactions.any { it.sourceNotificationHash == transaction.sourceNotificationHash }) {
            throw DuplicateNotificationException(transaction.sourceNotificationHash)
        }
        savedTransactions += transaction
        return notificationResult ?: NotificationSaveResult(1, reviewReason)
    }

    override suspend fun updateTransaction(transaction: MoneyTransaction) = Unit
    override suspend fun deleteTransaction(transactionId: Long) = Unit

    override suspend fun createReviewItem(transactionId: Long, reason: ReviewReason) {
        createReviewItemCalls += 1
    }

    override suspend fun resolveReviewItem(reviewItemId: Long) = Unit
    override suspend fun saveRule(rule: Rule): Long = 1
}

private fun expenseDraft() = reviewDraft().copy(
    direction = TransactionDirection.EXPENSE,
    type = TransactionType.EXPENSE,
    merchant = "store",
    counterparty = null,
    memo = "store",
    status = TransactionStatus.AUTO_CONFIRMED,
    reviewReason = null
)

private fun reviewDraft() = TransactionDraft(
    occurredAt = Instant.parse("2026-07-08T01:00:00Z"),
    amount = MoneyAmount(10_000),
    direction = TransactionDirection.NEUTRAL,
    type = TransactionType.TRANSFER,
    category = null,
    paymentMethod = "토스",
    merchant = null,
    counterparty = "김민수",
    memo = "송금 목적 확인 필요",
    sourceApp = "viva.republica.toss",
    sourceNotificationHash = "atomic-review-hash",
    status = TransactionStatus.NEEDS_REVIEW,
    confidence = 0.75,
    monthKey = YearMonth.of(2026, 7),
    reviewReason = ReviewReason.TRANSFER_UNKNOWN
)

private fun snapshot() = NotificationSnapshot(
    packageName = "viva.republica.toss",
    title = "토스",
    text = "김민수님에게 10,000원 송금했어요",
    bigText = null,
    postedAt = Instant.parse("2026-07-08T01:00:00Z")
)

private fun accountHint() = BankAccountHint(
    provider = BankProvider.KB,
    accountLast4 = "1234",
    direction = AccountMovementDirection.DEBIT,
    eventKind = BankEventKind.WITHDRAWAL
)

private fun refundDraft() = reviewDraft().copy(
    direction = TransactionDirection.NEUTRAL,
    type = TransactionType.REFUND,
    amount = MoneyAmount(6),
    status = TransactionStatus.NEEDS_REVIEW,
    reviewReason = ReviewReason.REFUND_OR_CANCEL
)

private fun TransactionDraft.toLegacyTransaction() = MoneyTransaction(
    occurredAt = occurredAt,
    amount = amount,
    direction = direction,
    type = type,
    category = category,
    paymentMethod = paymentMethod,
    merchant = merchant,
    counterparty = counterparty,
    memo = memo,
    sourceApp = sourceApp,
    sourceType = SourceType.NOTIFICATION,
    sourceNotificationHash = sourceNotificationHash,
    status = status,
    confidence = confidence,
    monthKey = monthKey
)
