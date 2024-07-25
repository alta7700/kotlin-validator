@file:Suppress("UNUSED")
package org.goal2be.standard.validator.meta

import org.goal2be.standard.validator.*
import org.goal2be.standard.validator.exceptions.ValidationException

abstract class ConstraintCheckBase<T>(
    internal val id: String,
    internal val priority: Int,
) : ValidatorMeta<T>() {
    override val displayName get() = id
    override val displayParams = listOf("priority=")
    override fun shouldRewrite(other: ValidatorMeta<T>): Boolean = other is ConstraintCheckBase && other.id == id

    internal abstract val check: ValidatorConstraintCheckFunction<T, T & Any>
    override fun patchValidator(validator: RealValidator<T>) {
        validator.constraintChecks.add(id = id, priority = priority, func = check)
    }

}

fun <T> ValidatorMetaCollectionBuilder<T>.constraintCheck(id: String, priority: Int = 10, check: ValidatorConstraintCheckFunction<T, T & Any>) = add(ConstraintCheck(id, priority, check))
fun <T> DataClassValidatorBuilder<T>.constraintCheck(id: String, priority: Int = 10, check: ValidatorConstraintCheckFunction<T, T & Any>) =
    meta(ConstraintCheck(id, priority) { value ->

        try { check(value) }
        catch (exc: ValidationException) {
            throw exc(location = "__root__")
        }
    })
class ConstraintCheck<T>(
    id: String,
    priority: Int,
    override val check: ValidatorConstraintCheckFunction<T, T & Any>
) : ConstraintCheckBase<T>(id, priority)
