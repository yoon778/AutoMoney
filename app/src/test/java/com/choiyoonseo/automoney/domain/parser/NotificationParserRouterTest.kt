package com.choiyoonseo.automoney.domain.parser

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class NotificationParserRouterTest {
    @Test
    fun routesToFirstParserThatCanParse() {
        val parser = RecordingParser(canParse = true, result = ParseResult.Ignored("handled"))
        val fallback = RecordingParser(canParse = true, result = ParseResult.Ignored("fallback"))
        val router = NotificationParserRouter(listOf(parser, fallback))

        val result = router.parse(snapshot("viva.republica.toss"))

        assertThat(result).isEqualTo(ParseResult.Ignored("handled"))
        assertThat(parser.parseCalls).isEqualTo(1)
        assertThat(fallback.parseCalls).isEqualTo(0)
    }

    @Test
    fun ignoresWhenNoParserCanParse() {
        val router = NotificationParserRouter(listOf(RecordingParser(canParse = false)))

        val result = router.parse(snapshot("unknown.package"))

        assertThat(result).isEqualTo(ParseResult.Ignored("unsupported package"))
    }

    private fun snapshot(packageName: String) = NotificationSnapshot(
        packageName = packageName,
        title = "title",
        text = "text",
        bigText = null,
        postedAt = Instant.parse("2026-07-03T01:00:00Z")
    )

    private class RecordingParser(
        private val canParse: Boolean,
        private val result: ParseResult = ParseResult.Ignored("unused")
    ) : NotificationParser {
        var parseCalls = 0

        override fun canParse(snapshot: NotificationSnapshot): Boolean = canParse

        override fun parse(snapshot: NotificationSnapshot): ParseResult {
            parseCalls += 1
            return result
        }
    }
}
