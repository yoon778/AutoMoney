package com.choiyoonseo.automoney.ui.transactions

import android.content.Context

interface WalletTopupNoticeStore {
    fun shouldShowNotice(): Boolean
    fun markNoticeSeen()
}

class SharedPreferencesWalletTopupNoticeStore(context: Context) : WalletTopupNoticeStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        WALLET_TOPUP_NOTICE_PREFERENCES,
        Context.MODE_PRIVATE
    )

    override fun shouldShowNotice(): Boolean =
        shouldShowWalletTopupNotice(preferences.getBoolean(KEY_WALLET_TOPUP_NOTICE_SEEN, false))

    override fun markNoticeSeen() {
        preferences.edit().putBoolean(KEY_WALLET_TOPUP_NOTICE_SEEN, true).apply()
    }
}

internal fun shouldShowWalletTopupNotice(hasSeenNotice: Boolean): Boolean =
    !hasSeenNotice

private const val WALLET_TOPUP_NOTICE_PREFERENCES = "wallet_topup_notice"
private const val KEY_WALLET_TOPUP_NOTICE_SEEN = "wallet_topup_notice_seen"
