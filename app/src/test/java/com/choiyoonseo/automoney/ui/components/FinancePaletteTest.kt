package com.choiyoonseo.automoney.ui.components

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FinancePaletteTest {
    @Test
    fun accountAccentUsesStableBrandColors() {
        assertThat(accountAccentForName("\uad6d\ubbfc\uc740\ud589 \ud1b5\uc7a5")).isEqualTo(MoneyYellow)
        assertThat(accountAccentForName("\ud1a0\uc2a4\ubc45\ud06c \ud1b5\uc7a5")).isEqualTo(MoneyBlue)
        assertThat(accountAccentForName("\uce74\uce74\uc624\ubc45\ud06c \ud1b5\uc7a5")).isEqualTo(MoneyYellow)
        assertThat(accountAccentForName("\ub124\uc774\ubc84\ud398\uc774")).isEqualTo(MoneyMint)
    }

    @Test
    fun categoryAccentUsesStableCategoryColors() {
        assertThat(categoryAccentForName("\uce74\ud398/\uac04\uc2dd")).isEqualTo(MoneyYellow)
        assertThat(categoryAccentForName("\uad50\ud1b5\ube44")).isEqualTo(MoneyBlue)
        assertThat(categoryAccentForName("\uae09\uc5ec")).isEqualTo(MoneyGreen)
    }

    @Test
    fun reviewAccentSeparatesReviewReasons() {
        assertThat(reviewAccentForLabel("\uc1a1\uae08")).isEqualTo(MoneyBlue)
        assertThat(reviewAccentForLabel("\ucda9\uc804")).isEqualTo(MoneyYellow)
        assertThat(reviewAccentForLabel("\uc911\ubcf5")).isEqualTo(MoneyCoral)
    }

    @Test
    fun unknownAccentIsStableAndFromAppPalette() {
        val first = accountAccentForName("custom account")
        val second = accountAccentForName("custom account")

        assertThat(first).isEqualTo(second)
        assertThat(first).isIn(listOf<Color>(MoneyBlue, MoneyMint, MoneyCoral, MoneyYellow, MoneyGreen))
    }
}
