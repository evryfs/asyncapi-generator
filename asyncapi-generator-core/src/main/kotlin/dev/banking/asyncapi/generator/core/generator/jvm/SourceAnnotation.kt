package dev.banking.asyncapi.generator.core.generator.jvm

/**
 * An annotation of generated JVM source: its type and its arguments. It is written with the
 * use-site target the language needs where it is placed, and imported by its type.
 *
 * @property type the fully qualified name of the annotation type
 * @property arguments the arguments as source text, without parentheses; null when there are none
 * @property argumentImports further types the arguments refer to, such as `JsonProperty.Access`
 */
internal data class SourceAnnotation(
    val type: String,
    val arguments: String? = null,
    val argumentImports: List<String> = emptyList(),
) {
    /** The simple name of [type], which the source uses after importing it. */
    val simpleName: String
        get() = type.substringAfterLast('.')

    /** The imports this annotation needs. */
    val imports: List<String>
        get() = listOf(type) + argumentImports

    /** The annotation as source text, such as `@field:Size(max = 10)` with [useSiteTarget] `field`. */
    fun render(useSiteTarget: String? = null): String =
        buildString {
            append('@')
            useSiteTarget?.let { append(it).append(':') }
            append(simpleName)
            arguments?.let { append('(').append(it).append(')') }
        }
}
