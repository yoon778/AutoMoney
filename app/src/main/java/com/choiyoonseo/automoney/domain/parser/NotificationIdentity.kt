package com.choiyoonseo.automoney.domain.parser

import java.security.MessageDigest

fun notificationIdentityHash(snapshot: NotificationSnapshot): String {
    val identity = snapshot.notificationKey
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let { key ->
            listOf(snapshot.packageName, key, snapshot.postedAt.toEpochMilli().toString())
                .joinToString("|")
        }
        ?: listOf(
            snapshot.packageName,
            snapshot.postedAt.toEpochMilli().toString(),
            sha256(snapshot.combinedText)
        ).joinToString("|")

    return sha256(identity)
}

private fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
