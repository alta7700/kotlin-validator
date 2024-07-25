package org.goal2be.standard.validator.types.extended

import kotlinx.datetime.*
import org.goal2be.standard.utils.toDateOrNull
import org.goal2be.standard.validator.ValidatorConstraintCheckFunction
import org.goal2be.standard.validator.ValidatorMetaCollection
import org.goal2be.standard.validator.ValidatorMetaCollectionBuilder
import org.goal2be.standard.validator.exceptions.IncorrectTypeException
import org.goal2be.standard.validator.exceptions.ValidationException
import org.goal2be.standard.validator.meta.ConstraintCheckBase
import org.goal2be.standard.validator.ExtendedValidator
import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import kotlin.reflect.KType
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.typeOf

class LocalDateValidator<T: LocalDate?>(meta: ValidatorMetaCollection<T>, thisType: KType) : ExtendedValidator<T, String>(meta, thisType) {
    override val internalType: KType = typeOf<String>()
    private val dateIncorrectTypeException = IncorrectTypeException(message = "Value must be a valid date")

    @Suppress("UNCHECKED_CAST")
    override fun convertFromInternal(value: String): T {
        return (value.toDateOrNull() as T?) ?: throw dateIncorrectTypeException
    }
    init {
        applyMeta()
    }

}

fun ValidatorTypesRegistry.initLocalDate() {
    addMatcher<LocalDate?> { type, meta ->
        if ( type.isSupertypeOf(typeOf<LocalDate>())) {
            LocalDateValidator(meta, type)
        }
        else null
    }
}

fun <T: LocalDate?> ValidatorMetaCollectionBuilder<T>.minPeriod(period: DatePeriod, priority: Int = 10) = add(MinPeriod(period, priority))
fun <T: LocalDate?> ValidatorMetaCollectionBuilder<T>.minPeriod(years: Int = 0, months: Int = 0, days: Int = 0, priority: Int = 10) = minPeriod(DatePeriod(years, months, days), priority)
class MinPeriod<T: LocalDate?>(private val period: DatePeriod, priority: Int) : ConstraintCheckBase<T>("MinPeriod", priority) {

    companion object {
        val MinPeriod = ValidationException("min_period", "Too little period.")
    }
    private val currentMinPeriod = MinPeriod("years" to period.years, "months" to period.months, "days" to period.days)
    override val check: ValidatorConstraintCheckFunction<T, T & Any> = { value ->
        val currentPeriod = Clock.System.now().toLocalDateTime(TimeZone.UTC).date - value
        val success = if (currentPeriod.years > period.years) true
        else if (currentPeriod.years == period.years) {
            if (currentPeriod.months > period.months) true
            else if (currentPeriod.months == period.months) currentPeriod.days > period.days
            else false
        } else false
        if (!success) throw currentMinPeriod
    }
}

fun <T: LocalDate?> ValidatorMetaCollectionBuilder<T>.maxPeriod(period: DatePeriod, priority: Int = 10) = add(MaxPeriod(period, priority))
fun <T: LocalDate?> ValidatorMetaCollectionBuilder<T>.maxPeriod(years: Int = 0, months: Int = 0, days: Int = 0, priority: Int = 10) = maxPeriod(
    DatePeriod(years, months, days), priority)
class MaxPeriod<T: LocalDate?>(private val period: DatePeriod, priority: Int) : ConstraintCheckBase<T>("MaxPeriod", priority) {

    companion object {
        val MaxPeriod = ValidationException("max_period", "Too big period.")
    }
    private val currentMaxPeriod = MaxPeriod("years" to period.years, "months" to period.months, "days" to period.days)
    override val check: ValidatorConstraintCheckFunction<T, T & Any> = { value ->
        val currentPeriod = Clock.System.now().toLocalDateTime(TimeZone.UTC).date - value
        val success = if (currentPeriod.years < period.years) true
        else if (currentPeriod.years == period.years) {
            if (currentPeriod.months < period.months) true
            else if (currentPeriod.months == period.months) currentPeriod.days < period.days
            else false
        } else false
        if (!success) throw currentMaxPeriod
    }
}

