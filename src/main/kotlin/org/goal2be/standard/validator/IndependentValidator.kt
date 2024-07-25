package org.goal2be.standard.validator

import kotlin.reflect.KType
import kotlin.reflect.typeOf

abstract class IndependentValidator<T>(
    meta: ValidatorMetaCollection<T>,
    thisType: KType,
) : RealValidator<T>(meta, thisType) {

    private val converters = mutableMapOf<KType, ValidatorConversionFunction<*, T>>()
    inline fun <reified F: Any> converterFor(noinline converter: ValidatorConversionFunction<F, T>) = converterFor(typeOf<F>(), converter)
    fun <F> converterFor(type: KType, converter: ValidatorConversionFunction<F, T>) { converters[type] = converter }
    inline fun <reified F: Any> getConverterFor() : ValidatorConversionFunction<F, T>? = getConverterFor(typeOf<F>())
    @Suppress("UNCHECKED_CAST")
    fun <F> getConverterFor(type: KType) : ValidatorConversionFunction<F, T>? = converters[type] as ValidatorConversionFunction<F, T>?

    override fun <F: Any?> buildNNConvertFrom(type: KType): ValidatorConversionFunction<F & Any, T> {
        assert (!type.isMarkedNullable)
        return getConverterFor(type) ?: throw Exception("$this don't has registered converter for $type")
    }
}