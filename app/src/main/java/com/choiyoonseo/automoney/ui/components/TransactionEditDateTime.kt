package com.choiyoonseo.automoney.ui.components

import com.choiyoonseo.automoney.domain.time.AppDateZoneId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

val transactionEditZoneId: ZoneId = AppDateZoneId

private val transactionEditPickerZoneId: ZoneId = ZoneOffset.UTC
private val transactionEditDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val transactionEditTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun Instant.toTransactionEditLocalDate(): LocalDate =
    atZone(transactionEditZoneId).toLocalDate()

fun Instant.toTransactionEditLocalTime(): LocalTime =
    atZone(transactionEditZoneId).toLocalTime().withSecond(0).withNano(0)

fun LocalDate.toTransactionEditInstant(time: LocalTime): Instant =
    atTime(time.withSecond(0).withNano(0)).atZone(transactionEditZoneId).toInstant()

fun LocalDate.toTransactionEditDateText(): String =
    format(transactionEditDateFormatter)

fun LocalTime.toTransactionEditTimeText(): String =
    withSecond(0).withNano(0).format(transactionEditTimeFormatter)

fun LocalDate.toTransactionEditDatePickerMillis(): Long =
    atStartOfDay(transactionEditPickerZoneId).toInstant().toEpochMilli()

fun Long.toTransactionEditDatePickerLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(transactionEditPickerZoneId).toLocalDate()
