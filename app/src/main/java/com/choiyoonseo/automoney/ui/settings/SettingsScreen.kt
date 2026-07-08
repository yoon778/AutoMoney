package com.choiyoonseo.automoney.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.notification.LastNotificationDiagnostic
import com.choiyoonseo.automoney.notification.NotificationDiagnosticResult
import com.choiyoonseo.automoney.ui.components.EmptyStateVisual
import com.choiyoonseo.automoney.ui.components.FinanceSectionCard
import com.choiyoonseo.automoney.ui.components.MoneyBlue
import com.choiyoonseo.automoney.ui.components.MoneyCanvas
import com.choiyoonseo.automoney.ui.components.MoneyCoral
import com.choiyoonseo.automoney.ui.components.ScreenTitle
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    onOpenNotificationSettings: () -> Unit = {},
    notificationAccessEnabled: Boolean? = null,
    lastNotificationDiagnostic: LastNotificationDiagnostic? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MoneyCanvas)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle(
            title = "설정",
            subtitle = "자동 기록과 알림 진단 상태를 확인해요."
        )

        EmptyStateVisual(
            title = "알림으로 자동 기록",
            message = "허용된 금융 앱 알림을 읽어 거래 후보로 바꾸고 확인해요."
        )

        FinanceSectionCard(
            title = "알림 접근 권한",
            subtitle = "금융 앱 알림 자동 분석",
            accent = MoneyBlue,
            icon = Icons.Filled.Settings
        ) {
            Text(
                text = when (notificationAccessEnabled) {
                    true -> "권한 켜짐"
                    false -> "권한 꺼짐"
                    null -> "권한 상태 확인 필요"
                },
                fontWeight = FontWeight.Medium
            )
            Text(
                when (notificationAccessEnabled) {
                    true -> "허용된 금융 앱 알림이 들어오면 자동 기록 후보로 처리해요."
                    false -> "알림 접근 권한을 허용해야 결제/송금 알림을 읽을 수 있어요."
                    null -> "권한 설정에서 AutoMoney 알림 접근을 확인해 주세요."
                }
            )
            Button(
                onClick = onOpenNotificationSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("권한 설정 열기")
            }
        }

        FinanceSectionCard(
            title = "최근 알림 결과",
            subtitle = "마지막 처리 상태",
            accent = MoneyCoral,
            icon = Icons.Filled.CheckCircle
        ) {
            if (lastNotificationDiagnostic == null) {
                Text("아직 처리한 금융 앱 알림이 없어요.")
                Text("허용된 금융 앱 알림이 들어오면 여기에서 마지막 결과가 표시돼요.")
            } else {
                Text(lastNotificationDiagnostic.result.toDisplayText(), fontWeight = FontWeight.Medium)
                Text("처리 ${lastNotificationDiagnostic.receivedAt.toDisplayTime()}")
                Text("Source ${lastNotificationDiagnostic.packageName}")
                Text(lastNotificationDiagnostic.title ?: "제목 없음")
                Text(lastNotificationDiagnostic.textPreview)
                lastNotificationDiagnostic.parsedType?.let {
                    Text("Type $it")
                }
                lastNotificationDiagnostic.message?.let { Text(it) }
            }
        }
    }
}

private fun NotificationDiagnosticResult.toDisplayText(): String =
    when (this) {
        NotificationDiagnosticResult.SAVED -> "저장됨"
        NotificationDiagnosticResult.DUPLICATE -> "중복"
        NotificationDiagnosticResult.IGNORED -> "무시됨"
        NotificationDiagnosticResult.ERROR -> "오류"
    }

private fun Instant.toDisplayTime(): String =
    DIAGNOSTIC_TIME_FORMATTER.format(this)

private val DIAGNOSTIC_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일 HH:mm").withZone(ZoneId.of("Asia/Seoul"))
