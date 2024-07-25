package org.goal2be.standard.auth.jwt

import com.auth0.jwt.JWTCreator
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("UNUSED")
class JWTProvider<C: JWTProviderCreator>(val inner: C) {
    fun token(build: C.() -> String) : String = inner.build()
}

@Suppress("UNUSED")
fun Application.wellKnownRoute(vararg providers: JWTProvider<*>, prefix: String = "") {
    routing {
        get("$prefix/.well-known/jwks.json") {
            call.respond(HttpStatusCode.OK, mapOf( "keys" to providers.map { it.inner.getJWKSItem() }))
        }
    }
}

interface JWTProviderCreator {

    fun clone(token: DecodedJWT, change: JWTCreator.Builder.() -> JWTCreator.Builder) : String

    fun getJWKSItem(): JWKSItem
}

@Serializable
data class JWKSItem(
    @SerialName("kty") val keyType: String,
    @SerialName("kid") val keyId: String,
    @SerialName("e") val exponent: String,
    @SerialName("n") val modulus: String,
)
