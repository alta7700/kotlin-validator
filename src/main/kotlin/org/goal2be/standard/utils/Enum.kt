@file:Suppress("UNUSED")
package org.goal2be.standard.utils

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

inline fun <A, reified E: Enum<E>> createEnumMap(prop: KProperty1<E, A>, noinline filter: (E) -> Boolean = { true }) = createEnumMap(E::class, filter) { prop.get(it) }
inline fun <A, reified E: Enum<E>> createEnumMap(noinline filter: (E) -> Boolean = { true }, noinline associator: (E) -> A): Map<A, E> = createEnumMap(E::class, filter = filter, associator = associator)
fun <A, E: Enum<E>> createEnumMap(enumClazz: KClass<E>, filter: (E) -> Boolean = { true }, associator: (E) -> A): Map<A, E> {
    val entries = enumClazz.java.enumConstants.filter(filter)
    val result = entries.associateBy(associator)
    assert(result.size == entries.size) { "Associator results must be unique for $enumClazz" }
    return result
}

// don't know why, but overloading don't work here in the right way. Linter see the same signature
// so, i decided to call functions with personal suffix

fun <A, E: Enum<E>> enumFinder(enumMap: Map<A, E>): (A) -> E? = { value -> enumMap[value] }
inline fun <A, reified E: Enum<E>> enumFinder(prop: KProperty1<E, A>): (A) -> E? = enumFinder(createEnumMap(E::class) { prop.get(it) })
// D - default
fun <A, E: Enum<E>> enumFinderD(enumMap: Map<A, E>, defaultIfNotFound: E): (A) -> E = { value -> enumMap[value] ?: defaultIfNotFound }
inline fun <A, reified E: Enum<E>> enumFinderD(prop: KProperty1<E, A>, defaultIfNotFound: E): (A) -> E = enumFinderD(createEnumMap(E::class) { prop.get(it) }, defaultIfNotFound)
// DF - default factory
fun <A, E: Enum<E>> enumFinderDF(enumMap: Map<A, E>, defaultIfNotFound: (A) -> E): (A) -> E = { value -> enumMap[value] ?: defaultIfNotFound(value) }
inline fun <A, reified E: Enum<E>> enumFinderDF(prop: KProperty1<E, A>, noinline defaultIfNotFound: (A) -> E): (A) -> E = enumFinderDF(createEnumMap(E::class) { prop.get(it) }, defaultIfNotFound)
// E - error
fun <A, E: Enum<E>> enumFinderE(enumMap: Map<A, E>, error: (A) -> Nothing): (A) -> E = { value -> enumMap[value] ?: error(value) }
inline fun <A, reified E: Enum<E>> enumFinderE(prop: KProperty1<E, A>, noinline error: (A) -> Nothing): (A) -> E = enumFinderE(createEnumMap(E::class) { prop.get(it) }, error)

fun <A, E: Enum<E>> enumListFinder(enumMap: Map<A, E>): (List<A>) -> List<E?> = { value ->
    value.map(enumFinder(enumMap))
}
// D - default
fun <A, E: Enum<E>> enumListFinderD(enumMap: Map<A, E>, defaultIfNotFound: E): (List<A>) -> List<E> = { value ->
    value.map(enumFinderD(enumMap, defaultIfNotFound))
}
// DF - default factory
fun <A, E: Enum<E>> enumListFinderDF(enumMap: Map<A, E>, defaultIfNotFound: (A) -> E): (List<A>) -> List<E> = { value ->
    value.map(enumFinderDF(enumMap, defaultIfNotFound))
}
// E - error
fun <A, E: Enum<E>> enumListFinderE(enumMap: Map<A, E>, error: (A) -> Nothing): (List<A>) -> List<E> = { value ->
    value.map(enumFinderE(enumMap, error))
}
