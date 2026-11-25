package dev.banking.asyncapi.generator.core.generator.jvm

import dev.banking.asyncapi.generator.core.generator.sourcemodel.PropertyAccess

/**
 * Builds the `@JsonProperty` annotation of a generated property: its wire name when the source
 * identifier differs from it, and its access when the schema is read-only or write-only.
 */
internal object JsonPropertyAccessAnnotationMapper {
    /**
     * @param access Read-only or write-only access of the property; null for neither.
     * @param wireName The JSON name, already escaped for a string literal of the target language;
     *   null when the identifier is the wire name.
     * @param namedTarget Annotation prefix for a wire name, such as `@field:JsonProperty` in Kotlin.
     */
    fun annotationFor(
        access: PropertyAccess?,
        wireName: String?,
        namedTarget: String,
    ): String? {
        val accessMode = access?.let { "Access.${it.name}" }
        return when {
            wireName != null && accessMode != null -> "$namedTarget(value = \"$wireName\", access = $accessMode)"
            wireName != null -> "$namedTarget(\"$wireName\")"
            accessMode != null -> "@JsonProperty(access = $accessMode)"
            else -> null
        }
    }
}
