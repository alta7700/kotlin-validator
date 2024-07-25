package org.goal2be.standard.auth.user

import org.goal2be.standard.auth.jwt.JWTRSAProviderCreator
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.interfaces.DecodedJWT
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Suppress("UNUSED")
class UserJWTRSAProviderCreator(
    privateKeyString: String,
    issuer: String,
    realm: String,
    jwkKID: String,
) : JWTRSAProviderCreator(privateKeyString, issuer, realm, jwkKID) {

    override val subject: String = "user"
    private val accessTokenLifetime = 1.hours
    private val refreshTokenLifetime = 15.days

    val addDefaults: JWTCreator.Builder.(data: UserTokenData, tokenType: String) -> JWTCreator.Builder = { data, tokenType ->
        this
            .withUserClaim(data)
            .withClaim("type", tokenType)
    }

    fun createAccess(data: UserTokenData) = create {
        val now = Clock.System.now()
        this
            .addDefaults(data, "access")
            .withIssuedAt(now.toJavaInstant())
            .withExpiresAt((now + accessTokenLifetime).toJavaInstant())
    }
    fun createRefresh(data: UserTokenData) = create {
        val now = Clock.System.now()
        this
            .addDefaults(data, "refresh")
            .withIssuedAt(now.toJavaInstant())
            .withExpiresAt((now + refreshTokenLifetime).toJavaInstant())
    }

    override val cloneInternal: JWTCreator.Builder.(token: DecodedJWT) -> JWTCreator.Builder = { token ->
        addDefaults(UserTokenData.fromDecodedJWT(token), token.getClaim("type").asString())
    }

}
