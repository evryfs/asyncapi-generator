package dev.banking.asyncapi.generator.core.generator.model

import dev.banking.asyncapi.generator.core.model.schemas.Schema

/**
 * Builds the `@JsonProperty` annotation of a generated property: its wire name when the source
 * identifier differs from it, and its access when the schema is read-only or write-only.
 */
internal object JsonPropertyAccessAnnotationMapper {

    /**
     * @param wireName The JSON name, already escaped for a string literal of the target language;
     *   null when the identifier is the wire name.
     * @param namedTarget Annotation prefix for a wire name, such as `@field:JsonProperty` in Kotlin.
     */
    fun annotationFor(schema: Schema?, wireName: String?, namedTarget: String): String? {
        val access = accessFor(schema)
        return when {
            wireName != null && access != null -> "$namedTarget(value = \"$wireName\", access = $access)"
            wireName != null -> "$namedTarget(\"$wireName\")"
            access != null -> "@JsonProperty(access = $access)"
            else -> null
        }
    }

    private fun accessFor(schema: Schema?): String? =
        when {
            schema?.readOnly == true && schema.writeOnly == true -> "Access.READ_WRITE"
            schema?.readOnly == true -> "Access.READ_ONLY"
            schema?.writeOnly == true -> "Access.WRITE_ONLY"
            else -> null
        }
}
