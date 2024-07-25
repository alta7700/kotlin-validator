package org.goal2be.standard.validator

import io.ktor.util.reflect.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.goal2be.standard.validator.exceptions.*
import org.goal2be.standard.validator.meta.common.choices
import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

class SealedDiscriminatorValidator<T: Any?>(
    meta: ValidatorMetaCollection<T>,
    thisType: KType,
) : IndependentValidator<T>(meta, thisType) {

    private val innerValidatorsMap: Map<String, RealValidator<T>> = run {
        @Suppress("UNCHECKED_CAST")
        val clazz = thisType.jvmErasure as KClass<T & Any>
        clazz.sealedSubclasses.let { subclasses ->
            val validatorsMap = meta.findMeta<DefinedInnerSealedValidator<T>>()?.tags ?: mapOf()
            val validatorsMetaMap = meta.findMeta<DefinedInnerSealedMeta<T>>()?.tagsMeta ?: mapOf()
            subclasses.associate {
                val tag = it.findAnnotation<DiscriminatorTag>() ?: error("DiscriminatorTag must be set in class $it")
                tag.name to validatorsMap.getOrElse(tag.name) {
                    ValidatorTypesRegistry.match(
                        it.createType(nullable = false),
                        validatorsMetaMap.getOrElse(tag.name) { ValidatorMetaCollection() }
                    )
                }
            }
        }
    }

    private val typePropValidator = ValidatorTypesRegistry.match<String> { choices(innerValidatorsMap.keys.toList()) }

    private fun buildConvertFromMapStringToBuiltConverter(
        typeConverter: ValidatorConversionFunction<Any?, String>,
        tagsConvertersMap: Map<String, ValidatorConversionFunction<Any?, T>>
    ): ValidatorConversionFunction<Map<String, Any?>, T> {
        assert(tagsConvertersMap.all { it.key in innerValidatorsMap } && (tagsConvertersMap.size == innerValidatorsMap.size))

        return { value ->
            try {

                val type: String = if ("type" in value) {
                    value["type"].let {
                        modifyValidationException(location = "type", input = it) {
                            typeConverter(it)
                        }
                    }
                } else {
                    throw RequiredValueException(location = "type", input = "__UNSET__")
                }
                if ("data" in value) {
                    value["data"].let {
                        modifyValidationException(location = "data", it) {
                            tagsConvertersMap[type]!!(it)
                        }
                    }
                } else {
                    throw RequiredValueException(location = "data", input = "__UNSET__")
                }
            } catch(exc: ValidationException) {
                throw(ValidationExceptionList(exc))
            }
        }
    }

    fun buildConvertInternalFromPolymorphic(
        typeFromType: KType,
        tagsMap: Map<String, KType>
    ): ValidatorConversionFunction<Map<String, Any?>, T> {
        val typeConverter = typePropValidator.buildConvertFrom<Any?>(typeFromType)
        val innerConvertersMap = innerValidatorsMap.map {
            it.key to it.value.buildConvertFrom<Any?>(tagsMap[it.key] ?: error("tag ${it.key} is not presented in $tagsMap"))
        }.toMap()
        return buildConvertFromMapStringToBuiltConverter(typeConverter, innerConvertersMap)
    }

    fun buildConvertInternalFrom(type: KType): ValidatorConversionFunction<Map<String, Any?>, T> {
        val typeConverter = typePropValidator.buildConvertFrom<Any?>(type)
        val valueConverters = innerValidatorsMap.map {
            it.key to it.value.buildConvertFrom<Any?>(type)
        }.toMap()
        return buildConvertFromMapStringToBuiltConverter(typeConverter, valueConverters)
    }

    override fun <F: Any?> buildNNConvertFrom(type: KType): ValidatorConversionFunction<F & Any, T> {
        if (type.jvmErasure == Map::class) {
            val keyType = type.arguments[0].type
            if (keyType == typeOf<String>()) {
                @Suppress("UNCHECKED_CAST")
                return buildConvertInternalFrom(
                    type.arguments[1].type?.withNullability(false)
                        ?: typeOf<Any>()
                ) as ValidatorConversionFunction<F & Any, T>
            }
        }
        return super.buildNNConvertFrom(type)
    }

    init {
        val cantConvertToThis = IncorrectTypeException(message = "Can't convert to $thisType.")

        converterFor<JsonElement> { value ->
            if (value !is JsonObject) throw cantConvertToThis
            val fromJsonObjectConverter = buildConvertFrom<Map<String, JsonElement>>()
            fromJsonObjectConverter(value)
        }
        converterFor<JsonObject> { value ->
            val fromJsonObjectConverter = buildConvertFrom<Map<String, JsonElement>>()
            fromJsonObjectConverter(value)
        }

        applyMeta()
    }
}

fun <T: Any?> ValidatorMetaCollectionBuilder<T>.forTag(name: String, validator: RealValidator<T>) =
    add(DefinedInnerSealedValidator(mapOf(name to validator)))
class DefinedInnerSealedValidator<T: Any?>(val tags: Map<String, RealValidator<T>>) : NoPatchValidatorMeta<T>() {
    override fun shouldRewrite(other: ValidatorMeta<T>): Boolean = false
    override fun shouldJoinTo(other: ValidatorMeta<T>): Boolean = other.instanceOf(this::class)
    override fun joinTo(other: ValidatorMeta<T>): ValidatorMeta<T> {
        return DefinedInnerSealedValidator((other as DefinedInnerSealedValidator<T>).tags + tags)
    }
}

fun <T: Any?> ValidatorMetaCollectionBuilder<T>.forTag(
    name: String, buildMeta: ValidatorMetaCollectionBuilder<T>.() -> ValidatorMetaCollectionBuilder<T>
) = add(DefinedInnerSealedMeta(mapOf(name to ValidatorMetaCollectionBuilder<T>().buildMeta().toMetaCollection())))
class DefinedInnerSealedMeta<T: Any?>(val tagsMeta: Map<String, ValidatorMetaCollection<T>>) : NoPatchValidatorMeta<T>() {
    override fun shouldRewrite(other: ValidatorMeta<T>): Boolean = false
    override fun shouldJoinTo(other: ValidatorMeta<T>): Boolean = other.instanceOf(this::class)
    override fun joinTo(other: ValidatorMeta<T>): ValidatorMeta<T> {
        other as DefinedInnerSealedMeta<T>
        val updatedMeta = tagsMeta.entries.associate {
            it.key to (other.tagsMeta[it.key]?.copyWith(it.value) ?: it.value)
        }
        return DefinedInnerSealedMeta(other.tagsMeta + updatedMeta)
    }
}

@Target(AnnotationTarget.CLASS)
annotation class DiscriminatorTag(val name: String)
