@file:Suppress("UNUSED")
package org.goal2be.standard.validator.meta

import org.goal2be.standard.validator.*

enum class ValidatorTransformTrigger {
    BeforeCheck,
    AfterCheck
}

abstract class BaseTransform<T>(
    internal val id: String,
    internal val trigger: ValidatorTransformTrigger,
    internal val priority: Int
) : ValidatorMeta<T>() {
    override val displayName: String get() = id
    override val displayParams: List<String> = listOf("trigger=", "priority=")
    override fun shouldRewrite(other: ValidatorMeta<T>): Boolean = other is BaseTransform<*> && id == other.id

    override fun patchValidator(validator: RealValidator<T>) {
        val collection = when (trigger) {
            ValidatorTransformTrigger.BeforeCheck -> validator.beforeCheckTransforms
            ValidatorTransformTrigger.AfterCheck -> validator.afterCheckTransforms
        }
        collection.add(id, priority, transform)
    }
    internal abstract val transform: ValidatorTransformFunction<T, T & Any>
}

fun <T> ValidatorMetaCollectionBuilder<T>.transformBeforeCheck(
    id: String,
    priority: Int = 10,
    transform: ValidatorTransformFunction<T, T & Any>
) = add(Transform(id, ValidatorTransformTrigger.BeforeCheck, priority, transform))
fun <T> DataClassValidatorBuilder<T>.transformBeforeCheck(
    id: String,
    priority: Int = 10,
    transform: ValidatorTransformFunction<T, T & Any>
) = meta(Transform(id, ValidatorTransformTrigger.BeforeCheck, priority, transform))
fun <T> ValidatorMetaCollectionBuilder<T>.transformAfterCheck(
    id: String,
    priority: Int = 10,
    transform: ValidatorTransformFunction<T, T & Any>
) = add(Transform(id, ValidatorTransformTrigger.AfterCheck, priority, transform))
fun <T> DataClassValidatorBuilder<T>.transformAfterCheck(
    id: String,
    priority: Int = 10,
    transform: ValidatorTransformFunction<T, T & Any>
) = meta(Transform(id, ValidatorTransformTrigger.AfterCheck, priority, transform))
class Transform<T>(
    id: String,
    trigger: ValidatorTransformTrigger,
    priority: Int,
    override val transform: ValidatorTransformFunction<T, T & Any>
) : BaseTransform<T>(id, trigger, priority)
