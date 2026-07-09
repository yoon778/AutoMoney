package com.choiyoonseo.automoney.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.ui.home.HomeScreen
import com.choiyoonseo.automoney.ui.report.MonthlyReportScreen
import com.choiyoonseo.automoney.ui.review.ReviewScreen
import com.choiyoonseo.automoney.ui.settings.SettingsScreen
import com.choiyoonseo.automoney.ui.theme.AutoMoneyTheme
import com.choiyoonseo.automoney.ui.transactions.TransactionsScreen

@Preview(name = "앱 전체", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppRootPreview() {
    AutoMoneyTheme {
        AppRoot()
    }
}

@Preview(name = "홈", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomePreview() {
    AutoMoneyTheme {
        HomeScreen(PaddingValues(0.dp))
    }
}

@Preview(name = "거래", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TransactionsPreview() {
    AutoMoneyTheme {
        TransactionsScreen(PaddingValues(0.dp))
    }
}

@Preview(name = "거래 · 다크", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TransactionsDarkPreview() {
    AutoMoneyTheme(darkTheme = true) {
        TransactionsScreen(PaddingValues(0.dp))
    }
}

@Preview(name = "검토", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReviewPreview() {
    AutoMoneyTheme {
        ReviewScreen(PaddingValues(0.dp))
    }
}

@Preview(name = "보고서", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReportPreview() {
    AutoMoneyTheme {
        MonthlyReportScreen(PaddingValues(0.dp))
    }
}

@Preview(name = "설정", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsPreview() {
    AutoMoneyTheme {
        SettingsScreen(PaddingValues(0.dp))
    }
}

@Preview(name = "검토 · 다크", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReviewDarkPreview() {
    AutoMoneyTheme(darkTheme = true) {
        ReviewScreen(PaddingValues(0.dp))
    }
}

@Preview(name = "자산 · 다크", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AssetsDarkPreview() {
    AutoMoneyTheme(darkTheme = true) {
        com.choiyoonseo.automoney.ui.assets.AssetsScreen(PaddingValues(0.dp))
    }
}

@Preview(name = "보고서 · 다크", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReportDarkPreview() {
    AutoMoneyTheme(darkTheme = true) {
        MonthlyReportScreen(PaddingValues(0.dp))
    }
}

@Preview(name = "설정 · 다크", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsDarkPreview() {
    AutoMoneyTheme(darkTheme = true) {
        SettingsScreen(PaddingValues(0.dp))
    }
}

@Preview(name = "홈 · 다크", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeDarkPreview() {
    AutoMoneyTheme(darkTheme = true) {
        HomeScreen(PaddingValues(0.dp))
    }
}

@Preview(name = "앱 전체 · 다크", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppRootDarkPreview() {
    AutoMoneyTheme(darkTheme = true) {
        AppRoot()
    }
}
