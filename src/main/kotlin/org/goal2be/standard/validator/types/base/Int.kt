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
class IntValidator<T: Int?>(meta: ValidatorMetaCollection<T>, thisType: KType) : TypeValidator<T>(meta, thisType) {
    init {
        converterFor<Any> { when (it) {
            is Int -> it as T
            is Number -> it as T
            is String -> intFromString(it) as T
            else -> throw cantConvertToIntegerException
        } }
        converterFor<Int> { it as T }
        converterFor<String> { intFromString(it) as T }
        converterFor<JsonElement> {
            if (it !is JsonPrimitive) throw integerIncorrectTypeException
            intFromString(it.content) as T
        }

        applyMeta()
    }
}

private fun intFromString(value: String) : Int = value.toIntOrNull() ?: throw cantConvertToIntegerException

private val cantConvertToIntegerException = IncorrectTypeException(message = "Can't convert to an integer")
private val integerIncorrectTypeException = IncorrectTypeException(message = "Value must be an integer")

fun ValidatorTypesRegistry.initInt() {
    val intKType = typeOf<Int>()
    addMatcher<Int?> { type, meta ->
        if (type.isSupertypeOf(intKType)) IntValidator(meta, type)
        else null
    }
}

fun <T: Int?>ValidatorMetaCollectionBuilder<T>.lt(maxValue: Int, priority: Int = 10) = add(LessThan(maxValue, false, priority))
fun <T: Int?>ValidatorMetaCollectionBuilder<T>.gt(minValue: Int, priority: Int = 10) = add(GreaterThan(minValue, false, priority))
fun <T: Int?>ValidatorMetaCollectionBuilder<T>.lte(maxValue: Int, priority: Int = 10) = add(LessThan(maxValue, true, priority))
fun <T: Int?>ValidatorMetaCollectionBuilder<T>.gte(minValue: Int, priority: Int = 10) = add(GreaterThan(minValue, true, priority))
