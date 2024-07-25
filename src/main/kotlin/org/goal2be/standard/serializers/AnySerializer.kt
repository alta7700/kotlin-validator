package org.goal2be.standard.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private fun Any?.toJsonPrimitive(): JsonPrimitive {
    return when (this) {
        null -> JsonNull
        is JsonPrimitive -> this
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        else -> throw Exception("${this::class}")
    }
}
private fun Any?.toJsonElement(): JsonElement {
    return when (this) {
        null -> JsonNull
        is JsonElement -> this
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
        is Map<*, *> -> JsonObject(this.map { it.key.toString() to it.value.toJsonElement() }.toMap())
        else -> throw Exception("${this::class}=${this}}")
    }
}

private fun JsonPrimitive.toAnyValue(): Any? {
    val content = this.content
    return when {
        this.isString -> content
        content.equals("null", ignoreCase = true) -> null
        content.equals("true", ignoreCase = true) -> true
        content.equals("false", ignoreCase = true) -> false
        content.toIntOrNull() != null -> content.toInt()
        content.toLongOrNull() != null -> content.toLong()
        content.toDoubleOrNull() != null -> content.toDouble()
        else -> throw Exception("content：$content")
    }
}

private fun JsonElement.toAnyOrNull(): Any? {
    return when (this) {
        is JsonNull -> null
        is JsonPrimitive -> toAnyValue()
        is JsonObject -> this.map { it.key to it.value.toAnyOrNull() }.toMap()
        is JsonArray -> this.map { it.toAnyOrNull() }
    }
}

open class AnySerializer : KSerializer<Any?> {
    open val delegateSerializer = JsonElement.serializer()
    override val descriptor
        get() = delegateSerializer.descriptor

    override fun deserialize(decoder: Decoder): Any? {
        return decoder.decodeSerializableValue(delegateSerializer).toAnyOrNull()
    }

    override fun serialize(encoder: Encoder, value: Any?) {
        encoder.encodeSerializableValue(delegateSerializer, value.toJsonElement())
    }
}

class PrimitiveAnySerializer : AnySerializer() {
    override val delegateSerializer = JsonElement.serializer()
    override fun serialize(encoder: Encoder, value: Any?) {
        encoder.encodeSerializableValue(delegateSerializer, value.toJsonPrimitive())
    }
}