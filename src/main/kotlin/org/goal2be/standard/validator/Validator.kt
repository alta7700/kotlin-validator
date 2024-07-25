package org.goal2be.standard.validator

import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface IValidator<T> {

    var nullValue: T?
    val required: Boolean
    val hasDefault: Boolean
    fun getDefault(): T
    fun setDefault(factory: () -> T)

    fun copyWith(newMeta: ValidatorMetaCollection<T>) : IValidator<T>

    fun <F> buildConvertFrom(type: KType) : ValidatorConversionFunction<F, T>
    fun <F> convertFrom(type: KType, value: F) : T = buildConvertFrom<F>(type)(value)
}
inline fun <reified F, T> IValidator<T>.buildConvertFrom() = buildConvertFrom<F>(typeOf<F>())
inline fun <reified F, T> IValidator<T>.convertFrom(value: F) = convertFrom(typeOf<F>(), value)
