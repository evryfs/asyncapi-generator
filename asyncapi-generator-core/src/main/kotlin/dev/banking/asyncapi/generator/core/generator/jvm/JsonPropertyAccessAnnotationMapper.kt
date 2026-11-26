package dev.banking.asyncapi.generator.core.generator.jvm

import dev.banking.asyncapi.generator.core.generator.sourcemodel.PropertyAccess

/**
 * Builds the `@JsonProperty` annotation of a generated property: its wire name when the source
 * identifier differs from it, and its access when the schema is read-only or write-only.
 */
internal object JsonPropertyAccessAnnotationMapper {
    private const val JSON_PROPERTY = "com.fasterxml.jackson.annotation.JsonProperty"

    /**
     * @param access Read-only or write-only access of the property; null for neither.
     * @param wireName The JSON name, already escaped for a string literal of the target language;
     *   null when the identifier is the wire name.
     */
    fun annotationFor(
        access: PropertyAccess?,
        wireName: String?,
    ): SourceAnnotation? {
        val accessMode = access?.let { "Access.${it.name}" }
        val accessImports = listOfNotNull("$JSON_PROPERTY.Access".takeIf { accessMode != null })
        val arguments =
            when {
                wireName != null && accessMode != null -> "value = \"$wireName\", access = $accessMode"
                wireName != null -> "\"$wireName\""
                accessMode != null -> "access = $accessMode"
                else -> return null
            }
        return SourceAnnotation(type = JSON_PROPERTY, arguments = arguments, argumentImports = accessImports)
    }
}
