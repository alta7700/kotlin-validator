@file:Suppress("UNUSED")
package org.goal2be.standard.validator.meta

import org.goal2be.standard.validator.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf


inline fun <reified F, T> ValidatorMetaCollectionBuilder<T>.converterFor(noinline func: ValidatorConversionFunction<F, T>) = add(Converter(typeOf<F>(), func))
inline fun <reified F, T> DataClassValidatorBuilder<T>.converterFor(noinline func: ValidatorConversionFunction<F, T>) = meta(Converter(typeOf<F>(), func))
class Converter<T, F>(val type: KType, val func: ValidatorConversionFunction<F, T>) : ValidatorMeta<T>() {

    override fun shouldRewrite(other: ValidatorMeta<T>) = false
    override fun patchValidator(validator: RealValidator<T>) {

        validator.convertFrom(type, func)
    }
}
