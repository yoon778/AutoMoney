package com.choiyoonseo.automoney.data.repository

class DuplicateNotificationException(
    val sourceNotificationHash: String?,
    cause: Throwable? = null
) : RuntimeException("duplicate notification: $sourceNotificationHash", cause)
