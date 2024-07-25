package org.goal2be.standard.validator

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName

typealias ValidatorConversionFunction<F, T> = (F) -> T

typealias ValidatorTransformFunction<V, T> = IValidator<V>.(T) -> T
typealias ValidatorConstraintCheckFunction<V, T> = IValidator<V>.(T) -> Unit
interface ValidatorActivityData<T, R> {
    val id: String
    val priority: Int
    val func: IValidator<T>.(T & Any) -> R

    operator fun invoke(value: T & Any, validator: IValidator<T>) : R {
        return validator.func(value)
    }
}

data class ValidatorTransform<T>(
    override val id: String,
    override val priority: Int,
    override val func: ValidatorTransformFunction<T, T & Any>
) : ValidatorActivityData<T, T & Any>

data class ValidatorConstraintCheck<T>(
    override val id: String,
    override val priority: Int,
    override val func: ValidatorConstraintCheckFunction<T, T & Any>
) : ValidatorActivityData<T, Unit>

abstract class ValidatorActivityCollection<T, A: ValidatorActivityData<T, *>> : Collection<A> {
    private val transforms: MutableList<A> = mutableListOf()

    fun add(el: A) {
        transforms.removeIf { it.id == el.id }
        val idx = transforms.indexOfFirst { it.priority > el.priority }
        if (idx == -1) transforms.add(el)
        else transforms.add(idx, el)
    }

    override val size: Int get() = transforms.size
    override fun isEmpty(): Boolean = transforms.isEmpty()
    override fun contains(element: A): Boolean = transforms.contains(element)
    override fun containsAll(elements: Collection<A>): Boolean = transforms.containsAll(elements)
    override fun iterator(): Iterator<A> = transforms.iterator()
}

class ValidatorTransformCollection<T> : ValidatorActivityCollection<T, ValidatorTransform<T>>() {
    fun add(id: String, priority: Int, func: ValidatorTransformFunction<T, T & Any>) {
        add(ValidatorTransform(id, priority, func))
    }
}

class ValidatorConstraintCheckCollection<T> : ValidatorActivityCollection<T, ValidatorConstraintCheck<T>>() {
    fun add(id: String, priority: Int, func: ValidatorConstraintCheckFunction<T, T & Any>) {
        add(ValidatorConstraintCheck(id, priority, func))
    }
}

class DataClassType<T>(private val type: KType) {
    @Suppress("UNCHECKED_CAST")
    private val clazz = type.jvmErasure as KClass<T & Any>

    init {
        if (!clazz.isData) throw Error("$clazz is not a data class")
        if (type.arguments.any { it.type == null }) throw Error("Not all of generics are set in $type")
    }

    val name = clazz.simpleName ?: clazz.qualifiedName ?: clazz.jvmName

    private val constructor = clazz.primaryConstructor ?: throw Error("$clazz has not a primary constructor")
    val params = constructor.parameters.filter { !it.hasAnnotation<DefaultedOnly>() }.associateBy { it.name!! }
    val defaultedParams = constructor.parameters.filter { it.hasAnnotation<DefaultedOnly>() }
    fun getParamType(param: KParameter): KType {
        val paramIndex = clazz.typeParameters.indexOf(param.type.classifier)
        if (paramIndex >= 0) return type.arguments[paramIndex].type!!
        if (param.type.arguments.isNotEmpty() && param.type.arguments.any { it.type!!.classifier in clazz.typeParameters }) {
            return param.type.let { it.jvmErasure.createType(
                it.arguments.map { arg ->
                    if (arg.type!!.classifier !in clazz.typeParameters) arg
                    else arg.copy(
                        arg.variance,
                        type.arguments[clazz.typeParameters.indexOf(arg.type!!.classifier)].type!!
                            .let { argType ->
                                // KTypeImpl has .type, which can be DefinitelyNotNullType. But i can't use cast to KTypeImpl, because it is internal
                                // So I use this unsafe string check :)
                                if (arg.toString().endsWith("& Any") && argType.isMarkedNullable) {
                                    argType.withNullability(false)
                                }
                                else argType
                            }
                    )
                },
                it.isMarkedNullable,
                it.annotations
            ) }.also(::println)
        }
        return param.type
    }

    fun create(data: Map<String, *>, defaults: Map<KParameter, Any?>): T & Any {
        return constructor.callBy(
            data.entries.associate { Pair(params[it.key]!!, it.value)} + defaults
        )
    }
}