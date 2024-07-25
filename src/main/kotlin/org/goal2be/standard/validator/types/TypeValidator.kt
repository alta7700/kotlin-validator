package org.goal2be.standard.validator.types

import org.goal2be.standard.validator.IndependentValidator
import org.goal2be.standard.validator.ValidatorMetaCollection
import org.goal2be.standard.validator.ValidatorMetaCollectionBuilder
import kotlin.reflect.KType

abstract class TypeValidator<T>(meta: ValidatorMetaCollection<T>, thisType: KType) : IndependentValidator<T>(meta, thisType) {
    fun copyWith(build: ValidatorMetaCollectionBuilder<T>.() -> ValidatorMetaCollectionBuilder<T>) : TypeValidator<T> {
        return copyWith(ValidatorMetaCollectionBuilder<T>().build().toMetaCollection()) as TypeValidator<T>
    }
}