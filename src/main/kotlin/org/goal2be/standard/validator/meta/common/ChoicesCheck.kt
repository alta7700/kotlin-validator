package org.goal2be.standard.validator.meta.common

import org.goal2be.standard.validator.ValidatorConstraintCheckFunction
import org.goal2be.standard.validator.ValidatorMetaCollectionBuilder
import org.goal2be.standard.validator.exceptions.ValidationException
import org.goal2be.standard.validator.meta.ConstraintCheckBase

fun <T: Any?> ValidatorMetaCollectionBuilder<T>.choices(values: List<T & Any>, priority: Int = 10) =
    add(ChoicesCheck(values, priority))
fun <T: Any?> ValidatorMetaCollectionBuilder<T>.choices(vararg values: T & Any, priority: Int = 10) =
    choices(values.toList(), priority)
class ChoicesCheck<T: Any?>(val available: List<T & Any>, priority: Int) : ConstraintCheckBase<T>("Choices", priority) {
    override val displayParams = listOf("available")
    val notAvailableException = ValidationException("not_available", "Value is not available", ctx = mapOf("available" to available))
    override val check: ValidatorConstraintCheckFunction<T, T & Any> = { value ->
        if (value !in available) throw notAvailableException
    }
}