package org.goal2be.standard.utils

import java.util.*

class InvalidUUIDException: Exception()

fun String.uuid() = uuidOrNull() ?: throw InvalidUUIDException()
fun String.uuidOrNull() = try {
    UUID.fromString(this)
} catch (_: IllegalArgumentException) {
    null
}
