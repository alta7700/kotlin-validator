package org.goal2be.standard.validator.meta.common

import org.goal2be.standard.validator.ValidatorConstraintCheckFunction
import org.goal2be.standard.validator.exceptions.ValidationException
import org.goal2be.standard.validator.meta.ConstraintCheckBase

class GreaterThan<T: Number?, V: Comparable<T & Any>>(
    private val minValue: V,
    canBeEqual: Boolean,
    priority: Int
): ConstraintCheckBase<T>("GreaterThan", priority) {

    override val displayParams = listOf("minValue", "canBeEqual=", "priority=")
    private val minValueException = ValidationException(
        "min_value",
        "Value must be greater than ${if (canBeEqual) "or equal to " else ""}minimal.",
        ctx = mapOf("min_value" to minValue)
    )
    override val check: ValidatorConstraintCheckFunction<T, T & Any> = if (canBeEqual) { value ->
        if (minValue > value) throw minValueException
    } else { value ->
        if (minValue >= value) throw minValueException
    }
}

class LessThan<T: Number?, V: Comparable<T & Any>>(
    private val maxValue: V,
    canBeEqual: Boolean,
    priority: Int
): ConstraintCheckBase<T>("LessThan", priority) {

    override val displayParams = listOf("maxValue", "canBeEqual=", "priority=")

    private val maxValueException = ValidationException(
        "max_value",
        "Value must be less than ${if (canBeEqual) "or equal to " else ""}maximal.",
        ctx = mapOf("max_value" to maxValue)
    )

    override val check: ValidatorConstraintCheckFunction<T, T & Any> = if (canBeEqual) { value ->
        if (maxValue < value) throw maxValueException
    } else { value ->
        if (maxValue <= value) throw maxValueException
    }
}