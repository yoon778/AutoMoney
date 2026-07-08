package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import com.choiyoonseo.automoney.domain.parser.TossNotificationParser
import java.time.Instant

data class SampleNotificationScenario(
    val id: String,
    val label: String,
    val description: String,
    val notificationTitle: String,
    val text: String,
    val bigText: String? = null
)

fun SampleNotificationScenario.toSnapshot(postedAt: Instant = Instant.now()): NotificationSnapshot =
    NotificationSnapshot(
        packageName = TossNotificationParser.TOSS_PACKAGE,
        title = notificationTitle,
        text = text,
        bigText = bigText,
        postedAt = postedAt
    )

val sampleNotificationScenarios = listOf(
    SampleNotificationScenario(
        id = "card_payment",
        label = "카드 결제",
        description = "자동 확정 지출로 들어가요",
        notificationTitle = "토스뱅크 체크카드",
        text = "스타벅스 홍대입구 6,100원 결제",
        bigText = "스타벅스 홍대입구 6,100원 결제\n06.27 12:47"
    ),
    SampleNotificationScenario(
        id = "wallet_topup",
        label = "포인트 충전",
        description = "검토 탭에서 실제 사용액을 입력해요",
        notificationTitle = "토스",
        text = "네이버페이머니에 10,000원 충전됐어요"
    ),
    SampleNotificationScenario(
        id = "transfer",
        label = "친구 송금",
        description = "N분의1/지출 제외 확인이 필요해요",
        notificationTitle = "토스",
        text = "김민수님에게 10,000원 송금했어요"
    ),
    SampleNotificationScenario(
        id = "refund_cancel",
        label = "결제 취소",
        description = "환불/취소 검토로 들어가요",
        notificationTitle = "토스뱅크 체크카드",
        text = "쿠팡 12,000원 결제 취소"
    ),
    SampleNotificationScenario(
        id = "payment_gateway",
        label = "결제대행사",
        description = "실제 사용처 확인이 필요해요",
        notificationTitle = "토스뱅크 체크카드",
        text = "KCP 35,000원 결제",
        bigText = "KCP 35,000원 결제\n온라인 결제"
    )
)
