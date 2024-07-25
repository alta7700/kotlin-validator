package org.goal2be.standard.validator.types.extended

import org.goal2be.standard.validator.ExtendedValidator
import org.goal2be.standard.validator.InternalMeta
import org.goal2be.standard.validator.ValidatorMetaCollection
import org.goal2be.standard.validator.ValidatorMetaCollectionBuilder
import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

sealed interface Optional<T: Any?> {
    class Value<T: Any?>(val value: T): Optional<T> {
        override fun equals(other: Any?): Boolean {
            return other is Value<*> && value == other.value
        }
        override fun hashCode(): Int = value.hashCode()
    }
    object UNSET : Optional<Any?>

    fun <R> ifSet(block: (value: T) -> R): R? {
        if (this is Value) {
            return block(value)
        }
        return null
    }

    fun isSet() = this is Value

    fun <R> ifUnset(block: () -> R): R? {
        if (this is UNSET) {
            return block()
        }
        return null
    }

}

class OptionalValidator<T: Any?>(
    meta: ValidatorMetaCollection<Optional<T>>,
    thisType: KType,
    override val internalType: KType,
) : ExtendedValidator<Optional<T>, T & Any>(meta, thisType) {

    @Suppress("UNCHECKED_CAST")
    override var nullValue: Optional<T>? = Optional.Value(null as T)

    @Suppress("UNCHECKED_CAST")
    override var defaultFactory: (() -> Optional<T>)? = { Optional.UNSET as Optional<T> }

    override fun convertFromInternal(value: T & Any): Optional<T> {
        return Optional.Value(value)
    }

    init {
        applyMeta()
    }
}

fun ValidatorTypesRegistry.initOptional() {
    addMatcher<Optional<Any?>> { type, meta ->
        if (type.isSubtypeOf(typeOf<Optional<*>>())) {
            val internalType = type.arguments[0].type
            if (internalType === null) throw Error("Internal type must be set")
            OptionalValidator(meta, type, internalType)
        }
        else null
    }
}

fun <T: Optional<I>, I: Any?> ValidatorMetaCollectionBuilder<T>.internal(
    build: ValidatorMetaCollectionBuilder<I>.() -> ValidatorMetaCollectionBuilder<I>
) = add(InternalMeta<T, I>(ValidatorMetaCollectionBuilder<I>().build().toMetaCollection()))
