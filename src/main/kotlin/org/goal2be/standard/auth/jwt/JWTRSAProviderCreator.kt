package org.goal2be.standard.auth.jwt

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPrivateCrtKeySpec
import java.security.spec.RSAPublicKeySpec
import java.util.*

abstract class JWTRSAProviderCreator(
    privateKeyString: String,
    val issuer: String,
    val realm: String,
    jwkKID: String,
) : JWTProviderCreator {

    private val privateKey: RSAPrivateKey
    private val publicKey: RSAPublicKey
    abstract val subject: String

    init {
        val keyFactory = KeyFactory.getInstance("RSA")
        privateKey = keyFactory.generatePrivate(
            PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString))
        ) as RSAPrivateKey

        val rsaPrivateKeySpec = keyFactory.getKeySpec(privateKey, RSAPrivateCrtKeySpec::class.java)
        val rsaPublicKeySpec = RSAPublicKeySpec(rsaPrivateKeySpec.modulus, rsaPrivateKeySpec.publicExponent)
        publicKey = keyFactory.generatePublic(rsaPublicKeySpec) as RSAPublicKey
    }

    private val jwksItem = JWKSItem(
        "RSA",
        jwkKID,
        Base64.getUrlEncoder().encodeToString(publicKey.publicExponent.toByteArray()),
        Base64.getUrlEncoder().encodeToString(publicKey.modulus.toByteArray())
    )
    val jwkProvider = LocalRSAJwkProvider(jwksItem)
    private val algorithm: Algorithm = Algorithm.RSA256(
        jwkProvider.get(jwksItem.keyId).publicKey as RSAPublicKey,
        privateKey
    )

    internal fun create(build: JWTCreator.Builder.() -> JWTCreator.Builder): String = JWT.create()
        .withIssuer(issuer)
        .withSubject(subject)
        .build()
        .sign(algorithm)

    override fun clone(token: DecodedJWT, change: JWTCreator.Builder.() -> JWTCreator.Builder): String = JWT.create()
        .withIssuer(token.issuer)
        .withSubject(token.subject)
        .withExpiresAt(token.expiresAt)
        .cloneInternal(token)
        .change()
        .sign(algorithm)
    abstract val cloneInternal: JWTCreator.Builder.(token: DecodedJWT) -> JWTCreator.Builder

    override fun getJWKSItem(): JWKSItem = jwksItem

}

class LocalRSAJwkProvider(
    keyId: String,
    exponent: String,
    modulus: String
) : JwkProvider {
    constructor(jwk: JWKSItem) : this(
        keyId = jwk.keyId,
        exponent = jwk.exponent,
        modulus = jwk.modulus,
    )
    private val jwk = Jwk.fromValues(mapOf(
        "kty" to "RSA",
        "kid" to keyId,
        "e" to exponent,
        "n" to modulus,
    ))
    override fun get(keyId: String?): Jwk = jwk
}
