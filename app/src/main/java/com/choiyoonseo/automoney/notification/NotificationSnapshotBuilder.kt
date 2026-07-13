package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import java.time.Instant

data class NotificationContentFields(
    val packageName: String,
    val notificationKey: String? = null,
    val postTimeMillis: Long,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val textLines: List<String> = emptyList()
)

class NotificationSnapshotBuilder {
    fun build(fields: NotificationContentFields): NotificationSnapshot {
        val title = fields.title.clean(MAX_TITLE_LENGTH)
        val text = fields.text.clean(MAX_TEXT_LENGTH)
        val primaryLines = listOfNotNull(title, text)

        val expandedLines = sequence {
            fields.bigText?.lineSequence()?.forEach { yield(it) }
            fields.textLines.asSequence().take(MAX_EXPANDED_LINES).forEach { value ->
                value.lineSequence().forEach { yield(it) }
            }
        }
            .mapNotNull { it.clean(MAX_EXPANDED_LINE_LENGTH) }
            .filterNot { line -> primaryLines.contains(line) }
            .distinct()
            .take(MAX_EXPANDED_LINES)
            .toList()

        return NotificationSnapshot(
            packageName = fields.packageName,
            title = title,
            text = text,
            bigText = expandedLines.joinToBoundedText(MAX_EXPANDED_TEXT_LENGTH),
            postedAt = Instant.ofEpochMilli(fields.postTimeMillis),
            notificationKey = fields.notificationKey?.take(MAX_NOTIFICATION_KEY_LENGTH)
        )
    }

    private fun String?.clean(maxLength: Int): String? =
        this?.take(maxLength)?.trim()?.takeIf { it.isNotEmpty() }
}

private fun List<String>.joinToBoundedText(maxLength: Int): String? {
    val result = StringBuilder()
    forEach { line ->
        val separatorLength = if (result.isEmpty()) 0 else 1
        val remaining = maxLength - result.length - separatorLength
        if (remaining <= 0) return@forEach
        if (separatorLength == 1) result.append('\n')
        result.append(line.take(remaining))
    }
    return result.toString().ifBlank { null }
}

internal const val MAX_NOTIFICATION_TITLE_LENGTH = 256
internal const val MAX_NOTIFICATION_TEXT_LENGTH = 1_024
internal const val MAX_NOTIFICATION_EXPANDED_LINES = 10
internal const val MAX_NOTIFICATION_EXPANDED_TEXT_LENGTH = 4_096
private const val MAX_TITLE_LENGTH = MAX_NOTIFICATION_TITLE_LENGTH
private const val MAX_TEXT_LENGTH = MAX_NOTIFICATION_TEXT_LENGTH
private const val MAX_EXPANDED_LINES = MAX_NOTIFICATION_EXPANDED_LINES
private const val MAX_EXPANDED_TEXT_LENGTH = MAX_NOTIFICATION_EXPANDED_TEXT_LENGTH
private const val MAX_EXPANDED_LINE_LENGTH = 1_024
private const val MAX_NOTIFICATION_KEY_LENGTH = 256
