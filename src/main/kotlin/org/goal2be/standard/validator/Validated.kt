package org.goal2be.standard.validator

import org.goal2be.standard.validator.types.ValidatorTypesRegistry
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf


@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Validated(val clazz: KClass<*>) {
    companion object {
        inline fun <reified T: Any> create(meta: ValidatorMetaCollection<T>): RealValidator<T>? = create(meta, typeOf<T>())
        fun <T: Any> create(meta: ValidatorMetaCollection<T>, type: KType): RealValidator<T>? {
            val clazz = type.jvmErasure
            return clazz.findAnnotation<Validated>()?.let { ann ->
                ann.clazz.let {
                    assert(it.isSubclassOf(RealValidator::class))
                    @Suppress("UNCHECKED_CAST")
                    (it as KClass<RealValidator<T>>).primaryConstructor!!.call(meta, type)
                }
            }
        }
    }
}

fun ValidatorTypesRegistry.initValidated() {
    addMatcher<Any> { type, meta -> Validated.create(meta, type) }
}