package org.goal2be.standard.auth.jwt

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.auth0.jwt.interfaces.Verification
import java.security.interfaces.RSAPublicKey

abstract class JWTRSAConsumerVerifier(
    private val realm: String,
    private val issuer: String,
    jwkProvider: JwkProvider? = null,
    private val keyId: String? = null
) : JWTConsumerVerifier {
    private val jwkProvider = if (jwkProvider !== null) jwkProvider else JwkProviderBuilder(issuer).build()

    private val algorithm = Algorithm.RSA256(this.jwkProvider.get(keyId).publicKey as RSAPublicKey)
    override fun getRealm(): String = realm
    abstract val subject: String

    internal fun build(additionalChecks: Verification.() -> Verification): JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withSubject(subject)
        .additionalChecks()
        .build()
}