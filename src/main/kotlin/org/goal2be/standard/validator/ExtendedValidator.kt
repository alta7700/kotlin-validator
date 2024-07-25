@file:Suppress("UNUSED")
package org.goal2be.standard.validator

import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.withNullability

abstract class ExtendedValidator<T, I: Any>(
    meta: ValidatorMetaCollection<T>,
    thisType: KType
) : RealValidator<T>(meta, thisType) {
    abstract val internalType: KType
    open val requiredInternalMeta: (ValidatorMetaCollectionBuilder<I>.() -> ValidatorMetaCollectionBuilder<I>)? = null

    private val internalValidator by lazy {
        ValidatorTypesRegistry.match(
            internalType,
            requiredInternalMeta?.let {
                meta.getInternalMeta<T, I>().copyWith{ ValidatorMetaCollectionBuilder<I>().it() }
            } ?: meta.getInternalMeta()
        )
    }

    override fun copyWith(newMeta: ValidatorMetaCollection<T>): ExtendedValidator<T, I> {
        @Suppress("UNCHECKED_CAST")
        return super.copyWith(newMeta) as ExtendedValidator<T, I>
    }
    fun copyWith(build: ValidatorMetaCollectionBuilder<T>.() -> ValidatorMetaCollectionBuilder<T>) : ExtendedValidator<T, I> {
        return copyWith(ValidatorMetaCollectionBuilder<T>().build().toMetaCollection())
    }

    override fun <F> buildNNConvertFrom(type: KType): ValidatorConversionFunction<F & Any, T> {
        assert (!type.isMarkedNullable)
        if (thisType.withNullability(false) == type) return {
            @Suppress("UNCHECKED_CAST")
            it as T
        }
        val internalConverter = internalValidator.buildConvertFrom<F>(type)
        return { value -> convertFromInternal(internalConverter(value)) }
    }

    abstract fun convertFromInternal(value: I): T
}

private fun <T, I> ValidatorMetaCollection<T>.getInternalMeta() : ValidatorMetaCollection<I> {
    return this.findMeta<InternalMeta<T, I>>()?.meta ?: ValidatorMetaCollection()
}

class InternalMeta<T, I>(val meta: ValidatorMetaCollection<I>) : ValidatorMeta<T>() {

    override val displayParams = listOf("meta")

    constructor (
        build: ValidatorMetaCollectionBuilder<I>.() -> ValidatorMetaCollectionBuilder<I>
    ): this(ValidatorMetaCollectionBuilder<I>().build().toMetaCollection())

    override fun shouldRewrite(other: ValidatorMeta<T>) = false
    override fun shouldJoinTo(other: ValidatorMeta<T>) = other is InternalMeta<*, *>
    override fun joinTo(other: ValidatorMeta<T>): ValidatorMeta<T> {
        @Suppress("UNCHECKED_CAST")
        other as InternalMeta<T, I>
        return this::class.primaryConstructor!!.call(other.meta.copyWith(this.meta))
    }
    override fun patchValidator(validator: RealValidator<T>) {
        if (validator !is ExtendedValidator<*, *>) throw Error("can be applied only to ExtendedValidator")
        // don't apply meta there, because it's applies on initializing
    }
}
