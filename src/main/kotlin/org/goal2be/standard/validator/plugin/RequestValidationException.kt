package org.goal2be.standard.validator.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import kotlinx.serialization.Serializable
import org.goal2be.standard.httpException.BaseHTTPException
import org.goal2be.standard.httpException.IHTTPExceptionBody
import org.goal2be.standard.httpException.ISimpleHTTPExceptionWrapped
import org.goal2be.standard.validator.exceptions.ValidationException
import org.goal2be.standard.validator.exceptions.ValidationExceptionData
import org.goal2be.standard.validator.exceptions.ValidationExceptionList

@Serializable
data class RequestValidationExceptionBody(
    override val code: String,
    val details: List<ValidationExceptionData>,
) : IHTTPExceptionBody

class RequestValidationExceptionWrapped(
    private val details: List<ValidationExceptionData>
) : ISimpleHTTPExceptionWrapped<RequestValidationExceptionBody> {
    override val code = "validation"
    override val statusCode = HttpStatusCode.UnprocessableEntity
    override val serializer = RequestValidationExceptionBody.serializer()
    override val body = RequestValidationExceptionBody(excCode(), details)
}

class RequestValidationException private constructor(
    private val wrapped: RequestValidationExceptionWrapped
) : BaseHTTPException() {
    constructor(excs: ValidationExceptionList) : this(RequestValidationExceptionWrapped(excs.toSerializable()))
    constructor(exc: ValidationException) : this(ValidationExceptionList().add(exc))

    override suspend fun respond(call: ApplicationCall) = respondWrapped(call, wrapped)

}