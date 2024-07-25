@file:Suppress("UNUSED")
package org.goal2be.standard.validator.types.extended

import org.goal2be.standard.validator.*
import org.goal2be.standard.validator.exceptions.ValidationException
import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import kotlin.reflect.*
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

class EnumValidator<T: Enum<*>?, I: Any>(
    meta: ValidatorMetaCollection<T>,
    enumType: KType,
) : ExtendedValidator<T, I>(meta, enumType) {
    @Suppress("UNCHECKED_CAST")
    private val mapper: EnumMapperI<T & Any, I> =
        meta.findMeta<EnumMapper<T, I>>()
            ?: DefaultEnumMapper.getFor(enumType.jvmErasure as KClass<T & Any>)
            ?: error("None of associatorMeta and defaultMapProperty are not set.")
    override val internalType: KType = mapper.mapType

    private val entriesMap: Map<I, T & Any> by lazy {
        @Suppress("UNCHECKED_CAST")
        val entries: List<T & Any> = meta.findMeta<EnumExcludeMeta<T>>()?.instances.let { exclude ->
            val allEnums = thisType.jvmErasure.java.enumConstants as Array<T & Any>
            if (exclude === null) allEnums.toList()
            else (thisType.jvmErasure.java.enumConstants as Array<T & Any>).filter { it !in exclude }
        }
        val associatorFunc = mapper.mapper
        assert(entries.map { associatorFunc(it)}.toSet().size == entries.size ) { "Associator results must be unique for $thisType" }
        entries.associateBy { associatorFunc(it) }
    }

    private val noEnumMatchException by lazy {
        ValidationException("enum_mismatch", "No enum values match for this value", ctx = mapOf("available" to entriesMap.entries.associate { it.value.name to it.key }))
    }

    var notFoundAction: () -> T = { throw noEnumMatchException }
    override fun convertFromInternal(value: I): T {
        return entriesMap[value] ?: notFoundAction()
    }
    init {
        applyMeta()
    }

}

fun ValidatorTypesRegistry.initEnum() {
    addMatcher { type, meta ->
        val clazz = type.jvmErasure
        if (clazz.java.isEnum) {
            EnumValidator<Enum<*>?, Any>(meta, type)
        }
        null
    }
}

fun <T: Enum<*>?> ValidatorMetaCollectionBuilder<T>.exclude(vararg instances: T & Any) = add(EnumExcludeMeta(instances.toList()))
class EnumExcludeMeta<T: Enum<*>?>(val instances: List<T & Any>) : NoPatchValidatorMeta<T>() {
    override val displayParams = listOf("instances")
}

fun <T: Enum<*>?> ValidatorMetaCollectionBuilder<T>.defaultIfNotFound(defaultValue: T & Any) = add(DefaultIfNotFound(defaultValue))
class DefaultIfNotFound<T: Enum<*>?>(val defaultValue: T) : ValidatorMeta<T>() {
    override val displayParams = listOf("defaultValue")

    override fun patchValidator(validator: RealValidator<T>) {
        assert(validator is EnumValidator<*, *>) { "This meta can be only applied only to ${EnumValidator::class}" }
        validator as EnumValidator<T, *>
        validator.notFoundAction = { defaultValue }
    }
}

interface EnumMapperI<T: Enum<*>, I> {
    val mapper: (T) -> I
    val mapType: KType
}

inline fun <T: Enum<*>?, reified I: Any> ValidatorMetaCollectionBuilder<T>.mapBy(noinline associator: (T & Any) -> I) = add(EnumMapper(associator, typeOf<I>()))
fun <T: Enum<*>?> ValidatorMetaCollectionBuilder<T>.mapByName() = mapBy { enum -> enum.name }
class EnumMapper<T: Enum<*>?, I>(override val mapper: (T & Any) -> I, override val mapType: KType)
    : NoPatchValidatorMeta<T>(), EnumMapperI<T & Any, I> {
    override val displayParams = listOf("mapper", "mapType")
}

class EnumMapperProperty<T: Enum<*>, I>(private val property: KProperty1<T, I>) : EnumMapperI<T, I> {
    override val mapper: (T) -> I = property::get
    override val mapType: KType = property.returnType
}

@Target(AnnotationTarget.PROPERTY)
annotation class DefaultEnumMapper {
    companion object {
        fun <E: Enum<*>, I> getFor(enumClazz: KClass<E>): EnumMapperProperty<E, I>? {
            return enumClazz.memberProperties.filter { it.hasAnnotation<DefaultEnumMapper>() }.let {
                assert(it.size <= 1) { error("Only 1 member can has annotation $DefaultEnumMapper") }
                @Suppress("UNCHECKED_CAST")
                (it.firstOrNull() as KProperty1<E, I>?)?.let(::EnumMapperProperty)
            }
        }
    }
}
