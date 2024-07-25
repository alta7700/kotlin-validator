package org.goal2be.standard.validator.types

import org.goal2be.standard.validator.NoPatchValidatorMeta
import org.goal2be.standard.validator.RealValidator
import org.goal2be.standard.validator.ValidatorMetaCollectionBuilder


fun <T: Any?> ValidatorMetaCollectionBuilder<T>.useValidator(validator: RealValidator<T>) = add(DefinedValidatorMeta(validator))
class DefinedValidatorMeta<T: Any?>(val validator: RealValidator<T>) : NoPatchValidatorMeta<T>() {
    override val displayParams = listOf("validator")
}

fun ValidatorTypesRegistry.initDefined() {
    addMatcher { type, meta ->
        with(meta.findMeta<DefinedValidatorMeta<Any?>>()) {
            this?.validator?.copyWith(meta.without(this))
        }
    }
}
