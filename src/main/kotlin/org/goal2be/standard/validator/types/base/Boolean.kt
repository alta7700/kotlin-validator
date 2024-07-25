package org.goal2be.standard.validator.types.base

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.goal2be.standard.validator.ValidatorConversionFunction
import org.goal2be.standard.validator.ValidatorMetaCollection
import org.goal2be.standard.validator.exceptions.IncorrectTypeException
import org.goal2be.standard.validator.types.TypeValidator
import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

@Suppress("UNCHECKED_CAST")
class BooleanValidator<T: Boolean?>(meta: ValidatorMetaCollection<T>, thisType: KType) : TypeValidator<T>(meta, thisType) {
    init {
        converterFor<Any> { value ->
            when (value) {
                is Boolean -> value as T
                is String -> boolFromString(value) as T
                is Int -> boolFromInt(value) as T
                else -> throw cantConvertToBooleanException
            }
        }
        converterFor<Int>(::boolFromInt as ValidatorConversionFunction<Int, T>)
        converterFor<String>(::boolFromString as ValidatorConversionFunction<String, T>)
        converterFor<JsonElement> { value ->
            if (value !is JsonPrimitive) throw cantConvertToBooleanException
            boolFromString(value.content) as T
        }

        applyMeta()
    }
}

fun ValidatorTypesRegistry.initBoolean() {
    val booleanKType = typeOf<Boolean>()
    addMatcher<Boolean?> { type, meta ->
        if (type.isSubtypeOf(booleanKType)) BooleanValidator(meta, type)
        else null
    }
}

private val cantConvertToBooleanException = IncorrectTypeException(message = "Can't convert to a boolean")

private val stringTrue = setOf("true", "1", "True", "on", "yes")
private val stringFalse = setOf("false", "0", "False", "off", "no")
private fun boolFromString(value: String) = when (value) {
    in stringTrue -> true
    in stringFalse -> false
    else -> throw cantConvertToBooleanException
}
private fun boolFromInt(value: Int) = when (value) {
    1 -> true
    0 -> false
    else -> throw cantConvertToBooleanException
}
