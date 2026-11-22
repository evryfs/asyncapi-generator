package dev.banking.asyncapi.generator.core.validator.walk

/**
 * The chain of objects from a root object to the object a rule checks, as findings name it, such as
 * `Channel 'orders' Message 'created'`.
 *
 * @param segments the objects of the chain, each as findings name it
 */
internal class ValidationPath private constructor(
    private val segments: List<String>,
) {
    /** The child object [name] of kind [kind], rendered as `Kind 'name'`. */
    fun named(
        kind: String,
        name: String,
    ): ValidationPath = then("$kind '$name'")

    /** Element [index] of a list of [kind] objects, rendered as `Kind[index]`. */
    fun indexed(
        kind: String,
        index: Int,
    ): ValidationPath = then("$kind[$index]")

    /** A child object of kind [kind] that has no name of its own, rendered as `Kind`. */
    fun child(kind: String): ValidationPath = then(kind)

    private fun then(segment: String): ValidationPath = ValidationPath(segments + segment)

    override fun toString(): String = segments.joinToString(" ")

    override fun equals(other: Any?): Boolean = other is ValidationPath && other.segments == segments

    override fun hashCode(): Int = segments.hashCode()

    companion object {
        /** A root object [name] of kind [kind], rendered as `Kind 'name'`. */
        fun named(
            kind: String,
            name: String,
        ): ValidationPath = ValidationPath(listOf("$kind '$name'"))

        /** A root object of kind [kind] that has no name, rendered as `Kind`. */
        fun of(kind: String): ValidationPath = ValidationPath(listOf(kind))
    }
}
