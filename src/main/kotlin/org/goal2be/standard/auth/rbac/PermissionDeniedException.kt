@file:Suppress("UNUSED")
package org.goal2be.standard.auth.rbac

import org.goal2be.standard.httpException.BaseHTTPException
import org.goal2be.standard.httpException.IHTTPExceptionBody
import org.goal2be.standard.httpException.ISimpleHTTPExceptionWrapped
import io.ktor.http.*
import io.ktor.server.application.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable(with = PermissionDeniedExceptionCause.LocalSerializer::class)
sealed class PermissionDeniedExceptionCause {

    @Serializable
    data class Cause(val message: String) : PermissionDeniedExceptionCause()

    @Serializable
    data class OneOf(@SerialName("one_of") val oneOf: List<PermissionDeniedExceptionCause>) : PermissionDeniedExceptionCause()

    @OptIn(ExperimentalSerializationApi::class)
    @Serializer(forClass = PermissionDeniedExceptionCause::class)
    object LocalSerializer : JsonContentPolymorphicSerializer<PermissionDeniedExceptionCause>(
        PermissionDeniedExceptionCause::class) {
        override fun selectDeserializer(element: JsonElement): DeserializationStrategy<PermissionDeniedExceptionCause> {
            return if ("message" in element.jsonObject) Cause.serializer() else OneOf.serializer()
        }
    }
}

@Serializable
data class PermissionDeniedExceptionBody(
    override val code: String,
    val cause: PermissionDeniedExceptionCause,
) : IHTTPExceptionBody


class PermissionDeniedExceptionWrapped(
    cause: PermissionDeniedExceptionCause,
) : ISimpleHTTPExceptionWrapped<PermissionDeniedExceptionBody> {
    override val code = "permission_denied"
    override val statusCode = HttpStatusCode.Forbidden
    override val serializer = PermissionDeniedExceptionBody.serializer()
    override val body = PermissionDeniedExceptionBody(excCode(), cause)
}


class PermissionDeniedException(
    private val wrapped: PermissionDeniedExceptionWrapped
) : BaseHTTPException() {
    constructor(cause: PermissionDeniedExceptionCause) : this(PermissionDeniedExceptionWrapped(cause))
    constructor(cause: String) : this(PermissionDeniedExceptionCause.Cause(cause))

    override suspend fun respond(call: ApplicationCall) = respondWrapped(call, wrapped)
}
