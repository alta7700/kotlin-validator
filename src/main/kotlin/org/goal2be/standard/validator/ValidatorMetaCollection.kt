@file:Suppress("UNUSED")
package org.goal2be.standard.validator

class ValidatorMetaCollection<T: Any?>(
    initMeta: List<ValidatorMeta<T>>,
) : Collection<ValidatorMeta<T>> {
    constructor(vararg meta: ValidatorMeta<T>): this(meta.toList())

    private var meta = mutableListOf<ValidatorMeta<T>>()

    init {
        initMeta.forEach(::add)
    }

    fun add(elem: ValidatorMeta<T>, addInStart: Boolean = false) {
        meta.removeIf { elem.shouldRewrite(it) }
        val toJoin = meta.filter { elem.shouldJoinTo(it) }
        var resultMeta = elem
        if (toJoin.isNotEmpty()) {
            meta.removeAll(toJoin)
            toJoin.forEach { resultMeta = resultMeta.joinTo(it) }
        }
        if (addInStart) meta.add(0, resultMeta)
        else meta.add(resultMeta)
    }

    fun copyWith(other: ValidatorMetaCollection<T>) = ValidatorMetaCollection(meta + other.meta)
    fun copyWith(vararg other: ValidatorMeta<T>) = ValidatorMetaCollection(meta + other)
    fun copyWith(
        build: ValidatorMetaCollectionBuilder<T>.() -> ValidatorMetaCollectionBuilder<T>
    ) : ValidatorMetaCollection<T> {
        val newMeta = ValidatorMetaCollectionBuilder<T>().build().toMetaCollection()
        return ValidatorMetaCollection(this.meta + newMeta)
    }

    override val size
        get() = meta.size
    override fun isEmpty() = meta.isEmpty()
    override operator fun contains(element: ValidatorMeta<T>) = meta.contains(element)
    override operator fun iterator() = meta.iterator()
    override fun containsAll(elements: Collection<ValidatorMeta<T>>) = meta.containsAll(elements)

    override fun toString(): String {
        return meta.filter { it.shouldDisplay }.joinToString()
    }

    fun canAddSafety(item: ValidatorMeta<T>): Boolean = !meta.any {item.shouldRewrite(it)}
    fun addSafety(vararg items: ValidatorMeta<T>, addInStart: Boolean = false) {
        items.reversed().forEach {
            if (canAddSafety(it)) add(it, addInStart)
        }
    }
    fun copySafety(item: ValidatorMeta<T>): ValidatorMetaCollection<T> {
        return if (canAddSafety(item)) {
            copyWith(item)
        }
        else this
    }

    fun without(vararg elements: ValidatorMeta<T>): ValidatorMetaCollection<T> {
        return ValidatorMetaCollection(meta.filter { it !in elements })
    }

    inline fun <reified V: ValidatorMeta<T>> findMeta() : V? = find { it is V } as V?
    inline fun <reified V: ValidatorMeta<T>> findAllMeta() : List<V> = filterIsInstance<V>()
}

class ValidatorMetaCollectionBuilder<T: Any?> {
    private val meta = mutableListOf<ValidatorMeta<T>>()
    fun add(vararg el: ValidatorMeta<T>): ValidatorMetaCollectionBuilder<T> {
        meta.addAll(el)
        return this
    }

    fun toMetaCollection(): ValidatorMetaCollection<T> {
        return ValidatorMetaCollection(meta)
    }
}
