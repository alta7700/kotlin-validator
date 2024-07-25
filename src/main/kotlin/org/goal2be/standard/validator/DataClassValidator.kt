@file:Suppress("UNUSED")
package org.goal2be.standard.validator

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.goal2be.standard.validator.exceptions.IncorrectTypeException
import org.goal2be.standard.validator.exceptions.RequiredValueException
import org.goal2be.standard.validator.exceptions.ValidationExceptionList
import org.goal2be.standard.validator.exceptions.accumulateValidationException
import org.goal2be.standard.validator.meta.Default
import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

class DataClassValidator<T>(
    thisType: KType,
    meta: ValidatorMetaCollection<T>,
    private val fieldsMeta: Map<KProperty1<T & Any, *>, ValidatorMetaCollection<*>>,
    private val fieldsAliases: Map<KProperty1<T & Any, *>, String>,
    private val realLocations: Map<KProperty1<T & Any, *>, List<String>>,
) : IndependentValidator<T>(meta, thisType) {

    companion object {
        inline fun <reified T> create(
            name: String = "default",
            noinline build: (DataClassValidatorBuilder<T>.() -> DataClassValidatorBuilder<T>)? = null
        ): DataClassValidator<T> {
            return DataClassValidatorBuilder<T>(name, typeOf<T>()).also { build?.invoke(it) }.toModelValidator()
        }
    }

    val dataClass: DataClassType<T> = DataClassType(thisType)
    val defaultedFields by lazy {  // lazy because validator can be extended (copied) later with new defaults
        dataClass.defaultedParams.map { param ->
            val default = fieldsMeta.entries.find { it.key.name == param.name }?.value?.find { it is Default<*> } as Default<*>?
            if (default === null) error("Property ${param.name} is DefaultOnly, but has no default")
            param to default.factory
        }
    }
    val fieldValidators: List<FieldValidatorWrapper<*>> = dataClass.params.map { (key, param) ->
        @Suppress("UNCHECKED_CAST")
        val fieldMeta = (fieldsMeta.entries.find { it.key.name == key }?.value?.copyWith()
            ?: ValidatorMetaCollection()) as ValidatorMetaCollection<Any?>
        val alias = fieldsAliases.entries.find { it.key.name == key }?.value ?: key
        val realLocation = realLocations.entries.find { it.key.name == key }?.value ?: listOf(alias)
        FieldValidatorWrapper(
            name = key,
            alias = alias,
            realLocation = realLocation,
            optional = param.isOptional,
            validator = ValidatorTypesRegistry.match(dataClass.getParamType(param), fieldMeta)
        )
    }.toList()

    override fun copyWith(newMeta: ValidatorMetaCollection<T>) = copyWith {
        meta(*newMeta.toTypedArray())
    }
    fun copyWith(
        build: DataClassValidatorBuilder<T>.() -> DataClassValidatorBuilder<T>
    ) = DataClassValidatorBuilder<T>(DataClassValidatorBuilder.DO_NOT_APPLY_PROPERTY_BUILDS, thisType).apply {
        fieldsMeta.forEach {
            @Suppress("UNCHECKED_CAST")
            field(it.key, it.value as ValidatorMetaCollection<Any?>)
        }
        fieldsAliases.forEach { alias(it.key, it.value) }
        meta.forEach { meta(it) }
        build()
    }.toModelValidator()

    fun buildConvertFromMapStringToBuiltConverter(
        valueConverters: List<Pair<FieldValidatorWrapper<*>, ValidatorConversionFunction<Any?, Any?>>>
    ) : ValidatorConversionFunction<Map<String, *>, T> {
        return { value ->
            val excs = ValidationExceptionList()
            val resultData = mutableMapOf<String, Any?>()
            valueConverters.forEach { (field, converter) ->
                if (field.alias in value) {
                    accumulateValidationException(excs, field.realLocation, value) {
                        resultData[field.name] = converter(value[field.alias])
                    }
                } else {
                    if (field.required) {
                        excs += RequiredValueException(
                            location = field.realLocation,
                            input = "__UNSET__"
                        )
                    } else {
                        if (field.hasDefault) {
                            resultData[field.name] = field.getDefault()
                        }
                    }
                }
            }
            if (excs.isNotEmpty()) throw excs
            dataClass.create(resultData, defaultedFields.associate { it.first to it.second() })
        }
    }

    private fun <F: Any?> buildConvertFromMapStringToSingle(type: KType) : ValidatorConversionFunction<Map<String, F>, T> {
        val valueConverters = this.fieldValidators.map {
            @Suppress("UNCHECKED_CAST")
            it to it.buildConvertFrom<F>(type) as ValidatorConversionFunction<Any?, Any?>
        }
        return buildConvertFromMapStringToBuiltConverter(valueConverters)
    }

    fun buildConvertFromMapStringToPolymorphic(typesMap: Map<String, KType>) : ValidatorConversionFunction<Map<String, Any?>, T> {
        val valueConverters = this.fieldValidators.map {
            it to it.buildConvertFrom<Any?>(typesMap[it.name]
                ?: throw Error("There is no converting type found for \"${it.name}\" in validator \"$thisType\""))
        }
        return buildConvertFromMapStringToBuiltConverter(valueConverters)
    }

    override fun <F: Any?> buildNNConvertFrom(type: KType): ValidatorConversionFunction<F & Any, T> {
        if (type.jvmErasure == Map::class) {
            val keyType = type.arguments[0].type
            if (keyType == typeOf<String>()) {
                @Suppress("UNCHECKED_CAST")
                return buildConvertFromMapStringToSingle<Any>(
                    type.arguments[1].type?.withNullability(false)
                        ?: typeOf<Any>()
                ) as ValidatorConversionFunction<F & Any, T>
            }
        }
        return super.buildNNConvertFrom(type)
    }

    init {
        val cantConvertToThis = IncorrectTypeException(message = "Can't convert to ${dataClass.name}")

        converterFor<JsonElement> { value ->
            if (value !is JsonObject) throw cantConvertToThis
            val fromJsonObjectConverter = buildConvertFrom<Map<String, JsonElement>>()
            fromJsonObjectConverter(value)
        }
        converterFor<JsonObject> { value ->
            val fromJsonObjectConverter = buildConvertFrom<Map<String, JsonElement>>()
            fromJsonObjectConverter(value)
        }

        converterFor<Any> { value ->
            if (value !is Map<*, *>) throw cantConvertToThis
            val fromAnyConverter = buildConvertFrom<Map<String, Any?>>()
            fromAnyConverter(value.entries.associate { it.key.toString() to it.value })
        }

        applyMeta()
    }

}

