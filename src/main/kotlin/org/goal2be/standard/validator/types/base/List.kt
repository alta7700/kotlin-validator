@file:Suppress("UNUSED")
package org.goal2be.standard.validator.types.base

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import org.goal2be.standard.validator.*
import org.goal2be.standard.validator.exceptions.IncorrectTypeException
import org.goal2be.standard.validator.exceptions.ValidationException
import org.goal2be.standard.validator.exceptions.ValidationExceptionList
import org.goal2be.standard.validator.exceptions.accumulateValidationException
import org.goal2be.standard.validator.meta.ConstraintCheckBase
import org.goal2be.standard.validator.types.TypeValidator
import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

class ListValidator<T: List<I>?, I: Any?>(
    meta: ValidatorMetaCollection<T>,
    thisType: KType,
) : TypeValidator<T>(meta, thisType) {
    private val itemValidator = thisType.arguments[0].type?.let {
        ValidatorTypesRegistry.match(it, meta.findMeta<ListItemMeta<T, I>>()?.meta ?: ValidatorMetaCollection())
    } ?: error("List item type parameter is not set. Can't apply itemValidator")
    var stringSplitter: String = ","

    private fun <F> buildConvertFromListOf(type: KType) : ValidatorConversionFunction<List<F>, T> {
        val itemConverter = itemValidator.buildConvertFrom<F>(type)
        return { value ->
            val excs = ValidationExceptionList()
            val result = mutableListOf<I>()
            value.forEachIndexed { index, item ->
                accumulateValidationException(excs, index.toString(), item) {
                    result.add(itemConverter(item))
                }
            }
            if (excs.isNotEmpty()) throw excs
            @Suppress("UNCHECKED_CAST")
            result.toList() as T
        }
    }
    override fun <F: Any?> buildNNConvertFrom(type: KType): ValidatorConversionFunction<F & Any, T> {
        if (type.jvmErasure == List::class) {
            @Suppress("UNCHECKED_CAST")
            return buildConvertFromListOf<Any?>(
                type.arguments[0].type?.withNullability(false)
                    ?: typeOf<Any>()
            ) as ValidatorConversionFunction<F & Any, T>
        }
        return super.buildNNConvertFrom(type)
    }

    init {
        converterFor<Any> { value ->
            if (value !is List<*>) throw cantConvertToListException
            val fromAnyConverter = buildConvertFrom<List<Any?>>()
            fromAnyConverter(value)
        }

        converterFor<String> { value ->
            val fromListOfStringConverter = buildConvertFrom<List<String>>()
            fromListOfStringConverter(value.split(stringSplitter))
        }

        converterFor<JsonElement> { value ->
            if (value !is JsonArray) throw cantConvertToListException
            val fromJsonArrayConverter = buildConvertFrom<List<JsonElement>>()
            fromJsonArrayConverter(value)
        }
        converterFor<JsonArray> { value ->
            val fromJsonArrayConverter = buildConvertFrom<List<JsonElement>>()
            fromJsonArrayConverter(value)
        }

        applyMeta()
    }
}
private val cantConvertToListException = IncorrectTypeException(message = "Value can't be converted to list")

fun ValidatorTypesRegistry.initList() {
    addMatcher<List<*>> { type, meta ->
        if (type.jvmErasure == List::class) ListValidator(meta, type)
        else null
    }
}

fun <T: List<I>?, I> ValidatorMetaCollectionBuilder<T>.itemsMeta(
    build: ValidatorMetaCollectionBuilder<I>.() -> ValidatorMetaCollectionBuilder<I>
) = add(ListItemMeta(build))
class ListItemMeta<T: List<I>?, I>(val meta: ValidatorMetaCollection<I>) : NoPatchValidatorMeta<T>() {

    override val displayParams = listOf("meta")
    constructor (
        build: ValidatorMetaCollectionBuilder<I>.() -> ValidatorMetaCollectionBuilder<I>
    ): this(ValidatorMetaCollectionBuilder<I>().build().toMetaCollection())

    override fun shouldRewrite(other: ValidatorMeta<T>) = false
    override fun shouldJoinTo(other: ValidatorMeta<T>) = other is ListItemMeta<*, *>
    override fun joinTo(other: ValidatorMeta<T>): ValidatorMeta<T> {
        @Suppress("UNCHECKED_CAST")
        other as ListItemMeta<T, I>
        return ListItemMeta(other.meta.copyWith(this.meta))
    }
}

fun <T: List<I>?, I> ValidatorMetaCollectionBuilder<T>.stringSplitter(splitter: String) = add(ListStringSplitter(splitter))
class ListStringSplitter<T: List<I>?, I>(private val splitter: String) : ValidatorMeta<T>() {

    override val displayParams = listOf("splitter")

    override fun patchValidator(validator: RealValidator<T>) {
        if (validator !is ListValidator<*, *>) throw Error("can be applied only to ListValidator")
        validator.stringSplitter = splitter
    }
}

fun <T: List<I>?, I> ValidatorMetaCollectionBuilder<T>.minItems(count: Int, priority: Int = 10) = add(ListMinItems(count, priority))
class ListMinItems<T: List<I>?, I>(private val count: Int, priority: Int) : ConstraintCheckBase<T>("ListMinItems", priority) {
    override val displayParams = listOf("count", "priority=")

    companion object {
        val ListMinException = ValidationException("min_items", "The list contains fewer items than allowed")
    }

    private val currentListMinException = ListMinException("min_items" to count)
    override val check: ValidatorConstraintCheckFunction<T, T & Any> = { value ->
        if (value.size < count) throw currentListMinException
    }
}

fun <T: List<I>?, I> ValidatorMetaCollectionBuilder<T>.maxItems(count: Int, priority: Int = 10) = add(ListMaxItems(count, priority))
class ListMaxItems<T: List<I>?, I>(private val count: Int, priority: Int) : ConstraintCheckBase<T>("ListMaxItems", priority) {
    override val displayParams = listOf("count", "priority=")

    companion object {
        val ListMaxException = ValidationException("max_items", "The list contains fewer items than allowed")
    }

    private val currentListMaxException = ListMaxException("max_items" to count)
    override val check: ValidatorConstraintCheckFunction<T, T & Any> = { value ->
        if (value.size > count) throw currentListMaxException
    }
}

fun <T: List<I>?, I> ValidatorMetaCollectionBuilder<T>.unique(priority: Int = 10) = add(ListUnique(priority))
class ListUnique<T: List<I>?, I>(priority: Int) : ConstraintCheckBase<T>("ListUnique", priority) {
    companion object {
        val ListUniqueException = ValidationException("unique", "All members of list must be unique.")
    }

    override val check: ValidatorConstraintCheckFunction<T, T & Any> = { value ->
        if (value.toSet().size != value.size) throw ListUniqueException
    }
}

