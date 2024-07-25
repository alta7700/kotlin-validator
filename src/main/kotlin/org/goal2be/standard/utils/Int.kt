package org.goal2be.standard.utils

private val ShortAvailableRange = Short.MIN_VALUE..Short.MAX_VALUE
fun Int.toShortOrNull() : Short? {
    if (this !in ShortAvailableRange) return null
    return this.toShort()
}