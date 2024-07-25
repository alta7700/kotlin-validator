package org.goal2be.standard.validator.types

import org.goal2be.standard.validator.*
import org.goal2be.standard.validator.types.base.*
import org.goal2be.standard.validator.types.extended.initEnum
import org.goal2be.standard.validator.types.extended.initLocalDate
import org.goal2be.standard.validator.types.extended.initOptional
import org.goal2be.standard.validator.types.extended.initUuid
import kotlin.reflect.KType
import kotlin.reflect.typeOf

typealias ValidatorMatcher<T> = (type: KType, meta: ValidatorMetaCollection<T>) -> RealValidator<T>?

object ValidatorTypesRegistry {

    private val matcherRegistry = mutableListOf<ValidatorMatcher<*>>()
    @Suppress("UNCHECKED_CAST")
    fun <T> addMatcher(matcher: ValidatorMatcher<T>) = matcherRegistry.add(matcher as ValidatorMatcher<*>)
    fun <T> matchOrNull(type: KType, meta: ValidatorMetaCollection<T>): RealValidator<T>? {
        for (matcher in matcherRegistry) {
            val validator = matcher(type, meta)
            @Suppress("UNCHECKED_CAST")
            if (validator !== null) return validator as RealValidator<T>
        }
        return null
    }
    fun <T> match(type: KType, meta: ValidatorMetaCollection<T>): RealValidator<T> = matchOrNull(type, meta) ?: throw Error("No matches found for $type")
    inline fun <reified T> match(meta: ValidatorMetaCollection<T>? = null) = match(typeOf<T>(), meta ?: ValidatorMetaCollection())
    inline fun <reified T> match(buildMeta: ValidatorMetaCollectionBuilder<T>.() -> ValidatorMetaCollectionBuilder<T>) = match(ValidatorMetaCollectionBuilder<T>().buildMeta().toMetaCollection())
    operator fun invoke() {
        initDefined()
        initValidated()
        initDataClasses()

        initList()
        initMap()
        initBoolean()
        initLong()
        initInt()
        initShort()
        initString()

        initLocalDate()
        initEnum()
        initUuid()
        initOptional()
    }
}