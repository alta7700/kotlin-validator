package org.goal2be.standard.auth.jwt

import com.auth0.jwt.exceptions.*
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import org.goal2be.standard.auth.AuthenticationHttpException

@Suppress("UNUSED")
class JWTConsumer<V: JWTConsumerVerifier>(private val verifier: V) {

    companion object

    class VerificationFailDescriber(
        private val verifier: JWTVerifier,
    ) : JWTVerifier {

        private fun describe(wrapped: () -> DecodedJWT) : DecodedJWT = try {
            wrapped()
        } catch (exc: TokenExpiredException) {
            throw AuthenticationHttpException.ExpiredCredentials.exception
        } catch (exc: SignatureVerificationException) {
            throw AuthenticationHttpException.IncorrectCredentials.exception
        } catch (exc: MissingClaimException) {
            throw AuthenticationHttpException.IncorrectCredentials.exception
        } catch (exc: IncorrectClaimException) {
            throw AuthenticationHttpException.IncorrectCredentials.exception
        } catch (exc: InvalidClaimException) {
            throw AuthenticationHttpException.IncorrectCredentials.exception
        } catch (exc: JWTDecodeException) {
            throw AuthenticationHttpException.IncorrectCredentials.exception
        } catch (exc: AlgorithmMismatchException) {
            throw AuthenticationHttpException.Unauthorized.exception
        } catch (exc: JWTVerificationException) {
            throw AuthenticationHttpException.Unauthorized.exception
        }

        override fun verify(token: DecodedJWT): DecodedJWT = describe { verifier.verify(token) }
        override fun verify(token: String): DecodedJWT = describe { verifier.verify(token) }

    }

    val realm get() = verifier.getRealm()

    fun verificationFailDescriber(
        verifier: JWTVerifier,
    ) = VerificationFailDescriber(verifier)

    fun verificationFailDescriber(
        getVerifier: V.() -> JWTVerifier,
    ) = verificationFailDescriber(verifier.getVerifier())
}

interface JWTConsumerVerifier {
    fun getRealm(): String
}
