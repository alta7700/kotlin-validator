package org.goal2be.standard.auth.service

import com.auth0.jwt.JWTCreator
import com.auth0.jwt.interfaces.DecodedJWT
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.goal2be.standard.auth.jwt.JWTRSAProviderCreator

class ServiceJWTRSAProviderCreator(
    privateKeyString: String,
    issuer: String,
    realm: String,
    jwkKID: String,
) : JWTRSAProviderCreator(privateKeyString, issuer, realm, jwkKID) {
    override val subject: String = "service"

    val addDefaults: JWTCreator.Builder.(data: ServiceTokenData) -> JWTCreator.Builder = {
        withServiceClaim(it)
        withIssuedAt(Clock.System.now().toJavaInstant())
    }

    fun create(data: ServiceTokenData) = create {
        addDefaults(data)
    }

    override val cloneInternal: JWTCreator.Builder.(token: DecodedJWT) -> JWTCreator.Builder = {
        addDefaults(ServiceTokenData.fromDecodedJWT(it))
    }
}
