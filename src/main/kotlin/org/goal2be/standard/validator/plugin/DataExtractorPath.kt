package org.goal2be.standard.validator.plugin

class DataExtractorPath(private val path: List<Element> = listOf()) : Collection<DataExtractorPath.Element> by path {
    class Element private constructor(val path: String? = null, val idx: Int? = null) {
        constructor(path: String): this(path, null)
        constructor(idx: Int) : this(null, idx)
        val isName = path !== null
        val isIdx = idx !== null
        override fun toString() = path ?: idx!!.toString()

    }

    operator fun div(other: DataExtractorPath) = DataExtractorPath(path + other.path)
    operator fun div(other: Element) = DataExtractorPath(path + other)
    operator fun div(other: String) = DataExtractorPath(path + Element(other))
    operator fun div(other: Int) = DataExtractorPath(path + Element(other))

    fun toLocation() = path.map {it.toString()}

}
operator fun String.div(path: String) = DataExtractorPath() / this / path
operator fun String.div(path: Int) = DataExtractorPath() / this / path
operator fun Int.div(path: String) = DataExtractorPath() / this / path
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
operator fun Int.div(path: Int) = DataExtractorPath() / this / path
