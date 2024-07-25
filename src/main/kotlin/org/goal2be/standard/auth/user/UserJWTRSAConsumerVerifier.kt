package org.goal2be.standard.auth.user

import com.auth0.jwk.JwkProvider
import com.auth0.jwt.interfaces.Verification
import org.goal2be.standard.auth.jwt.JWTConsumer
import org.goal2be.standard.auth.jwt.JWTProvider
import org.goal2be.standard.auth.jwt.JWTRSAConsumerVerifier

@Suppress("UNUSED")
class UserJWTRSAConsumerVerifier(
    realm: String,
    issuer: String,
    jwkProvider: JwkProvider? = null,
    keyId: String? = null
) : JWTRSAConsumerVerifier(realm, issuer, jwkProvider, keyId) {

    override val subject = "user"

    val addDefaults: Verification.(tokenType: String) -> Verification = { tokenType ->
        this
            .withUserTokenData()
            .withClaim("type", tokenType)
    }

    fun buildAccessTokenVerifier() = build { addDefaults("access") }
    fun buildRefreshTokenVerifier() = build { addDefaults("refresh") }

}

@Suppress("UNUSED")
fun JWTProvider<UserJWTRSAProviderCreator>.toUserJWTRSAConsumer(): JWTConsumer<UserJWTRSAConsumerVerifier> {
    return JWTConsumer(UserJWTRSAConsumerVerifier(
        inner.realm,
        inner.issuer,
        inner.jwkProvider,
        inner.getJWKSItem().keyId,
    ))
}