class FieldValidatorWrapper<T>(
    val name: String,
    val alias: String,
    val realLocation: List<String>,
    val optional: Boolean,
    val validator: IValidator<T>,
) : IValidator<T> by validator {
    override val required: Boolean get() = !optional && validator.required
    override fun copyWith(newMeta: ValidatorMetaCollection<T>): IValidator<T> {
        return FieldValidatorWrapper(
            name = name,
            alias = alias,
            realLocation = realLocation,
            optional = optional,
            validator = validator.copyWith(newMeta),
        )
    }
}

class DataClassValidatorBuilder<T>(
    private val name: String,
    private val type: KType,
) {
    companion object {
        const val DO_NOT_APPLY_PROPERTY_BUILDS = "_do_not_apply_prop_builds_"
    }

    private val fields = mutableMapOf<KProperty1<T & Any, *>, ValidatorMetaCollection<*>>()
    private val aliases = mutableMapOf<KProperty1<T & Any, *>, String>()
    private val realLocations = mutableMapOf<KProperty1<T & Any, *>, List<String>>()
    private val metaList = mutableListOf<ValidatorMeta<T>>()

    init {
        if (name !== DO_NOT_APPLY_PROPERTY_BUILDS) {
            @Suppress("UNCHECKED_CAST")
            type.jvmErasure.companionObject?.let {
                val companionInstance = type.jvmErasure.companionObjectInstance!!
                val validatorBuildAlwaysProp = it.memberProperties.find {
                    prop -> prop.findAnnotation<DCValidatorBuildAlways>() !== null
                } as KProperty1<Any, DataClassValidatorBuilder<T>.() -> Unit>?
                val validatorBuildProp = it.memberProperties.find {
                    prop -> prop.findAnnotation<DCValidatorBuild>()?.name == name
                } as KProperty1<Any, DataClassValidatorBuilder<T>.() -> Unit>?
                validatorBuildAlwaysProp?.get(companionInstance)?.invoke(this)
                validatorBuildProp?.get(companionInstance)?.invoke(this)
            }
        }
    }

    fun toModelValidator() = DataClassValidator(
        type,
        ValidatorMetaCollection(metaList),
        fields.toMap(),
        aliases.toMap(),
        realLocations.toMap()
    )

    fun <F> field(
        prop: KProperty1<T & Any, F>,
        build: (ValidatorMetaCollectionBuilder<F>.() -> ValidatorMetaCollectionBuilder<F>)? = null
    ) = field(prop, prop.name, build)
    fun <F> field(
        prop: KProperty1<T & Any, F>,
        alias: String,
        build: (ValidatorMetaCollectionBuilder<F>.() -> ValidatorMetaCollectionBuilder<F>)? = null
    ) = field(prop, alias, ValidatorMetaCollectionBuilder<F>().also { build?.invoke(it) }.toMetaCollection())

    fun <F> field(
        prop: KProperty1<T & Any, F>,
        metaCollection: ValidatorMetaCollection<F>
    ) = field(prop, prop.name, metaCollection)

    fun <F> field(
        prop: KProperty1<T & Any, F>,
        alias: String,
        metaCollection: ValidatorMetaCollection<F>
    ) : DataClassValidatorBuilder<T> {
        val existedCollection = fields[prop]
        if (existedCollection === null) {
            fields[prop] = metaCollection
        } else {
            @Suppress("UNCHECKED_CAST")
            fields[prop] = (existedCollection as ValidatorMetaCollection<F>).copyWith(metaCollection)
        }
        alias(prop, alias)
        return this
    }

    fun field(
        prop: KProperty1<T & Any, *>,
        alias: String
    ) = alias(prop, alias)

    fun alias(field: KProperty1<T & Any, *>, alias: String) : DataClassValidatorBuilder<T> {
        aliases[field] = alias
        return this
    }

    fun loc(field: KProperty1<T & Any, *>, location: String) = loc(field, listOf(location))
    fun loc(field: KProperty1<T & Any, *>, location: List<String>) : DataClassValidatorBuilder<T> {
        realLocations[field] = location
        return this
    }

    fun meta(vararg elements: ValidatorMeta<T>) : DataClassValidatorBuilder<T> {
        metaList.addAll(elements)
        return this
    }
    fun meta(collection: ValidatorMetaCollection<T>) : DataClassValidatorBuilder<T> {
        metaList.addAll(collection.toList())
        return this
    }
}

