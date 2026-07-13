package com.choiyoonseo.automoney.notification

import android.content.Context
import java.time.Instant

data class ObservedNotificationSource(
    val packageName: String,
    val lastSeenAt: Instant,
    val count: Int
)

interface ObservedNotificationSourceStore {
    fun record(packageName: String, seenAt: Instant)

    fun load(): List<ObservedNotificationSource>
}

class SharedPreferencesObservedNotificationSourceStore internal constructor(
    private val preferences: NotificationSourcePreferences,
    private val selfPackageName: String
) : ObservedNotificationSourceStore {
    constructor(context: Context) : this(
        preferences = SharedPreferencesNotificationSourcePreferences(
            context.applicationContext.getSharedPreferences(
                NOTIFICATION_SOURCE_PREFERENCES,
                Context.MODE_PRIVATE
            )
        ),
        selfPackageName = context.applicationContext.packageName
    )

    @Synchronized
    override fun record(packageName: String, seenAt: Instant) {
        if (packageName in deniedNotificationPackages(selfPackageName)) return
        if (!isValidNotificationPackageName(packageName)) return

        val current = load().associateBy { it.packageName }.toMutableMap()
        val existing = current[packageName]
        current[packageName] = ObservedNotificationSource(
            packageName = packageName,
            lastSeenAt = maxOf(existing?.lastSeenAt ?: seenAt, seenAt),
            count = existing?.count?.let { if (it == Int.MAX_VALUE) it else it + 1 } ?: 1
        )

        val retained = current.values
            .sortedWith(compareByDescending<ObservedNotificationSource> { it.lastSeenAt }
                .thenBy { it.packageName })
            .take(MAX_OBSERVED_SOURCES)
        preferences.putString(KEY_OBSERVED_SOURCES, encodeObservedNotificationSources(retained))
    }

    override fun load(): List<ObservedNotificationSource> =
        decodeObservedNotificationSources(preferences.getString(KEY_OBSERVED_SOURCES).orEmpty())
            .sortedWith(compareByDescending<ObservedNotificationSource> { it.lastSeenAt }
                .thenBy { it.packageName })
            .take(MAX_OBSERVED_SOURCES)
}

internal fun encodeObservedNotificationSources(
    sources: List<ObservedNotificationSource>
): String = sources.joinToString("\n") { source ->
    "${source.packageName}|${source.lastSeenAt.toEpochMilli()}|${source.count}"
}

internal fun decodeObservedNotificationSources(value: String): List<ObservedNotificationSource> =
    value.lineSequence().mapNotNull { row ->
        val parts = row.split('|', limit = 3)
        if (parts.size != 3) return@mapNotNull null
        val packageName = parts[0]
        if (!isValidNotificationPackageName(packageName)) return@mapNotNull null
        try {
            val count = parts[2].toInt()
            if (count <= 0) return@mapNotNull null
            ObservedNotificationSource(
                packageName = packageName,
                lastSeenAt = Instant.ofEpochMilli(parts[1].toLong()),
                count = count
            )
        } catch (_: RuntimeException) {
            null
        }
    }.toList()

private const val KEY_OBSERVED_SOURCES = "observed_sources"
private const val MAX_OBSERVED_SOURCES = 50
