package org.goal2be.standard.validator.plugin

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import org.goal2be.standard.validator.DataClassValidator
import org.goal2be.standard.validator.IValidator
import org.goal2be.standard.validator.ValidatorConversionFunction
import org.goal2be.standard.validator.buildConvertFrom
import kotlin.reflect.typeOf

interface DataExtractor<R, T> {
    suspend fun extract(call: ApplicationCall) : Pair<R, Boolean>
    fun createConverter(validator: IValidator<T>) : ValidatorConversionFunction<R, T>
}

interface DataExtractorBuilder<R, T> {
    fun toExtractor() : DataExtractor<R, T>
}

class RootStringValuesExtractor<T>(
    private val fieldsAsList: List<String>,
    private val getStringValues: ApplicationCall.() -> StringValues
) : DataExtractor<Map<String, Any>, T> {
    private var fields: Map<String, Boolean> = mapOf()
    override suspend fun extract(call: ApplicationCall) = call.getStringValues().let { data ->
        val result = mutableMapOf<String, Any>()
        for ((alias, isList) in fields)
            if (isList) data.getAll(alias)?.let { result[alias] = it }
            else data[alias]?.let { result[alias] = it }
        result.toMap() to true
    }

    override fun createConverter(validator: IValidator<T>): ValidatorConversionFunction<Map<String, Any?>, T> {
        if (validator !is DataClassValidator) throw Error("Root extractor can only wrap data classes")
        // build fieldsAsList here, it's like a huck :)
        fields = validator.fieldValidators.associate{ fieldValidator ->
            fieldValidator.alias to (fieldValidator.name in fieldsAsList)
        }

        return validator.buildConvertFromMapStringToPolymorphic(fields.entries.associate {
            it.key to (if (it.value) typeOf<List<String>>() else typeOf<String>())
        })
    }
}

class FieldStringValuesExtractor<T>(
    private val alias: String,
    private val getStringValues: ApplicationCall.() -> StringValues
) : DataExtractor<String, T> {
    override suspend fun extract(call: ApplicationCall) = run {
        val value = call.getStringValues()[alias]
        if (value !== null) value to true
        else "" to false
    }

    override fun createConverter(validator: IValidator<T>): ValidatorConversionFunction<String, T> {
        return validator.buildConvertFrom<String, T>()
    }
}
class FieldListStringValuesExtractor<T>(
    private val alias: String,
    private val emptyByDefault: Boolean = false,
    private val getStringValues: ApplicationCall.() -> StringValues
) : DataExtractor<List<String>, T> {
    override suspend fun extract(call: ApplicationCall) = run {
        val value = call.getStringValues().getAll(alias)
        if (value !== null) value to true
        else listOf<String>() to emptyByDefault
    }

    override fun createConverter(validator: IValidator<T>): ValidatorConversionFunction<List<String>, T> {
        return validator.buildConvertFrom<List<String>, T>()
    }
}

private val ParsedBodyJson = AttributeKey<Pair<JsonElement, Boolean>>("ParsedBodyJsonObject")

class JsonBodyExtractor<T>(private val path: DataExtractorPath?) : DataExtractor<JsonElement, T> {
    private suspend fun getRawData(call: ApplicationCall): Pair<JsonElement, Boolean> {
        var data = call.attributes.getOrNull(ParsedBodyJson)
        if (data === null) {
            data = try {
                Json.parseToJsonElement(call.receiveText()) to true
            } catch (e: SerializationException) {
                JsonNull to false
            }
            call.attributes.put(ParsedBodyJson, data!!)
        }
        return data
    }
    override suspend fun extract(call: ApplicationCall) = run {
        val (data, isSet) = getRawData(call)
        if (!isSet) return@run JsonNull to false

        if (path.isNullOrEmpty()) return@run data to true

        var elem: JsonElement = data
        for (p in path) {
            val tempElem = elem
            if (p.isName) {
                if (tempElem !is JsonObject || tempElem[p.path!!]?.also { elem = it } === null) {
                    return@run JsonNull to false
                }
            } else {
                if (tempElem !is JsonArray || tempElem.getOrNull(p.idx!!)?.also { elem = it } === null) {
                    return@run JsonNull to false
                }
            }
        }
        elem to true
    }

    override fun createConverter(validator: IValidator<T>): ValidatorConversionFunction<JsonElement, T> {
        return validator.buildConvertFrom<JsonElement, T>()
    }
}
