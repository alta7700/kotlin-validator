package org.goal2be.standard.auth.service

import com.auth0.jwt.JWTCreator
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.Verification
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.*
import org.goal2be.standard.validator.DataClassValidator

data class ServiceTokenData(
    val serviceName: String,
    val grants: List<String>,
) {
    companion object {
        private val validator = DataClassValidator.create<ServiceTokenData>()
        private val fromMapStringAny = validator.buildConvertFrom<Map<String, Any?>>()
        fun fromJWTPrincipal(principal: JWTPrincipal) : ServiceTokenData {
            return fromMapStringAny(mapOf(
                "serviceName" to principal["sn"],
                "grants" to principal.getListClaim("grants", String::class)
            ))
        }
        fun fromDecodedJWT(decodedJWT: DecodedJWT) : ServiceTokenData {
            return fromMapStringAny(mapOf(
                "serviceName" to decodedJWT.getClaim("sn").asString(),
                "grants" to decodedJWT.getClaim("grants").asList(String::class.java)
            ))
        }
    }
}

fun Verification.withServiceTokenData() : Verification = this
    .withClaimPresence("sn")
    .withClaimPresence("grants")

fun JWTCreator.Builder.withServiceClaim(data: ServiceTokenData): JWTCreator.Builder = this
    .withClaim("sn", data.serviceName)
    .withClaim("grants", data.grants)


val ServiceTokenDataAttrKey = AttributeKey<ServiceTokenData>("ServiceTokenData")
fun ApplicationCall.setServiceTokenData(serviceTokenData: ServiceTokenData) {
    attributes.put(ServiceTokenDataAttrKey, serviceTokenData)
}
fun ApplicationCall.getServiceTokenData() = getServiceTokenDataOrNull() ?: throw IllegalStateException("ServiceTokenData was not set to call")
fun ApplicationCall.getServiceTokenDataOrNull() = attributes.getOrNull(ServiceTokenDataAttrKey)
