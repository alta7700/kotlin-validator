@file:Suppress("UNUSED")
package org.goal2be.standard.validator.exceptions

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.goal2be.standard.serializers.AnySerializer

@Serializable
data class ValidationExceptionData(
    val type: String,
    val message: String,
    val loc: List<String>,
    val ctx: Map<String, @Serializable(with = AnySerializer::class) Any?>?,
    val input: @Serializable(with = AnySerializer::class) Any?,
) {
    companion object {
        @Throws(Exception::class)
        fun fromException(exc: ValidationException): ValidationExceptionData {
            if (exc.message.isEmpty() || exc.location.isEmpty() || exc.input === ValidationException.UNSET) {
                throw Exception("message(${exc.message}), location(${exc.location}) or input(${exc.input}) are not set")
            }
            return ValidationExceptionData(
                type = exc.type,
                message = exc.message,
                loc = exc.location,
                ctx = exc.ctx.ifEmpty { null },
                input = exc.input,
            )
        }
    }
}
class ValidationException(
    val type: String,
    override val message: String = "",
    val location: List<String> = listOf(),
    val ctx: Map<String, Any?> = mapOf(),
    val input: Any? = UNSET
) : Exception(message) {

    companion object {
        val UNSET = object {}
    }

    operator fun invoke(vararg ctx: Pair<String, Any?>) = invoke(location = null, ctx = ctx)
    operator fun invoke(location: String) = invoke(location = listOf(location))
    operator fun invoke(location: String, input: Any?) = invoke(location = listOf(location), input = input)

    operator fun invoke(
        message: String? = null,
        location: List<String>? = null,
        input: Any? = this.input,
        vararg ctx: Pair<String, Any?>,
    ): ValidationException {
        return ValidationException(
            type = type,
            message = message ?: this.message,
            location = if (location === null) this.location else location + this.location,
            ctx = if (ctx.isNotEmpty()) this.ctx + ctx else this.ctx,
            input = input,
        )
    }
}

open class ValidationExceptionList(
    val exceptions: MutableList<ValidationException> = mutableListOf()
) : Exception("Validation exceptions"), Collection<ValidationException> by exceptions {
    constructor(vararg exceptions: ValidationException): this(exceptions.toMutableList())

    fun add(exc: ValidationException): ValidationExceptionList {
        exceptions.add(exc)
        return this
    }
    fun addAll(vararg excs: ValidationException): ValidationExceptionList {
        exceptions.addAll(excs)
        return this
    }
    fun addAll(excs: List<ValidationException>): ValidationExceptionList {
        exceptions.addAll(excs)
        return this
    }

    operator fun plus(other: ValidationExceptionList): ValidationExceptionList {
        return ValidationExceptionList((this.exceptions + other.exceptions).toMutableList())
    }

    operator fun plusAssign(other: ValidationExceptionList) {
        this.addAll(other.exceptions)
    }
    operator fun plusAssign(other: ValidationException) {
        this.add(other)
    }

    fun prependLocation(location: String): ValidationExceptionList {
        val newErr = ValidationExceptionList()
        this.forEach { err -> newErr.add(err(location = location)) }
        return newErr
    }
    fun prependLocation(location: List<String>): ValidationExceptionList {
        val newErr = ValidationExceptionList()
        this.forEach { err -> newErr.add(err(location = location)) }
        return newErr
    }

    fun toSerializable() = exceptions.map(ValidationExceptionData::fromException)

    override fun getLocalizedMessage(): String {
        return "${super.getLocalizedMessage()}: ${Json.encodeToString<List<ValidationExceptionData>>(toSerializable())}"
    }

}

fun <T> modifyValidationException(location: String, input: Any? = null, callback: () -> T) : T {
    try {
        return callback()
    } catch (exc: ValidationException) {
        throw exc(location = location, input = input)
    } catch (exc: ValidationExceptionList) {
        throw exc.prependLocation(location)
    }
}
fun <T> modifyValidationException(location: List<String>, input: Any? = null, callback: () -> T) : T {
    try {
        return callback()
    } catch (exc: ValidationException) {
        throw exc(location = location, input = input)
    } catch (exc: ValidationExceptionList) {
        throw exc.prependLocation(location)
    }
}

fun accumulateValidationException(excs: ValidationExceptionList, callback: () -> Unit) {
    try {
        callback()
    } catch (exc: ValidationException) {
        excs += exc
    } catch (exc: ValidationExceptionList) {
        excs += exc
    }
}
suspend fun accumulateValidationExceptionSuspend(excs: ValidationExceptionList, callback: suspend () -> Unit) {
    try {
        callback()
    } catch (exc: ValidationException) {
        excs += exc
    } catch (exc: ValidationExceptionList) {
        excs += exc
    }
}
fun accumulateValidationException(excs: ValidationExceptionList, location: String, input: Any? = null, callback: () -> Unit) {
    try {
        callback()
    } catch (exc: ValidationException) {
        excs += exc(location = location, input = input)
    } catch (exc: ValidationExceptionList) {
        excs += exc.prependLocation(location)
    }
}
suspend fun accumulateValidationExceptionSuspend(excs: ValidationExceptionList, location: String, input: Any? = null, callback: suspend () -> Unit) {
    try {
        callback()
    } catch (exc: ValidationException) {
        excs += exc(location = location, input = input)
    } catch (exc: ValidationExceptionList) {
        excs += exc.prependLocation(location)
    }
}
fun accumulateValidationException(excs: ValidationExceptionList, location: List<String>, input: Any? = null, callback: () -> Unit) {
    try {
        callback()
    } catch (exc: ValidationException) {
        excs += exc(location = location, input = input)
    } catch (exc: ValidationExceptionList) {
        excs += exc.prependLocation(location)
    }
}
suspend fun accumulateValidationExceptionSuspend(excs: ValidationExceptionList, location: List<String>, input: Any? = null, callback: suspend () -> Unit) {
    try {
        callback()
    } catch (exc: ValidationException) {
        excs += exc(location = location, input = input)
    } catch (exc: ValidationExceptionList) {
        excs += exc.prependLocation(location)
    }
}