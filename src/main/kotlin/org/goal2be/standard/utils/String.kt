@file:Suppress("UNUSED")
package org.goal2be.standard.utils

val LettersAndDigits = ('A'..'Z') + ('a'..'z') + ('0'..'9')
val Letters = ('A'..'Z') + ('a'..'z')
val UppercaseLetters = 'A'..'Z'
val LowercaseLetters = 'a'..'z'
val Digits = '0'..'9'

val emailRegex = Regex("^(?!\\.)[a-zA-Z0-9._%+-]+@(?!-)[a-zA-Z0-9.-]+\\.[a-zA-Z]{1,25}\$")

fun getRandomString(length: Int, allowedChars: List<Char> = LettersAndDigits) : String {
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}