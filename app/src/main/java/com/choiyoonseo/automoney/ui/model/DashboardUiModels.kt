package com.choiyoonseo.automoney.ui.model

import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.time.AppDateZoneId
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

data class MetricTileUi(
    val title: String,
    val value: String,
    val progress: Float? = null,
    val helper: String? = null
) {
    val normalizedProgress: Float
        get() = (progress ?: 0f).coerceIn(0f, 1f)
}

data class TransactionRowUi(
    val merchant: String,
    val category: String,
    val amountWon: Long,
    val method: String,
    val iconText: String,
    val id: Long? = null,
    val isExcluded: Boolean = false,
    val sourceApp: SourceAppUi? = null,
    val sourceLabel: String? = null
)

data class TransactionDateSectionUi(
    val date: LocalDate,
    val dateLabel: String,
    val rows: List<TransactionRowUi>
)

enum class ReviewCardKind {
    TRANSFER,
    WALLET_TOPUP,
    REFUND,
    OTHER
}

data class ReviewCardUi(
    val id: String,
    val title: String,
    val message: String,
    val amountWon: Long,
    val tag: String,
    val iconText: String,
    val primaryAction: String,
    val secondaryAction: String,
    val editAction: String? = null,
    val detailLines: List<String> = emptyList(),
    val kind: ReviewCardKind = ReviewCardKind.OTHER,
    val reviewItemId: Long? = null,
    val sourceTransaction: MoneyTransaction? = null,
    val sourceApp: SourceAppUi? = null
)

data class CategorySpendUi(
    val name: String,
    val amountWon: Long,
    val ratio: Float
) {
    val percentText: String = "${(ratio.coerceIn(0f, 1f) * 100).roundToInt()}%"
}

data class HomeSnapshot(
    val monthTitle: String,
    val netSavedWon: Long,
    val reviewCount: Int,
    val metrics: List<MetricTileUi>,
    val recentTransactions: List<TransactionRowUi>
)

data class DailySpendUi(
    val day: Int,
    val amountWon: Long,
    val label: String
)

data class MonthlySpendCalendarUi(
    val monthTitle: String,
    val daysInMonth: Int,
    val firstWeekdayOffset: Int,
    val dailySpends: List<DailySpendUi>,
    val yearMonth: YearMonth? = null
)

fun formatWon(amount: Long): String = "%,d원".format(amount)

fun dismissReviewCard(cards: List<ReviewCardUi>, cardId: String): List<ReviewCardUi> =
    cards.filterNot { it.id == cardId }

fun MonthlySpendCalendarUi.spendForDay(day: Int): DailySpendUi? =
    dailySpends.firstOrNull { it.day == day }

fun MonthlySpendCalendarUi.totalSpendWon(): Long =
    dailySpends.sumOf { it.amountWon }

fun MonthlySpendCalendarUi.defaultSelectedDay(today: LocalDate = LocalDate.now(AppDateZoneId)): Int {
    if (yearMonth == YearMonth.from(today)) {
        return today.dayOfMonth.coerceIn(1, daysInMonth)
    }
    return dailySpends.lastOrNull()?.day ?: 1
}

val sampleHomeSnapshot = HomeSnapshot(
    monthTitle = "6월 돈 흐름",
    netSavedWon = 342000,
    reviewCount = 5,
    metrics = listOf(
        MetricTileUi("이번 달 지출", formatWon(898000), 0.72f, "예산 72%"),
        MetricTileUi("저축률", "64%", 0.64f, "목표까지 19.8만원"),
        MetricTileUi("검토 필요", "5건", 0.5f, "확인 필요")
    ),
    recentTransactions = listOf(
        TransactionRowUi("스타벅스 홍대입구", "카페/간식", -6100, "체크카드", "커"),
        TransactionRowUi("GS25 합정역점", "식비", -4800, "토스페이", "식"),
        TransactionRowUi("쿠팡", "생활", -12900, "체크카드", "생")
    )
)

val sampleReviewCards = listOf(
    ReviewCardUi(
        id = "transfer-friend-split",
        title = "김민수에게 송금",
        message = "친구가 먼저 결제한 돈인지 확인이 필요해요.",
        amountWon = 10000,
        tag = "송금",
        iconText = "송",
        primaryAction = "수정",
        secondaryAction = "삭제",
        editAction = null,
        kind = ReviewCardKind.TRANSFER
    ),
    ReviewCardUi(
        id = "wallet-naverpay",
        title = "네이버페이 충전",
        message = "충전 알림만 있고 실제 결제 알림은 없을 수 있어요. 사용한 금액만 지출로 기록해요.",
        amountWon = 10000,
        tag = "충전",
        iconText = "충",
        primaryAction = "사용액 입력",
        secondaryAction = "아직 안 씀",
        editAction = "수정",
        detailLines = listOf(
            "충전액 1만원",
            "예: 6,000원 사용 입력 → 지출 6,000원",
            "남은 충전 잔액 4,000원 보류"
        ),
        kind = ReviewCardKind.WALLET_TOPUP
    )
)

val sampleCategorySpends = listOf(
    CategorySpendUi("식비", 168000, 0.82f),
    CategorySpendUi("카페/간식", 137000, 0.67f),
    CategorySpendUi("교통비", 92000, 0.45f),
    CategorySpendUi("생활", 71000, 0.34f)
)

val sampleSpendCalendar = MonthlySpendCalendarUi(
    monthTitle = "2026년 6월",
    daysInMonth = 30,
    firstWeekdayOffset = 1,
    dailySpends = listOf(
        DailySpendUi(1, 6100, "카페/간식"),
        DailySpendUi(3, 4800, "식비"),
        DailySpendUi(5, 12800, "생활"),
        DailySpendUi(7, 32000, "교통비"),
        DailySpendUi(9, 15400, "식비"),
        DailySpendUi(12, 59000, "쇼핑"),
        DailySpendUi(15, 42300, "카페/간식"),
        DailySpendUi(18, 8800, "생활"),
        DailySpendUi(21, 21900, "식비"),
        DailySpendUi(25, 36700, "교통비"),
        DailySpendUi(28, 46800, "쇼핑")
    ),
    yearMonth = YearMonth.of(2026, 6)
)
