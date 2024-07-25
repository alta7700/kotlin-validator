@file:Suppress("UNUSED")
package org.goal2be.standard.validator.plugin

import io.ktor.server.application.*
import io.ktor.util.*
import kotlinx.serialization.json.JsonElement
import org.goal2be.standard.validator.*
import org.goal2be.standard.validator.exceptions.ValidationException
import org.goal2be.standard.validator.exceptions.ValidationExceptionList
import org.goal2be.standard.validator.meta.constraintCheck
import org.goal2be.standard.validator.meta.transformAfterCheck
import org.goal2be.standard.validator.meta.transformBeforeCheck
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

class RequestValidationData<T : Any>(
    dcValidator: DataClassValidator<T>,
    private val callParser: suspend ApplicationCall.() -> T
) {
    companion object {
        inline fun <reified T: Any>create(dcName: String = "default", noinline build: RequestValidationDataBuilder<T>.() -> Unit) : RequestValidationData<T> {
            return RequestValidationDataBuilder<T>(typeOf<T>(), dcName).also { build(it) }.toRequestValidationData()
        }
    }

    private val attrKey = AttributeKey<T>("CustomRequestValidation:${dcValidator.dataClass.name}")

    suspend fun parseCall(call: ApplicationCall) {
        try {
            putToCall(call, call.callParser())
        } catch (exc: ValidationException) {
            throw RequestValidationException(exc)
        } catch (excs: ValidationExceptionList) {
            throw RequestValidationException(excs)
        }
    }

    fun putToCall(call: ApplicationCall, data: T) {
        call.attributes.put(attrKey, data)
    }
    fun get(call: ApplicationCall) = call.attributes[attrKey]

}

class RequestValidationDataBuilder<T: Any>(val type: KType, dcName: String = "default") {
    private val dcBuilder = DataClassValidatorBuilder<T>(dcName, type)

    private val extractorBuilders = mutableMapOf<KProperty1<T, *>, DataExtractorBuilder<*, *>>()

    fun toRequestValidationData() : RequestValidationData<T> {
        val dcValidator = createDataClassValidator()
        val callParser = createCallParser(dcValidator)
        return RequestValidationData(dcValidator, callParser)
    }
    private fun createDataClassValidator() = dcBuilder.toModelValidator()
    private fun createCallParser(dcValidator: DataClassValidator<T>) : suspend ApplicationCall.() -> T {
        val extractors = mutableMapOf<String, DataExtractor<*, *>>()
        @Suppress("UNCHECKED_CAST")
        val fieldConverters = dcValidator.fieldValidators.map { fieldValidator ->
            val builder = extractorBuilders.entries.find { fieldValidator.name == it.key.name }?.value
                ?: throw Error("Path to data is not set in field \"${fieldValidator.name}\" of $type")
            val extractor: DataExtractor<Any?, Any?> = builder.toExtractor().also { extractors[fieldValidator.name] = it } as DataExtractor<Any?, Any?>
            fieldValidator to extractor.createConverter(fieldValidator as FieldValidatorWrapper<Any?>)
        }
        val dcConverter = dcValidator.wrapConverterToValidator(
            dcValidator.buildConvertFromMapStringToBuiltConverter(fieldConverters))
        return {
            val data = mutableMapOf<String, Any?>()
            extractors.forEach { (fieldName, extractor) ->
                extractor.extract(this).also { (value, isSet) ->
                    if (isSet) data[fieldName] = value
                }
            }
            try{ dcConverter(data) }
            catch (exc: ValidationException) {
                throw exc(input = data)
            }
        }
    }

    fun <F> pathRoot(field: KProperty1<T, F>) : RootStringValuesExtractorBuilder<T, F> {
        dcBuilder.loc(field, "path")
        return RootStringValuesExtractorBuilder(dcBuilder, field) { parameters }.also {
            extractorBuilders[field] = it
        }
    }
    fun <F> path(field: KProperty1<T, F>, name: String) : FieldStringValuesExtractorBuilder<T, F> {
        dcBuilder.loc(field, listOf("path", name))
        return FieldStringValuesExtractorBuilder(dcBuilder, field, name) { parameters }.also {
            extractorBuilders[field] = it
        }
    }
    fun <F> pathList(field: KProperty1<T, F>, name: String) : FieldListStringValuesExtractorBuilder<T, F> {
        dcBuilder.loc(field, listOf("path", name))
        return FieldListStringValuesExtractorBuilder(dcBuilder, field, name) { parameters }.also {
            extractorBuilders[field] = it
        }
    }

    fun <F> queryRoot(field: KProperty1<T, F>) : RootStringValuesExtractorBuilder<T, F> {
        dcBuilder.loc(field, "query")
        return RootStringValuesExtractorBuilder(dcBuilder, field) { request.queryParameters }.also {
            extractorBuilders[field] = it
        }
    }
    fun <F> query(field: KProperty1<T, F>, name: String) : FieldStringValuesExtractorBuilder<T, F> {
        dcBuilder.loc(field, listOf("query", name))
        return FieldStringValuesExtractorBuilder(dcBuilder, field, name) { request.queryParameters }.also {
            extractorBuilders[field] = it
        }
    }
    fun <F> queryList(field: KProperty1<T, F>, name: String) : FieldListStringValuesExtractorBuilder<T, F> {
        dcBuilder.loc(field, listOf("query", name))
        return FieldListStringValuesExtractorBuilder(dcBuilder, field, name) { request.queryParameters }.also {
            extractorBuilders[field] = it
        }
    }