fun ValidatorTypesRegistry.initDataClasses() {
    addMatcher { type, meta ->
        val clazz = type.jvmErasure
        @Suppress("UNCHECKED_CAST")
        if (clazz.isData) {
            val validatorName = meta.findMeta<DCValidatorNameMeta<Any?>>()?.name ?: "default"
            val validator = clazz.companionObject?.let {
                val validatorProp = it.memberProperties.find {
                    prop -> prop.findAnnotation<DCValidator>()?.name == validatorName
                } as KProperty1<Any, *>?
                if (validatorProp !== null) {
                    val validator = validatorProp.get(clazz.companionObjectInstance!!)
                    check(validator is DataClassValidator<*>)
                    validator as DataClassValidator<Any?>
                    if (meta.isEmpty()) validator
                    else validator.copyWith(meta)
                } else null
            }
            if (validator !== null) validator
            else DataClassValidatorBuilder<Any?>(validatorName, type).apply {
                meta(meta)
            }.toModelValidator()
        }
        else null
    }
}

fun <T: Any?> ValidatorMetaCollectionBuilder<T>.useValidatorName(name: String) = add(DCValidatorNameMeta(name))
class DCValidatorNameMeta<T: Any?>(val name: String) : NoPatchValidatorMeta<T>() {
    override val displayParams = listOf("name")
}

@Target(AnnotationTarget.PROPERTY)
annotation class DCValidator(val name: String = "default")

@Target(AnnotationTarget.PROPERTY)
annotation class DCValidatorBuild(val name: String = "default")
@Target(AnnotationTarget.PROPERTY)
annotation class DCValidatorBuildAlways

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class DefaultedOnly