package org.goal2be.standard.auth

import io.ktor.http.*
import org.goal2be.standard.httpException.HTTPException

class AuthenticationException(override val message: String) : Exception(message) {
    val httpExc get() = HTTPException(HttpStatusCode.Unauthorized, message)
}

enum class AuthenticationHttpException(val message: String) {
    Unauthorized("unauthorized"),
    ExpiredCredentials("expired_credentials"),
    IncorrectCredentials("incorrect_credentials");

    companion object {
        fun fromMessage(message: String) = AuthenticationHttpException.entries.find { it.message == message }
            ?: throw Exception("Exception with message=$message is not found in ${AuthenticationHttpException::class}")
    }

    val exception: AuthenticationException = AuthenticationException(message)
    val httpException get() = exception.httpExc
}