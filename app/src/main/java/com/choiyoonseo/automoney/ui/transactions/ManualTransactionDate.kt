package com.choiyoonseo.automoney.ui.transactions

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

val manualTransactionZoneId: ZoneId = ZoneId.of("Asia/Seoul")

private val pickerZoneId: ZoneId = ZoneOffset.UTC
private val manualTransactionDateFormatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)

fun LocalDate.toManualTransactionInstant(): Instant =
    atStartOfDay(manualTransactionZoneId).toInstant()

fun LocalDate.toManualTransactionDateLabel(): String =
    format(manualTransactionDateFormatter)

fun LocalDate.toDatePickerMillis(): Long =
    atStartOfDay(pickerZoneId).toInstant().toEpochMilli()

fun Long.toDatePickerLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(pickerZoneId).toLocalDate()