    fun <F> headerRoot(field: KProperty1<T, F>) : RootStringValuesExtractorBuilder<T, F> {
        dcBuilder.loc(field, "header")
        return RootStringValuesExtractorBuilder(dcBuilder, field) { request.headers }.also {
            extractorBuilders[field] = it
        }
    }
    fun <F> header(field: KProperty1<T, F>, name: String) : FieldStringValuesExtractorBuilder<T, F> {
        dcBuilder.loc(field, listOf("header", name))
        return FieldStringValuesExtractorBuilder(dcBuilder, field, name) { request.headers }.also {
            extractorBuilders[field] = it
        }
    }
    fun <F> headerList(field: KProperty1<T, F>, name: String) : FieldListStringValuesExtractorBuilder<T, F> {
        dcBuilder.loc(field, listOf("header", name))
        return FieldListStringValuesExtractorBuilder(dcBuilder, field, name) { request.headers }.also {
            extractorBuilders[field] = it
        }
    }

    fun <F> jsonBody(field: KProperty1<T, F>,) : JsonBodyExtractorBuilder<T, F> {
        return JsonBodyExtractorBuilder(dcBuilder, field).also {
            extractorBuilders[field] = it
        }
    }

    fun constraintCheck(id: String, priority: Int = 10, func: ValidatorConstraintCheckFunction<T, T>) {
        dcBuilder.constraintCheck(id, priority, func)
    }
    fun transformBeforeCheck(id: String, priority: Int = 10, func: ValidatorTransformFunction<T, T>) {
        dcBuilder.transformBeforeCheck(id, priority, func)
    }
    fun transformAfterCheck(id: String, priority: Int = 10, func: ValidatorTransformFunction<T, T>) {
        dcBuilder.transformAfterCheck(id, priority, func)
    }
}

class RootStringValuesExtractorBuilder<T: Any, F>(
    private val dcValidatorBuilder: DataClassValidatorBuilder<T>,
    private val field: KProperty1<T, F>,
    private val getStringValues: ApplicationCall.() -> StringValues,
) : DataExtractorBuilder<Map<String, Any>, F> {

    private var fieldsAsList = mutableListOf<String>()

    override fun toExtractor(): RootStringValuesExtractor<F> {
        val fieldClazz = field.returnType.jvmErasure
        if (!fieldClazz.isData) {
            throw Exception("Only data classes can be passed to StringValuesExtractorBuilder")
        }
        return RootStringValuesExtractor(fieldsAsList.toList(), getStringValues)
    }

    fun meta(
        build: ValidatorMetaCollectionBuilder<F>.() -> ValidatorMetaCollectionBuilder<F>
    ) : RootStringValuesExtractorBuilder<T, F> {
        dcValidatorBuilder.field(field, build)
        return this
    }

    private val asListFields = mutableListOf<String>()
    fun extractAsList(vararg fields: KProperty1<F & Any, *>) : RootStringValuesExtractorBuilder<T, F> {
        asListFields.addAll(fields.map { it.name })
        return this
    }

}
class FieldStringValuesExtractorBuilder<T : Any, F>(
    private val dcValidatorBuilder: DataClassValidatorBuilder<T>,
    private val field: KProperty1<T, F>,
    private val alias: String,
    private val getStringValues: ApplicationCall.() -> StringValues,
) : DataExtractorBuilder<String, F> {

    override fun toExtractor(): FieldStringValuesExtractor<F> {
        return FieldStringValuesExtractor(alias, getStringValues)
    }
    fun meta(
        build: ValidatorMetaCollectionBuilder<F>.() -> ValidatorMetaCollectionBuilder<F>
    ) : FieldStringValuesExtractorBuilder<T, F> {
        dcValidatorBuilder.field(field, build)
        return this
    }
}

class FieldListStringValuesExtractorBuilder<T : Any, F>(
    private val dcValidatorBuilder: DataClassValidatorBuilder<T>,
    private val field: KProperty1<T, F>,
    private val alias: String,
    private val getStringValues: ApplicationCall.() -> StringValues,
) : DataExtractorBuilder<List<String>, F> {

    override fun toExtractor(): FieldListStringValuesExtractor<F> {
        return FieldListStringValuesExtractor(alias, emptyByDefault, getStringValues)
    }
    fun meta(
        build: ValidatorMetaCollectionBuilder<F>.() -> ValidatorMetaCollectionBuilder<F>
    ) : FieldListStringValuesExtractorBuilder<T, F> {
        dcValidatorBuilder.field(field, build)
        return this
    }

    private var emptyByDefault: Boolean = false
    fun emptyListByDefault() : FieldListStringValuesExtractorBuilder<T, F> {
        emptyByDefault = true
        return this
    }
}

class JsonBodyExtractorBuilder<T : Any, F>(
    private val dcValidatorBuilder: DataClassValidatorBuilder<T>,
    private val field: KProperty1<T, F>,
) : DataExtractorBuilder<JsonElement, F> {
    override fun toExtractor(): JsonBodyExtractor<F> {
        return JsonBodyExtractor(path)
    }

    fun meta(
        build: ValidatorMetaCollectionBuilder<F>.() -> ValidatorMetaCollectionBuilder<F>
    ) : JsonBodyExtractorBuilder<T, F> {
        dcValidatorBuilder.field(field, build)
        return this
    }

    private var path: DataExtractorPath? = null
    fun path(p: DataExtractorPath) : JsonBodyExtractorBuilder<T, F> {
        path = (path ?: DataExtractorPath()) / p
        dcValidatorBuilder.loc(field, path!!.toLocation())
        return this
    }
    fun path(elem: DataExtractorPath.Element) = path(DataExtractorPath() / elem)
    fun path(elem: String) = path(DataExtractorPath.Element(elem))
    fun path(elem: Int) = path(DataExtractorPath.Element(elem))
}
