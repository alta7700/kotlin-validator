@file:Suppress("UNUSED")
package org.goal2be.standard.validator.types.base

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.goal2be.standard.utils.emailRegex
import org.goal2be.standard.validator.ValidatorConstraintCheckFunction
import org.goal2be.standard.validator.ValidatorMetaCollection
import org.goal2be.standard.validator.ValidatorMetaCollectionBuilder
import org.goal2be.standard.validator.ValidatorTransformFunction
import org.goal2be.standard.validator.exceptions.IncorrectTypeException
import org.goal2be.standard.validator.exceptions.ValidationException
import org.goal2be.standard.validator.meta.BaseTransform
import org.goal2be.standard.validator.meta.ConstraintCheckBase
import org.goal2be.standard.validator.meta.ValidatorTransformTrigger
import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import org.goal2be.standard.validator.types.TypeValidator
import kotlin.reflect.KType
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.typeOf

@Suppress("UNCHECKED_CAST")
class StringValidator<T: String?>(meta: ValidatorMetaCollection<T>, thisType: KType) : TypeValidator<T>(meta, thisType) {
    init {
        converterFor<Any> { it.toString() as T }
        converterFor<String> { it as T }

        val stringIncorrectTypeException = IncorrectTypeException(message = "Value must be a string")
        converterFor<JsonElement> { value ->
            if (value !is JsonPrimitive) throw stringIncorrectTypeException
            value.content as T
        }

        applyMeta()
    }
}

fun ValidatorTypesRegistry.initString() {
    val StringKType = typeOf<String>()
    addMatcher<String?> { type, meta ->
        if (type.isSupertypeOf(StringKType)) StringValidator(meta, type)
        else null
    }
}

fun <T: String?> ValidatorMetaCollectionBuilder<T>.minLength(length: Int, priority: Int = 10) = add(MinLength(length, priority))
class MinLength<T: String?>(private val length: Int, priority: Int): ConstraintCheckBase<T>("MinLength", priority) {

    override val displayParams = listOf("length", "priority=")
    companion object {
        val MinLengthException = ValidationException("min_length", "Length of string is less then minimum.")
    }

    private val currentMinLengthException = MinLengthException("min_length" to length)
    override val check: ValidatorConstraintCheckFunction<T, T & Any> = { value ->
        if (value.length < length) throw currentMinLengthException
    }
}

fun <T: String?> ValidatorMetaCollectionBuilder<T>.maxLength(length: Int, priority: Int = 10) = add(MaxLength(length, priority))
class MaxLength<T: String?>(private val length: Int, priority: Int): ConstraintCheckBase<T>("MaxLength", priority) {

    override val displayParams = listOf("length", "priority=")
    companion object {
        val MaxLengthException = ValidationException("max_length", "Length of string is more then maximum.")
    }

    private val currentMaxLengthException = MaxLengthException("max_length" to length)
    override val check: ValidatorConstraintCheckFunction<T, T & Any> = { value ->
        if (value.length > length) throw currentMaxLengthException
    }
}

fun <T: String?> ValidatorMetaCollectionBuilder<T>.pattern(regex: Regex, message: String? = null, priority: Int = 10) = add(Pattern(regex, message, priority))
open class Pattern<T: String?>(private val regex: Regex, private val message: String? = null, priority: Int) : ConstraintCheckBase<T>("Pattern", priority) {
    companion object {
        val PatternException = ValidationException("pattern", "Length of string is more then maximum.")
    }

    override val displayParams get() = if (message === null) listOf("regex", "priority=") else listOf("regex", "priority=", "message=")
    override fun getMemberValue(name: String): Any? {
        if (name == "regex") {
            return "Regex(${regex.pattern})"
        }
        return super.getMemberValue(name)
    }

    private val currentPatternException = PatternException(message = message)("pattern" to regex.pattern)
    override val check: ValidatorConstraintCheckFunction<T, T & Any> = { value ->
        if (!regex.matches(value)) throw currentPatternException
    }
}

