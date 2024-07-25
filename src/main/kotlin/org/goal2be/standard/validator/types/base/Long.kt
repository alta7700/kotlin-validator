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
class LongValidator<T: Long?>(meta: ValidatorMetaCollection<T>, thisType: KType) : TypeValidator<T>(meta, thisType) {
    init {
        converterFor<Any> { when (it) {
            is Long -> it as T
            is Number -> it.toLong() as T
            is String -> longFromString(it) as T
            else -> throw cantConvertToLongException
        } }
        converterFor<Long> { it as T }
        converterFor<Int> { it.toLong() as T }
        converterFor<Short> { it.toLong() as T }
        converterFor<String> { longFromString(it) as T }
        converterFor<JsonElement> {
            if (it !is JsonPrimitive) throw longIncorrectTypeException
            longFromString(it.content) as T
        }

        applyMeta()
    }
}

private fun longFromString(value: String) : Long = value.toLongOrNull() ?: throw cantConvertToLongException

private val cantConvertToLongException = IncorrectTypeException(message = "Can't convert to an BigInteger")
private val longIncorrectTypeException = IncorrectTypeException(message = "Value must be an BigInteger")

fun ValidatorTypesRegistry.initLong() {
    val longKType = typeOf<Long>()
    addMatcher<Long?> { type, meta ->
        if (type.isSupertypeOf(longKType)) LongValidator(meta, type)
        else null
    }
}

fun <T: Long?>ValidatorMetaCollectionBuilder<T>.lt(maxValue: Long, priority: Int = 10) = add(LessThan(maxValue, false, priority))
fun <T: Long?>ValidatorMetaCollectionBuilder<T>.gt(minValue: Long, priority: Int = 10) = add(GreaterThan(minValue, false, priority))
fun <T: Long?>ValidatorMetaCollectionBuilder<T>.lte(maxValue: Long, priority: Int = 10) = add(LessThan(maxValue, true, priority))
fun <T: Long?>ValidatorMetaCollectionBuilder<T>.gte(minValue: Long, priority: Int = 10) = add(GreaterThan(minValue, true, priority))
