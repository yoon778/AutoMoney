# Toss Parser Realistic Notifications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Toss notification parsing more reliable for common Korean notification text variations.

**Architecture:** Keep parsing inside `TossNotificationParser` and strengthen it with focused helpers for payment, topup, transfer, refund/cancel, and payment gateway detection. Tests drive each new behavior.

**Tech Stack:** Kotlin, JUnit, Truth, Gradle wrapper.

## Global Constraints

- General merchant card/pay payments should become `AUTO_CONFIRMED` expenses.
- Wallet/point topups should become `NEEDS_REVIEW` with `ReviewReason.WALLET_TOPUP`.
- Transfers should become `NEEDS_REVIEW` with `ReviewReason.TRANSFER_UNKNOWN`.
- Refunds/cancels should become `NEEDS_REVIEW` with `ReviewReason.REFUND_OR_CANCEL`.
- Payment gateways such as `KCP`, `NICE`, `KG이니시스`, and `토스페이먼츠` should become `NEEDS_REVIEW` with `ReviewReason.PAYMENT_GATEWAY`.
- Verification: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Add Realistic Parser Tests

**Files:**
- Modify: `app/src/test/java/com/choiyoonseo/automoney/domain/parser/TossNotificationParserTest.kt`

- [x] Add tests for Toss Pay style payment text.
- [x] Add tests for payment gateway review routing.
- [x] Add tests for cancel/refund merchant extraction.
- [x] Add tests for wallet topup text variations.
- [x] Run parser tests and confirm RED for missing behavior.

### Task 2: Improve Parser Helpers

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/domain/parser/TossNotificationParser.kt`

- [x] Normalize merchant names after extraction.
- [x] Improve merchant extraction for lines where amount appears before action text.
- [x] Confirm existing gateway detection covers gateway merchant text.
- [x] Confirm existing wallet name extraction covers point/money variants.
- [x] Run parser tests and confirm GREEN.

### Task 3: Verification

- [x] Run full unit tests and debug build.
- [x] Update this plan checklist.
