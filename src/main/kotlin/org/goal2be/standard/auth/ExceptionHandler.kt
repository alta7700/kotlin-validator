package org.goal2be.standard.auth

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.statuspages.*

fun StatusPagesConfig.addUnauthorizedStatus() {
    status(HttpStatusCode.Unauthorized) { call, _ ->
        val cause = if (call.authentication.allFailures.isNotEmpty()) call.authentication.allFailures[0] else null
        when (cause) {
            is AuthenticationFailedCause.Error -> {
                AuthenticationHttpException.fromMessage(cause.message).httpException
            }
            else ->
                AuthenticationHttpException.Unauthorized.httpException
        }.respond(call)
    }
}