package com.choiyoonseo.automoney.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choiyoonseo.automoney.R
import com.choiyoonseo.automoney.ui.model.CategorySpendUi
import com.choiyoonseo.automoney.ui.model.MetricTileUi
import com.choiyoonseo.automoney.ui.model.ReviewCardUi
import com.choiyoonseo.automoney.ui.model.SourceAppUi
import com.choiyoonseo.automoney.ui.model.TransactionRowUi
import com.choiyoonseo.automoney.ui.model.formatWon

val MoneyBlue = Color(0xFF2F80ED)
val MoneyGreen = Color(0xFF24A148)
val MoneyCoral = Color(0xFFFF8A65)
val MoneyInk = Color(0xFF172033)
val MoneyMint = Color(0xFF19B59B)
val MoneyMuted = Color(0xFF697386)
val MoneyCanvas = Color(0xFFF7F9FC)
val MoneySoftBlue = Color(0xFFEAF2FF)
val MoneySoftMint = Color(0xFFEAF8F4)
val MoneySoftCoral = Color(0xFFFFF0EA)

@Composable
fun ScreenTitle(title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MoneyInk
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MoneyHeroCard(
    title: String,
    amount: String,
    helper: String,
    @DrawableRes imageRes: Int = R.drawable.illustration_cash_flow,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick == null) {
        modifier.fillMaxWidth()
    } else {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    }

    ElevatedCard(
        modifier = cardModifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFEAF2FF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MoneyInk)
                Text(
                    amount,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MoneyInk
                )
                Text(
                    helper,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.size(width = 132.dp, height = 92.dp)
            )
        }
    }
}

@Composable
fun MoneyFlowHeroCard(
    title: String,
    primaryValue: String,
    helper: String,
    reviewCount: Int,
    spentLabel: String,
    savedLabel: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick == null) {
        modifier.fillMaxWidth()
    } else {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    }

    Surface(
        modifier = cardModifier,
        shape = RoundedCornerShape(8.dp),
        color = MoneySoftBlue
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MoneyInk)
                    Text(
                        primaryValue,
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MoneyInk
                    )
                    Text(
                        helper,
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MoneyMuted
                    )
                }
                Image(
                    painter = painterResource(R.drawable.illustration_wallet_coins),
                    contentDescription = null,
                    modifier = Modifier.size(width = 120.dp, height = 88.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowPill("지출", spentLabel, MoneyCoral, Modifier.weight(1f))
                FlowPill("저축", savedLabel, MoneyMint, Modifier.weight(1f))
                FlowPill("검토", "${reviewCount}건", MoneyBlue, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun IllustratedSummaryCard(
    title: String,
    value: String,
    helper: String,
    accent: Color,
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick == null) {
        modifier.fillMaxWidth()
    } else {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    }

    Surface(
        modifier = cardModifier,
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MoneyInk)
                Text(
                    value,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MoneyInk
                )
                Text(
                    helper,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoneyMuted
                )
            }
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.size(width = 128.dp, height = 92.dp)
            )
        }
    }
}

