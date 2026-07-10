package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import com.choiyoonseo.automoney.domain.parser.TossNotificationParser
import java.time.Instant

data class SampleNotificationScenario(
    val id: String,
    val packageName: String,
    val notificationKey: String,
    val label: String,
    val description: String,
    val notificationTitle: String,
    val text: String,
    val bigText: String? = null
)

fun SampleNotificationScenario.toSnapshot(postedAt: Instant = Instant.now()): NotificationSnapshot =
    NotificationSnapshot(
        packageName = packageName,
        title = notificationTitle,
        text = text,
        bigText = bigText,
        postedAt = postedAt,
        notificationKey = notificationKey
    )

val sampleNotificationScenarios = listOf(
    SampleNotificationScenario(
        id = "card_payment",
        packageName = TossNotificationParser.TOSS_PACKAGE,
        notificationKey = "sample-card-payment",
        label = "카드 결제",
        description = "자동 확정 지출로 들어가요",
        notificationTitle = "토스뱅크 체크카드",
        text = "스타벅스 홍대입구 6,100원 결제",
        bigText = "스타벅스 홍대입구 6,100원 결제\n06.27 12:47"
    ),
    SampleNotificationScenario(
        id = "wallet_topup",
        packageName = TossNotificationParser.TOSS_PACKAGE,
        notificationKey = "sample-wallet-topup",
        label = "포인트 충전",
        description = "검토 탭에서 실제 사용액을 입력해요",
        notificationTitle = "토스",
        text = "네이버페이머니에 10,000원 충전됐어요"
    ),
    SampleNotificationScenario(
        id = "transfer",
        packageName = TossNotificationParser.TOSS_PACKAGE,
        notificationKey = "sample-transfer",
        label = "친구 송금",
        description = "N분의1/지출 제외 확인이 필요해요",
        notificationTitle = "토스",
        text = "김민수님에게 10,000원 송금했어요"
    ),
    SampleNotificationScenario(
        id = "refund_cancel",
        packageName = TossNotificationParser.TOSS_PACKAGE,
        notificationKey = "sample-refund-cancel",
        label = "결제 취소",
        description = "환불/취소 검토로 들어가요",
        notificationTitle = "토스뱅크 체크카드",
        text = "쿠팡 12,000원 결제 취소"
    ),
    SampleNotificationScenario(
        id = "payment_gateway",
        packageName = TossNotificationParser.TOSS_PACKAGE,
        notificationKey = "sample-payment-gateway",
        label = "결제대행사",
        description = "실제 사용처 확인이 필요해요",
        notificationTitle = "토스뱅크 체크카드",
        text = "KCP 35,000원 결제",
        bigText = "KCP 35,000원 결제\n온라인 결제"
    ),
    SampleNotificationScenario(
        id = "kb_account_withdrawal",
        packageName = "com.kbstar.kbbank",
        notificationKey = "sample-kb-withdrawal",
        label = "KB \uacc4\uc88c \ucd9c\uae08",
        description = "\ub4f1\ub85d \uacc4\uc88c\uac00 \ub9de\uc73c\uba74 \uc794\uc561\uc744 \ucc28\uac10\ud558\uace0 \uac70\ub798\ub97c \uac80\ud1a0\ud574\uc694",
        notificationTitle = "KB\uc2a4\ud0c0\ubc45\ud0b9",
        text = "\uacc4\uc88c 123-***-4567",
        bigText = "10,000\uc6d0 \ucd9c\uae08"
    ),
    SampleNotificationScenario(
        id = "kb_account_deposit",
        packageName = "com.kbstar.kbbank",
        notificationKey = "sample-kb-deposit",
        label = "KB \uacc4\uc88c \uc785\uae08",
        description = "\ub4f1\ub85d \uacc4\uc88c\uac00 \ub9de\uc73c\uba74 \uc794\uc561\uc744 \uc99d\uac00\uc2dc\ud0a4\uace0 \uac70\ub798\ub97c \uac80\ud1a0\ud574\uc694",
        notificationTitle = "KB\uc2a4\ud0c0\ubc45\ud0b9",
        text = "\uacc4\uc88c 123-***-4567",
        bigText = "20,000\uc6d0 \uc785\uae08"
    ),
    SampleNotificationScenario(
        id = "kb_account_transfer",
        packageName = "com.kbstar.kbbank",
        notificationKey = "sample-kb-transfer",
        label = "KB \uacc4\uc88c \uc774\uccb4",
        description = "\ub4f1\ub85d \uacc4\uc88c\uac00 \ub9de\uc73c\uba74 \uc2dc\uc791\ud55c \uacc4\uc88c \uc794\uc561\uc744 \ucc28\uac10\ud558\uace0 \uc774\uccb4\ub97c \uac80\ud1a0\ud574\uc694",
        notificationTitle = "KB\uc2a4\ud0c0\ubc45\ud0b9",
        text = "\uacc4\uc88c 123-***-4567",
        bigText = "30,000\uc6d0 \uc1a1\uae08"
    )
)
