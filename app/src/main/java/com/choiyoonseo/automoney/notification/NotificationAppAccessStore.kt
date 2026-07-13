package com.choiyoonseo.automoney.notification

import android.content.Context
import android.content.SharedPreferences

enum class NotificationAccessUpdateResult {
    UPDATED,
    LIMIT_REACHED,
    INVALID_PACKAGE,
    DENIED_PACKAGE
}

interface NotificationAppAccessStore {
    fun accessFor(packageName: String): NotificationSourceAccess

    fun setAllowed(
        packageName: String,
        allowed: Boolean
    ): NotificationAccessUpdateResult

    fun explicitlyEnabledPackages(): Set<String>
}

class SharedPreferencesNotificationAppAccessStore internal constructor(
    private val preferences: NotificationSourcePreferences,
    private val selfPackageName: String,
    private val policy: NotificationSourceAccessPolicy = NotificationSourceAccessPolicy()
) : NotificationAppAccessStore {
    constructor(context: Context) : this(
        preferences = SharedPreferencesNotificationSourcePreferences(
            context.applicationContext.getSharedPreferences(
                NOTIFICATION_SOURCE_PREFERENCES,
                Context.MODE_PRIVATE
            )
        ),
        selfPackageName = context.applicationContext.packageName
    )

    override fun accessFor(packageName: String): NotificationSourceAccess {
        if (!isValidNotificationPackageName(packageName)) {
            return NotificationSourceAccess.BLOCKED
        }
        if (packageName in deniedNotificationPackages(selfPackageName)) {
            return NotificationSourceAccess.BLOCKED
        }
        return policy.accessFor(
            packageName = packageName,
            explicitlyEnabledPackages = preferences.getStringSet(KEY_EXPLICITLY_ENABLED),
            explicitlyDisabledPackages = preferences.getStringSet(KEY_EXPLICITLY_DISABLED)
        )
    }

    @Synchronized
    override fun setAllowed(
        packageName: String,
        allowed: Boolean
    ): NotificationAccessUpdateResult {
        if (packageName in deniedNotificationPackages(selfPackageName)) {
            return NotificationAccessUpdateResult.DENIED_PACKAGE
        }
        if (!isValidNotificationPackageName(packageName)) {
            return NotificationAccessUpdateResult.INVALID_PACKAGE
        }

        val enabled = preferences.getStringSet(KEY_EXPLICITLY_ENABLED).toMutableSet()
        val disabled = preferences.getStringSet(KEY_EXPLICITLY_DISABLED).toMutableSet()
        val defaultTrusted = FinancialAppRegistry.infoForPackage(packageName)
            ?.defaultContentAccess == true

        if (allowed) {
            if (!defaultTrusted && packageName !in enabled && enabled.size >= MAX_EXPLICITLY_ENABLED) {
                return NotificationAccessUpdateResult.LIMIT_REACHED
            }
            disabled.remove(packageName)
            if (defaultTrusted) {
                enabled.remove(packageName)
            } else {
                enabled.add(packageName)
            }
        } else {
            enabled.remove(packageName)
            if (defaultTrusted) {
                disabled.add(packageName)
            } else {
                disabled.remove(packageName)
            }
        }

        preferences.putStringSets(
            mapOf(
                KEY_EXPLICITLY_ENABLED to enabled,
                KEY_EXPLICITLY_DISABLED to disabled
            )
        )
        return NotificationAccessUpdateResult.UPDATED
    }

    override fun explicitlyEnabledPackages(): Set<String> =
        preferences.getStringSet(KEY_EXPLICITLY_ENABLED).toSet()
}

internal interface NotificationSourcePreferences {
    fun getStringSet(key: String): Set<String>

    fun putStringSets(values: Map<String, Set<String>>)

    fun getString(key: String): String?

    fun putString(key: String, value: String)
}

internal class SharedPreferencesNotificationSourcePreferences(
    private val preferences: SharedPreferences
) : NotificationSourcePreferences {
    override fun getStringSet(key: String): Set<String> =
        preferences.getStringSet(key, emptySet())?.toSet().orEmpty()

    override fun putStringSets(values: Map<String, Set<String>>) {
        preferences.edit().apply {
            values.forEach { (key, value) -> putStringSet(key, value.toSet()) }
        }.apply()
    }

    override fun getString(key: String): String? =
        preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
}

internal fun isValidNotificationPackageName(packageName: String): Boolean =
    PACKAGE_NAME_REGEX.matches(packageName)

internal fun deniedNotificationPackages(selfPackageName: String): Set<String> =
    setOf(selfPackageName, "android", "com.android.systemui")

internal const val NOTIFICATION_SOURCE_PREFERENCES = "notification_sources"
private const val KEY_EXPLICITLY_ENABLED = "explicitly_enabled_packages"
private const val KEY_EXPLICITLY_DISABLED = "explicitly_disabled_packages"
private const val MAX_EXPLICITLY_ENABLED = 50
private val PACKAGE_NAME_REGEX = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
