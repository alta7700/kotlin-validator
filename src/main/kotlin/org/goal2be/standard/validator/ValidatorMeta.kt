package org.goal2be.standard.validator

import io.ktor.util.reflect.*
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmName

abstract class ValidatorMeta<T> {

    open val shouldDisplay: Boolean = true
    open val displayName get() = this::class.simpleName ?: this::class.qualifiedName ?: this::class.jvmName
    open val displayParams = listOf<String>()

    open fun shouldRewrite(other: ValidatorMeta<T>): Boolean = other.instanceOf(this::class)
    open fun shouldJoinTo(other: ValidatorMeta<T>): Boolean = false
    open fun joinTo(other: ValidatorMeta<T>): ValidatorMeta<T> {
        throw Exception("Not implemented for $this")
    }

    @Suppress("UNCHECKED_CAST")
    open fun getMemberValue(name: String): Any? {
        return (this::class as KClass<ValidatorMeta<T>>).memberProperties
            .first { it.name == name }
            .let{ it.isAccessible = true; it }
            .getter(this)
    }
    internal open fun getParamsString(): String = displayParams.joinToString {
        if (it.endsWith('=')) {
            val name = it.dropLast(1)
            "$name=${this.getMemberValue(name)}"
        } else "${this.getMemberValue(it)}"
    }

    override fun toString(): String =
        "${this.displayName}(${getParamsString()})"

    abstract fun patchValidator(validator: RealValidator<T>)
}
