package org.goal2be.standard.auth.user

import com.auth0.jwt.JWTCreator
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.Verification
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.*
import org.goal2be.standard.utils.uuid
import org.goal2be.standard.validator.DataClassValidator
import org.goal2be.standard.validator.types.base.itemsMeta
import org.goal2be.standard.validator.types.extended.mapBy
import java.util.*

data class UserTokenData(
    val id: UUID,
    val email: String,
    val username: String,
    val type: UserType?,
    val roles: List<UserRole>
) {
    @Suppress("UNUSED")
    constructor(
        id: String,
        email: String,
        username: String,
        type: Int?,
        roles: List<Int>,
    ) : this(
        id.uuid(),
        email,
        username,
        type?.let { UserType.fromInt(it) },
        UserRole.fromListInt(roles)
    )

    companion object {
        val validator = DataClassValidator.create<UserTokenData> {
            field(UserTokenData::type) { mapBy { it.value } }
            field(UserTokenData::roles) { itemsMeta { mapBy { it.value } } }
        }
        val fromMapStringAny = validator.buildConvertFrom<Map<String, Any?>>()
        fun fromJWTPrincipal(principal: JWTPrincipal) : UserTokenData {
            @Suppress("UNCHECKED_CAST")
            return fromMapStringAny(principal.getClaim("user", Map::class) as Map<String, Any?>)
        }
        fun fromDecodedJWT(decodedJWT: DecodedJWT) : UserTokenData {
            return fromMapStringAny(decodedJWT.getClaim("user").asMap())
        }
    }
}

fun Verification.withUserTokenData() : Verification = this
    .withClaimPresence("user")

fun JWTCreator.Builder.withUserClaim(data: UserTokenData): JWTCreator.Builder = this
    .withClaim("user", mapOf(
        "id" to data.id.toString(),
        "email" to data.email,
        "username" to data.username,
        "type" to data.type?.value?.toInt(),
        "roles" to data.roles.map { it.value.toInt() },
    ))

val UserTokenDataAttrKey = AttributeKey<UserTokenData>("UserTokenData")
fun ApplicationCall.setUserTokenData(userTokenData: UserTokenData) {
    attributes.put(UserTokenDataAttrKey, userTokenData)
}
fun ApplicationCall.getUserTokenData() = getUserTokenDataOrNull() ?: throw IllegalStateException("UserTokenData was not set to call")
fun ApplicationCall.getUserTokenDataOrNull() = attributes.getOrNull(UserTokenDataAttrKey)
