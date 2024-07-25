@file:Suppress("UNUSED")
package org.goal2be.standard.validator.types.base

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import org.goal2be.standard.validator.*
import org.goal2be.standard.validator.exceptions.IncorrectTypeException
import org.goal2be.standard.validator.exceptions.ValidationExceptionList
import org.goal2be.standard.validator.exceptions.accumulateValidationException
import org.goal2be.standard.validator.types.TypeValidator
import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

class MapValidator<T: Map<String, V>?, V>(
    meta: ValidatorMetaCollection<T>,
    thisType: KType,
) : TypeValidator<T>(meta, thisType) {
    val valueValidator = thisType.arguments[1].type?.let {
        ValidatorTypesRegistry.match(it, meta.findMeta<MapValuesMeta<T, V>>()?.meta ?: ValidatorMetaCollection())
    } ?: error("Map value type parameter is not set. Can't apply find valueValidator")

    private fun <F> buildConvertFromMapStringOf(type: KType) : ValidatorConversionFunction<Map<String, F>, T> {
        val valueConverter = valueValidator.buildConvertFrom<Any?>(type)
        return { value ->
            val excs = ValidationExceptionList()
            val result = mutableMapOf<String, V>()
            value.map { (key, item) ->
                accumulateValidationException(excs, key, item) {
                    result[key] = valueConverter(item)
                }
            }
            if (excs.isNotEmpty()) throw excs
            @Suppress("UNCHECKED_CAST")
            result.toMap() as T
        }
    }
    override fun <F: Any?> buildNNConvertFrom(type: KType): ValidatorConversionFunction<F & Any, T> {
        if (type.jvmErasure == Map::class && type.arguments[0].type == typeOf<String>()) {
            @Suppress("UNCHECKED_CAST")
            return buildConvertFromMapStringOf<Any?>(
                type.arguments[1].type?.withNullability(false)
                    ?: typeOf<Any>()
            ) as ValidatorConversionFunction<F, T>
        }
        return super.buildNNConvertFrom(type)
    }

    init {
        converterFor<Any> { value ->
            if (value !is Map<*, *>) throw cantConvertToMapException
            val fromAnyConverter = buildConvertFrom<Map<String, Any?>>()
            fromAnyConverter(value.entries.associate { it.key.toString() to it.value })
        }

        converterFor<JsonElement> { value ->
            if (value !is JsonObject) throw cantConvertToMapException
            val fromJsonObjectConverter = buildConvertFrom<Map<String, JsonElement>>()
            fromJsonObjectConverter(value)
        }
        converterFor<JsonObject> { value ->
            val fromJsonObjectConverter = buildConvertFrom<Map<String, JsonElement>>()
            fromJsonObjectConverter(value)
        }

        applyMeta()
    }
}
private val cantConvertToMapException = IncorrectTypeException(message = "Value can't be converted to map")

fun ValidatorTypesRegistry.initMap() {
    addMatcher<Map<String, *>> { type, meta ->
        if (type.jvmErasure == Map::class && type.arguments[0].type == typeOf<String>()) {
            MapValidator(meta, type)
        }
        else null
    }
}

fun <T: Map<String, V>?, V> ValidatorMetaCollectionBuilder<T>.valuesMeta(
    build: ValidatorMetaCollectionBuilder<V>.() -> ValidatorMetaCollectionBuilder<V>
) = add(MapValuesMeta(build))
class MapValuesMeta<T: Map<String, V>?, V>(val meta: ValidatorMetaCollection<V>) : NoPatchValidatorMeta<T>() {

    override val displayParams = listOf("meta")
    constructor (
        build: ValidatorMetaCollectionBuilder<V>.() -> ValidatorMetaCollectionBuilder<V>
    ): this(ValidatorMetaCollectionBuilder<V>().build().toMetaCollection())

    override fun shouldRewrite(other: ValidatorMeta<T>) = false
    override fun shouldJoinTo(other: ValidatorMeta<T>) = other is MapValuesMeta<*, *>
    override fun joinTo(other: ValidatorMeta<T>): ValidatorMeta<T> {
        @Suppress("UNCHECKED_CAST")
        other as MapValuesMeta<T, V>
        return MapValuesMeta(other.meta.copyWith(this.meta))
    }
}
