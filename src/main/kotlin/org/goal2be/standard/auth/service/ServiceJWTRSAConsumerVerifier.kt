package org.goal2be.standard.auth.service

import com.auth0.jwk.JwkProvider
import com.auth0.jwt.interfaces.Verification
import org.goal2be.standard.auth.jwt.JWTConsumer
import org.goal2be.standard.auth.jwt.JWTProvider
import org.goal2be.standard.auth.jwt.JWTRSAConsumerVerifier

@Suppress("UNUSED")
class ServiceJWTRSAConsumerVerifier(
    realm: String,
    issuer: String,
    jwkProvider: JwkProvider? = null,
    keyId: String? = null
) : JWTRSAConsumerVerifier(realm, issuer, jwkProvider, keyId) {

    override val subject = "service"

    val addDefaults: Verification.() -> Verification = { withServiceTokenData() }

    fun buildTokenVerifier() = build { addDefaults() }

}


@Suppress("UNUSED")
fun JWTProvider<ServiceJWTRSAProviderCreator>.toServiceJWTRSAConsumer(): JWTConsumer<ServiceJWTRSAConsumerVerifier> {
    return JWTConsumer(ServiceJWTRSAConsumerVerifier(
        inner.realm,
        inner.issuer,
        inner.jwkProvider,
        inner.getJWKSItem().keyId,
    ))
}
