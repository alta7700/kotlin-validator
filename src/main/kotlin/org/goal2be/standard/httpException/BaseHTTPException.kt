package org.goal2be.standard.httpException

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

interface IHTTPExceptionBody {
    val code: String
}

interface IHTTPExceptionWrapped<T: IHTTPExceptionBody> {
    fun excStatusCode() : HttpStatusCode
    fun excCode() : String
    fun excBodySerializer() : KSerializer<T>
    fun excBody() : T
}

abstract class BaseHTTPException : Exception("HTTPException") {
    abstract suspend fun respond(call: ApplicationCall)
    suspend fun <T: IHTTPExceptionBody> respondWrapped(call: ApplicationCall, wrapped: IHTTPExceptionWrapped<T>) {
        call.respondText(
            text = Json.encodeToString(wrapped.excBodySerializer(), wrapped.excBody()),
            contentType = ContentType.Application.Json,
            status = wrapped.excStatusCode()
        )
    }
}

interface ISimpleHTTPExceptionWrapped<T: IHTTPExceptionBody> : IHTTPExceptionWrapped<T> {
    val statusCode: HttpStatusCode
    val code: String
    val serializer: KSerializer<T>
    val body: T

    override fun excStatusCode(): HttpStatusCode = statusCode
    override fun excCode(): String = code
    override fun excBodySerializer(): KSerializer<T> = serializer
    override fun excBody(): T = body
}
