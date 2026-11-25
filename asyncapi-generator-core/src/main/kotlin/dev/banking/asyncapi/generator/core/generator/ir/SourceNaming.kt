package dev.banking.asyncapi.generator.core.generator.ir

/**
 * How names from the contract become identifiers of generated sources. The rules depend on the
 * languages the models are rendered in; property and enum constant collisions are checked against
 * the identifiers these rules produce.
 */
internal interface SourceNaming {
    /** The identifier of a property whose name in the payload is [wireName]. */
    fun propertyIdentifier(wireName: String): String

    /** The constant of the enum value [value]; null when no identifier remains. */
    fun enumConstant(value: String): String?
}
