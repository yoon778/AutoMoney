# Claude Transaction UI Handoff

Date: 2026-07-09

## Goal

Transactions tab should be list-first:
- Show confirmed ledger transactions in descending date/time order
- Group rows by local date
- Keep manual add hidden until the user taps the top-right `+`
- Do not expose "충전/포인트" as a transaction type

## Logic Contract

Use this mapper for the transaction list:

```kotlin
transactionsToDateSections(
    transactions: List<MoneyTransaction>,
    limit: Int = Int.MAX_VALUE,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<TransactionDateSectionUi>
```

Output model:

```kotlin
data class TransactionDateSectionUi(
    val date: LocalDate,
    val dateLabel: String,
    val rows: List<TransactionRowUi>
)
```

Rules already handled by the mapper:
- `occurredAt DESC`
- Local date grouping
- `TransactionStatus.NEEDS_REVIEW` hidden
- `TransactionType.WALLET_TOPUP` hidden
- Each row is already converted to `TransactionRowUi`

## Add Flow

Current state in `TransactionsScreen`:

```kotlin
var isManualFormVisible by remember { mutableStateOf(false) }
```

UI expectation:
- Top-right `+` sets `isManualFormVisible = true`
- Manual form is not shown by default
- Save success sets `isManualFormVisible = false`
- Close/cancel can set `isManualFormVisible = false`

## Wallet Topup Notice

One-time notice logic:

```kotlin
interface WalletTopupNoticeStore {
    fun shouldShowNotice(): Boolean
    fun markNoticeSeen()
}
```

Current default implementation:

```kotlin
SharedPreferencesWalletTopupNoticeStore
```

UI may show this once:

> 충전과 포인트 이동은 거래 목록에서 제외하고, 실제 사용 금액만 지출로 기록해요.

If the UI does not need the popup, it can ignore this store.

## Do Not Re-Add

Do not add this option back to edit type UI:

```kotlin
TransactionType.WALLET_TOPUP
```

`transactionEditTypeOptions` intentionally excludes it.

## Files To Read

- `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/TransactionsScreen.kt`
- `app/src/main/java/com/choiyoonseo/automoney/ui/model/MonthlySummaryMapper.kt`
- `app/src/main/java/com/choiyoonseo/automoney/ui/model/DashboardUiModels.kt`
- `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/WalletTopupNoticeStore.kt`
- `app/src/main/java/com/choiyoonseo/automoney/ui/components/TransactionEditTypeOptions.kt`

## Tests

- `MonthlySummaryMapperTest.transactionsToDateSectionsGroupsByLocalDateDescendingThenTimeDescending`
- `TransactionEditTypeOptionsTest.transactionEditTypeOptionsExposeUserFacingTypesWithoutWalletTopup`
- `WalletTopupNoticeStoreTest.walletTopupNoticeShowsUntilMarkedSeen`