@Composable
fun MonthlyFlowCardOld(
    title: String,
    period: String,
    remainingValue: String,
    incomeValue: String,
    expenseValue: String,
    savingsValue: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val flowSteps = homeFlowStepVisuals(incomeValue, expenseValue, savingsValue)
    val cardModifier = if (onClick == null) {
        modifier.fillMaxWidth()
    } else {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    }

    Surface(
        modifier = cardModifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("$title  ›", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(period, color = MoneyMuted, modifier = Modifier.padding(top = 4.dp))
                }
                Image(
                    painter = painterResource(R.drawable.illustration_wallet_coins),
                    contentDescription = null,
                    modifier = Modifier.size(width = 118.dp, height = 82.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MoneySoftBlue
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("남은 돈", color = MoneyInk, fontWeight = FontWeight.Medium)
                    Text(
                        remainingValue,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        flowSteps.forEachIndexed { index, step ->
                            FlowStep(step, Modifier.weight(1f))
                            if (index < flowSteps.lastIndex) {
                                FlowConnector()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyFlowCard(
    title: String,
    period: String,
    remainingValue: String,
    incomeValue: String,
    expenseValue: String,
    savingsValue: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val flowSteps = homeFlowStepVisuals(incomeValue, expenseValue, savingsValue)
    val cardModifier = if (onClick == null) {
        modifier.fillMaxWidth()
    } else {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    }

    Surface(
        modifier = cardModifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("$title  >", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(period, color = MoneyMuted, modifier = Modifier.padding(top = 4.dp))
                }
                Image(
                    painter = painterResource(R.drawable.illustration_wallet_coins),
                    contentDescription = null,
                    modifier = Modifier.size(width = 118.dp, height = 82.dp)
                )
            }
            Text("\ub0a8\uc740 \ub3c8", color = MoneyInk, fontWeight = FontWeight.Medium)
            Text(
                remainingValue,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                flowSteps.forEachIndexed { index, step ->
                    FlowStep(step, Modifier.weight(1f))
                    if (index < flowSteps.lastIndex) {
                        FlowConnector()
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowStep(step: HomeFlowStepVisual, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(step.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(step.imageRes),
                contentDescription = null,
                modifier = Modifier.size(50.dp)
            )
        }
        Text(step.label, color = MoneyInk, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        Text(
            step.value,
            color = MoneyInk,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FlowConnector() {
    Box(
        modifier = Modifier
            .width(14.dp)
            .height(2.dp)
            .clip(RoundedCornerShape(50))
            .background(MoneyMuted.copy(alpha = 0.22f))
    )
}

@Composable
private fun FlowPill(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.72f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MoneyMuted)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FinanceSectionCard(
    title: String,
    subtitle: String? = null,
    accent: Color = MoneyBlue,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick == null) {
        modifier.fillMaxWidth()
    } else {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    }

    Surface(
        modifier = cardModifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon == null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MoneyMuted)
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun MetricTile(
    metric: MetricTileUi,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val tileModifier = if (onClick == null) {
        modifier
    } else {
        modifier.clickable(onClick = onClick)
    }
    Surface(
        modifier = tileModifier
            .height(124.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(metric.title, style = MaterialTheme.typography.labelLarge)
            Text(metric.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { metric.normalizedProgress },
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                trackColor = accent.copy(alpha = 0.16f)
            )
            metric.helper?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TransactionRow(transaction: TransactionRowUi, onClick: (() -> Unit)? = null) {
    val accent = categoryAccentForName(transaction.category)
    val rowModifier = if (onClick == null) {
        Modifier
            .fillMaxWidth()
            .alpha(if (transaction.isExcluded) 0.48f else 1f)
            .padding(vertical = 10.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .alpha(if (transaction.isExcluded) 0.48f else 1f)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(text = transaction.iconText, color = softAccentColor(accent), textColor = accent)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.merchant, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${transaction.category} · ${transaction.method}",
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                transaction.sourceApp?.let { sourceApp ->
                    SourceAppBadge(sourceApp)
                }
            }
        }
        Text(
            formatWon(transaction.amountWon),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (transaction.amountWon < 0) Color(0xFFD64545) else MoneyGreen
        )
    }
}

@Composable
fun ReviewActionCard(
    card: ReviewCardUi,
    modifier: Modifier = Modifier,
    onPrimaryAction: () -> Unit = {},
    onSecondaryAction: () -> Unit = {},
    onAccountTransferAction: (() -> Unit)? = null,
    onEditAction: (() -> Unit)? = null
) {
    val accent = reviewAccentForLabel(card.tag)
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = accent.copy(alpha = 0.10f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(text = card.iconText, color = accent.copy(alpha = 0.14f), textColor = accent)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(card.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(card.tag, style = MaterialTheme.typography.labelMedium, color = accent)
                        card.sourceApp?.let { sourceApp ->
                            SourceAppBadge(sourceApp)
                        }
                    }
                }
                Text(formatWon(card.amountWon), fontWeight = FontWeight.Bold)
            }
            Text(card.message, style = MaterialTheme.typography.bodyMedium)
            card.detailLines.forEach { detail ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.08f)
                ) {
                    Text(
                        detail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MoneyInk
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onPrimaryAction, modifier = Modifier.weight(1f)) {
                    Text(card.primaryAction, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(onClick = onSecondaryAction, modifier = Modifier.weight(1f)) {
                    Text(card.secondaryAction, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (onAccountTransferAction != null) {
                OutlinedButton(onClick = onAccountTransferAction, modifier = Modifier.fillMaxWidth()) {
                    Text("내 계좌 이동", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (card.editAction != null && onEditAction != null) {
                OutlinedButton(onClick = onEditAction, modifier = Modifier.fillMaxWidth()) {
                    Text(card.editAction, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun CategoryBar(category: CategorySpendUi, color: Color, onClick: (() -> Unit)? = null) {
    val rowModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(onClick = onClick)
    }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
            Text(category.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text("${formatWon(category.amountWon)} · ${category.percentText}", style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { category.ratio.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = color,
            trackColor = color.copy(alpha = 0.14f)
        )
    }
}

@Composable
fun EmptyStateVisual(
    title: String,
    message: String,
    @DrawableRes imageRes: Int = R.drawable.illustration_notification_flow
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF7F9FC)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.size(width = 180.dp, height = 120.dp)
            )
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun IconBadge(text: String, color: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SourceAppBadge(sourceApp: SourceAppUi, modifier: Modifier = Modifier) {
    val accent = accountAccentForName(sourceApp.displayName)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(start = 5.dp, top = 3.dp, end = 8.dp, bottom = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sourceApp.badgeText,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Text(
                text = sourceApp.displayName,
                color = accent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
