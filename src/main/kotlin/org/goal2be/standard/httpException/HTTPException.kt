package org.goal2be.standard.httpException

import io.ktor.http.*
import io.ktor.server.application.*
import kotlinx.serialization.Serializable

@Serializable
private data class HTTPExceptionBody(override val code: String) : IHTTPExceptionBody

private class HTTPExceptionWrapped(
    override val statusCode: HttpStatusCode,
    override val code: String
) : ISimpleHTTPExceptionWrapped<HTTPExceptionBody> {
    override val serializer = HTTPExceptionBody.serializer()
    override val body = HTTPExceptionBody(excCode())
}

class HTTPException private constructor(private val wrapped: HTTPExceptionWrapped) : BaseHTTPException() {
    constructor(statusCode: HttpStatusCode, code: String) : this(HTTPExceptionWrapped(statusCode, code))

    override suspend fun respond(call: ApplicationCall) = respondWrapped(call, wrapped)
}