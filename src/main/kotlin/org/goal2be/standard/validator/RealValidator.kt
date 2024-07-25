@file:Suppress("UNUSED")
package org.goal2be.standard.validator

import org.goal2be.standard.validator.exceptions.NonNullableException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlin.reflect.KType
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.withNullability
import kotlin.reflect.typeOf

abstract class RealValidator<T>(
    internal val meta: ValidatorMetaCollection<T>,
    internal val thisType: KType,
) : IValidator<T> {
    internal fun applyMeta() {
        meta.forEach { it.patchValidator(this) }
    }

    internal open var defaultFactory: (() -> T)? = null
    override var nullValue: T? = null
    override val required get() = !hasDefault
    override val hasDefault get() = defaultFactory !== null
    override fun getDefault(): T = (defaultFactory ?: throw Exception("$this has not default factory"))()
    override fun setDefault(factory: () -> T) { defaultFactory = factory }

    override fun copyWith(newMeta: ValidatorMetaCollection<T>): RealValidator<T> {
        return this::class.primaryConstructor!!.call(meta.copyWith(newMeta), thisType)
    }

    val beforeCheckTransforms = ValidatorTransformCollection<T>()
    val afterCheckTransforms = ValidatorTransformCollection<T>()
    val constraintChecks = ValidatorConstraintCheckCollection<T>()

    open fun transformBeforeChecks(value: T & Any): T & Any {
        var result = value
        beforeCheckTransforms.forEach { result = it(value, this) }
        return result
    }

    open fun checkConstraints(value: T & Any) = constraintChecks.forEach { it(value, this) }

    open fun transformAfterChecks(value: T & Any): T & Any {
        var result = value
        afterCheckTransforms.forEach { result = it(value, this) }
        return result
    }
    inline fun <reified F> buildConvertFrom() = buildConvertFrom<F>(typeOf<F>())

    final override fun <F> buildConvertFrom(type: KType): ValidatorConversionFunction<F, T> {
        val nonNullableType: KType = if (type.isMarkedNullable) type.withNullability(false) else type
        val nonNullableConverter: ValidatorConversionFunction<F & Any, T> =
            if (nonNullableType == thisType.withNullability(false)) { {
                @Suppress("UNCHECKED_CAST")
                it as T
            } }
            else buildNNConvertFrom<F>(nonNullableType)
        return wrapNullableConverter(nonNullableType, wrapConverterToValidator(nonNullableConverter))
    }
    internal abstract fun <F: Any?> buildNNConvertFrom(type: KType): ValidatorConversionFunction<F & Any, T>

    internal fun <F> wrapNullableConverter(
        nonNullableConvertFromType: KType,
        converter: ValidatorConversionFunction<F & Any, T>
    ) : ValidatorConversionFunction<F, T> {
        if (nonNullableConvertFromType != AnyKType && nonNullableConvertFromType.isSupertypeOf(JsonElementKType)) {
            if (thisType.isMarkedNullable) {
                return { value ->
                    @Suppress("UNCHECKED_CAST")
                    if (isJsonNull(value as JsonElement)) nullValue as T
                    else converter(value)
                }
            } else {
                return { value ->
                    throwIfJsonNull(value as JsonElement)
                    converter(value)
                }
            }
        } else {
            if (thisType.isMarkedNullable) {
                return { value ->
                    @Suppress("UNCHECKED_CAST")
                    if (isNull(value)) nullValue as T
                    else converter(value!!)
                }
            } else {
                return { value ->
                    throwIfNull(value)
                    converter(value!!)
                }
            }
        }
    }
    private fun throwIfNull(value: Any?) {
        if (isNull(value)) throw NonNullableException
    }
    private fun throwIfJsonNull(value: JsonElement) {
        if (isJsonNull(value)) throw NonNullableException
    }
    private fun isNull(value: Any?): Boolean = value === null
    private fun isJsonNull(value: JsonElement): Boolean = value is JsonNull

    fun <F> wrapConverterToValidator(nonNullableConverter: ValidatorConversionFunction<F & Any, T>) : ValidatorConversionFunction<F & Any, T> {
        return { value ->
            val convertedValue = nonNullableConverter(value)
            if (convertedValue == nullValue) convertedValue
            else transformBeforeChecks(convertedValue!!).also(::checkConstraints).let(::transformAfterChecks)
        }
    }
}

private val AnyKType = typeOf<Any>()
private val JsonElementKType = typeOf<JsonElement>()

fun <T> ValidatorMetaCollectionBuilder<T>.nullValue(value: T) = add(NullValueChanger(value))
class NullValueChanger<T>(val nullValue: T) : ValidatorMeta<T>() {
    override val displayParams = listOf("nullValue")
    override fun patchValidator(validator: RealValidator<T>) {
        validator.nullValue = nullValue
    }
}
