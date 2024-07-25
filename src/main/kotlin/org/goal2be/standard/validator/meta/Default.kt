@file:Suppress("UNUSED")
package org.goal2be.standard.validator.meta

import org.goal2be.standard.validator.DataClassValidatorBuilder
import org.goal2be.standard.validator.RealValidator
import org.goal2be.standard.validator.ValidatorMeta
import org.goal2be.standard.validator.ValidatorMetaCollectionBuilder

fun <T: Any?> ValidatorMetaCollectionBuilder<T>.default(value: T) = add(Default(value))
fun <T: Any?> DataClassValidatorBuilder<T>.default(value: T) = meta(Default(value))
fun <T: Any?> ValidatorMetaCollectionBuilder<T>.defaultFactory(factory: () -> T) = add(Default(factory))
fun <T: Any?> DataClassValidatorBuilder<T>.defaultFactory(factory: () -> T) = meta(Default(factory))
class Default<T: Any?>(val factory: () -> T) : ValidatorMeta<T>() {
    constructor(value: T): this({ value }) {
        isStatic = true
    }
    private var isStatic: Boolean = false
    override val displayParams = if (isStatic) listOf("value") else listOf("exampleValue=")
    override fun patchValidator(validator: RealValidator<T>) {
        validator.setDefault(factory)
    }
    val exampleValue get() = factory()
}
