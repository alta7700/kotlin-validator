package org.goal2be.standard.utils

import kotlinx.datetime.*
import java.time.format.DateTimeParseException

class DateTimeParsingError : Error()

fun String.toDate() = toDateOrNull() ?: throw DateTimeParsingError()

fun LocalDate.Companion.utcNow() = Clock.System.now().toLocalDateTime(TimeZone.UTC).date

fun String.toDateOrNull() = try {
    LocalDate.parse(this)
} catch (_: DateTimeParseException) {
    null
} catch (_: IllegalArgumentException) {
    null
}

fun Instant.utcDateTime(): LocalDateTime {
    return toLocalDateTime(TimeZone.UTC)
}
