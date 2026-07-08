package com.choiyoonseo.automoney.domain.parser

class NotificationParserRouter(
    private val parsers: List<NotificationParser>
) : NotificationParser {
    override fun canParse(snapshot: NotificationSnapshot): Boolean =
        parsers.any { it.canParse(snapshot) }

    override fun parse(snapshot: NotificationSnapshot): ParseResult {
        val parser = parsers.firstOrNull { it.canParse(snapshot) }
            ?: return ParseResult.Ignored("unsupported package")
        return parser.parse(snapshot)
    }
}
