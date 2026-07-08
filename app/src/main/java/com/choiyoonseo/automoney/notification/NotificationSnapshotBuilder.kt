package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import java.time.Instant

data class NotificationContentFields(
    val packageName: String,
    val postTimeMillis: Long,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val textLines: List<String> = emptyList()
)

class NotificationSnapshotBuilder {
    fun build(fields: NotificationContentFields): NotificationSnapshot {
        val title = fields.title.clean()
        val text = fields.text.clean()
        val primaryLines = listOfNotNull(title, text)

        val expandedLines = buildList {
            addAll(fields.bigText?.lines().orEmpty())
            addAll(fields.textLines)
        }
            .mapNotNull { it.clean() }
            .filterNot { line -> primaryLines.contains(line) }
            .distinct()

        return NotificationSnapshot(
            packageName = fields.packageName,
            title = title,
            text = text,
            bigText = expandedLines.joinToString("\n").ifBlank { null },
            postedAt = Instant.ofEpochMilli(fields.postTimeMillis)
        )
    }

    private fun String?.clean(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}
