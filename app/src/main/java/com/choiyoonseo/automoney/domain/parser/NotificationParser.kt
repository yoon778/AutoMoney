package com.choiyoonseo.automoney.domain.parser

interface NotificationParser {
    fun canParse(snapshot: NotificationSnapshot): Boolean

    fun parse(snapshot: NotificationSnapshot): ParseResult
}
