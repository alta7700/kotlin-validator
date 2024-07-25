package org.goal2be.standard.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.goal2be.standard.auth.jwt.JWTConsumer
import org.goal2be.standard.auth.service.ServiceJWTRSAConsumerVerifier
import org.goal2be.standard.auth.service.ServiceTokenData
import org.goal2be.standard.auth.service.setServiceTokenData
import org.goal2be.standard.auth.user.UserJWTRSAConsumerVerifier
import org.goal2be.standard.auth.user.UserTokenData
import org.goal2be.standard.auth.user.setUserTokenData
import org.goal2be.standard.validator.exceptions.ValidationExceptionList

@Suppress("UNUSED")
fun Application.installAuthentication(
    userJWTConsumer: JWTConsumer<UserJWTRSAConsumerVerifier>? = null,
    serviceJWTConsumer: JWTConsumer<ServiceJWTRSAConsumerVerifier>? = null,
    configure: (AuthenticationConfig.() -> Unit)? = null
) {
    install(Authentication) {
        if (userJWTConsumer !== null)
            jwt("user-jwt") {
                realm = userJWTConsumer.realm
                verifier(userJWTConsumer.verificationFailDescriber { buildAccessTokenVerifier() })
                validate { credential -> JWTPrincipal(credential.payload).also {
                    try {
                        requestSubject = RequestSubject.User
                        setUserTokenData(UserTokenData.fromJWTPrincipal(it))
                    } catch (exc: ValidationExceptionList) {
                        throw AuthenticationHttpException.IncorrectCredentials.exception
                    }
                } }
                challenge { _, _ -> AuthenticationHttpException.Unauthorized.httpException.respond(call) }
            }
        if (serviceJWTConsumer !== null)
            jwt("service-jwt") {
                realm = serviceJWTConsumer.realm
                verifier(serviceJWTConsumer.verificationFailDescriber { buildTokenVerifier() })
                validate { credential -> JWTPrincipal(credential.payload).also {
                    try {
                        requestSubject = RequestSubject.Service
                        setServiceTokenData(ServiceTokenData.fromJWTPrincipal(it))
                    } catch (exc: ValidationExceptionList) {
                        throw AuthenticationHttpException.IncorrectCredentials.exception
                    }
                } }
                challenge { _, _ -> AuthenticationHttpException.Unauthorized.httpException.respond(call) }
            }

        configure?.invoke(this)
    }
}