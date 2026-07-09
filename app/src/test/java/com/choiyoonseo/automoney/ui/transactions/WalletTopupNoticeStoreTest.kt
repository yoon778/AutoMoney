package com.choiyoonseo.automoney.ui.transactions

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WalletTopupNoticeStoreTest {
    @Test
    fun walletTopupNoticeShowsUntilMarkedSeen() {
        assertThat(shouldShowWalletTopupNotice(hasSeenNotice = false)).isTrue()
        assertThat(shouldShowWalletTopupNotice(hasSeenNotice = true)).isFalse()
    }
}
