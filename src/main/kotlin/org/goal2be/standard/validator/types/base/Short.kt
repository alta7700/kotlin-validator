@file:Suppress("UNUSED")
package org.goal2be.standard.validator.types.base

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.goal2be.standard.validator.ValidatorMetaCollection
import org.goal2be.standard.validator.ValidatorMetaCollectionBuilder
import org.goal2be.standard.validator.exceptions.IncorrectTypeException
import org.goal2be.standard.validator.meta.common.GreaterThan
import org.goal2be.standard.validator.meta.common.LessThan
import org.goal2be.standard.validator.types.TypeValidator
import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import kotlin.reflect.KType
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.typeOf

@Suppress("UNCHECKED_CAST")
class ShortValidator<T: Short?>(meta: ValidatorMetaCollection<T>, thisType: KType) : TypeValidator<T>(meta, thisType) {
    init {
        converterFor<Any> { when (it) {
            is Short -> it as T
            is Number -> it.toShort() as T
            is String -> shortFromString(it) as T
            else -> throw cantConvertToShortException
        } }
        converterFor<Long> { it.toShort() as T }
        converterFor<Int> { it.toShort() as T }
        converterFor<Short> { it as T }
        converterFor<String> { shortFromString(it) as T }
        converterFor<JsonElement> {
            if (it !is JsonPrimitive) throw shortIncorrectTypeException
            shortFromString(it.content) as T
        }

        applyMeta()
    }
}

private fun shortFromString(value: String) : Short = value.toShortOrNull() ?: throw cantConvertToShortException

private val cantConvertToShortException = IncorrectTypeException(message = "Can't convert to an SmallInteger")
private val shortIncorrectTypeException = IncorrectTypeException(message = "Value must be an SmallInteger")

fun ValidatorTypesRegistry.initShort() {
    val shortKType = typeOf<Short>()
    addMatcher<Short?> { type, meta ->
        if (type.isSupertypeOf(shortKType)) ShortValidator(meta, type)
        else null
    }
}

fun <T: Short?>ValidatorMetaCollectionBuilder<T>.lt(maxValue: Short, priority: Int = 10) = add(LessThan(maxValue, false, priority))
fun <T: Short?>ValidatorMetaCollectionBuilder<T>.gt(minValue: Short, priority: Int = 10) = add(GreaterThan(minValue, false, priority))
fun <T: Short?>ValidatorMetaCollectionBuilder<T>.lte(maxValue: Short, priority: Int = 10) = add(LessThan(maxValue, true, priority))
fun <T: Short?>ValidatorMetaCollectionBuilder<T>.gte(minValue: Short, priority: Int = 10) = add(GreaterThan(minValue, true, priority))