fun <T: String?> ValidatorMetaCollectionBuilder<T>.digitsOnly(priority: Int = 10) = add(DigitsOnly(priority))
class DigitsOnly<T: String?>(priority: Int) : ConstraintCheckBase<T>("DigitsOnly", priority) {
    companion object {
        val DigitsOnlyException = ValidationException("digits_only", "Only digits are available")
        val availableSymbols = '0'..'9'
    }
    override val check: ValidatorConstraintCheckFunction<T, T & Any> = { value ->
        if (!value.all { it in availableSymbols }) throw DigitsOnlyException
    }
}

fun <T: String?> ValidatorMetaCollectionBuilder<T>.email(priority: Int = 10) = add(Email(priority))
class Email<T: String?>(priority: Int) : Pattern<T>(emailRegex, "Value is not a valid email.", priority) {
    override fun toString() = "Email()"
}

fun <T: String?> ValidatorMetaCollectionBuilder<T>.removeDoubleSpaces(
    trigger: ValidatorTransformTrigger = ValidatorTransformTrigger.BeforeCheck,
    priority: Int = 10
) = add(RemoveDoubleSpaces(trigger, priority))
class RemoveDoubleSpaces<T: String?>(trigger: ValidatorTransformTrigger, priority: Int) : BaseTransform<T>("RemoveDoubleSpaces", trigger, priority) {
    companion object {
        private val regex = Regex("""\s+""")
    }
    override val transform: ValidatorTransformFunction<T, T & Any> = { value ->
        @Suppress("UNCHECKED_CAST")
        regex.replace(value, " ") as (T & Any)
    }
}

fun <T: String?> ValidatorMetaCollectionBuilder<T>.lowerCase(
    trigger: ValidatorTransformTrigger = ValidatorTransformTrigger.BeforeCheck,
    priority: Int = 10
) = add(LowerCase(trigger, priority))
class LowerCase<T: String?>(trigger: ValidatorTransformTrigger, priority: Int) : BaseTransform<T>("LowerCase", trigger, priority) {
    override val transform: ValidatorTransformFunction<T, T & Any> = { value ->
        @Suppress("UNCHECKED_CAST")
        value.lowercase() as (T & Any)
    }
}


fun <T: String?> ValidatorMetaCollectionBuilder<T>.trimWhiteSpace(
    trigger: ValidatorTransformTrigger = ValidatorTransformTrigger.BeforeCheck,
    priority: Int = 10
) = add(TrimWhiteSpaces(trimStart = true, trimEnd = true, trigger = trigger, priority = priority))
fun <T: String?> ValidatorMetaCollectionBuilder<T>.trimWhiteSpaceStart(
    trigger: ValidatorTransformTrigger = ValidatorTransformTrigger.BeforeCheck,
    priority: Int = 10
) = add(TrimWhiteSpaces(trimStart = true, trimEnd = false, trigger = trigger, priority = priority))
fun <T: String?> ValidatorMetaCollectionBuilder<T>.trimWhiteSpaceEnd(
    trigger: ValidatorTransformTrigger = ValidatorTransformTrigger.BeforeCheck,
    priority: Int = 10
) = add(TrimWhiteSpaces(trimStart = false, trimEnd = true, trigger = trigger, priority = priority))
class TrimWhiteSpaces<T: String?>(
    private val trimStart: Boolean, private val trimEnd: Boolean,
    trigger: ValidatorTransformTrigger, priority: Int
) : BaseTransform<T>("TrimSpaces", trigger, priority) {
    override val displayName: String get() =
        if (trimStart && trimEnd) "TrimWhiteSpaces"
        else if (trimStart) "TrimWhiteSpacesStart"
        else "TrimWhiteSpacesEnd"

    override val transform: ValidatorTransformFunction<T, T & Any> = (
            @Suppress("UNCHECKED_CAST")
            if (trimStart && trimEnd) {
                { value -> value.trim() as (T & Any)}
            } else if (trimStart) {
                { value -> value.trimStart() as (T & Any)}
            } else if (trimEnd) {
                { value -> value.trimEnd() as (T & Any)}
            } else throw Error("trimStart or trimEnd must be equal to true")
    )
}
