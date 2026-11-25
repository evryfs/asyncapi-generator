package dev.banking.asyncapi.generator.core.generator.sourcemodel

/** A payload model to generate, independent of the source language it is rendered in. */
internal sealed interface ModelDefinition {
    /** Name of the generated type. */
    val name: String

    /** Package the type is generated in. */
    val packageName: String

    /** Contract description as written, not yet formatted as documentation. */
    val description: String?
}

/**
 * An object with properties.
 *
 * @property name Name of the generated type.
 * @property packageName Package the type is generated in.
 * @property description Contract description as written.
 * @property properties Properties in contract order.
 * @property unions Union models this object is a member of.
 */
internal data class ObjectModel(
    override val name: String,
    override val packageName: String,
    override val description: String?,
    val properties: List<PropertyDefinition>,
    val unions: List<String>,
) : ModelDefinition

/**
 * A string enum.
 *
 * @property name Name of the generated type.
 * @property packageName Package the type is generated in.
 * @property description Contract description as written.
 * @property constants Constants in contract order.
 */
internal data class EnumModel(
    override val name: String,
    override val packageName: String,
    override val description: String?,
    val constants: List<EnumConstant>,
) : ModelDefinition

/**
 * A `oneOf`/`anyOf` union.
 *
 * @property name Name of the generated type.
 * @property packageName Package the type is generated in.
 * @property description Contract description as written.
 * @property discriminator Property whose value selects the member; null without one.
 * @property subtypes Members that have a generated type.
 */
internal data class UnionModel(
    override val name: String,
    override val packageName: String,
    override val description: String?,
    val discriminator: String?,
    val subtypes: List<UnionSubtype>,
) : ModelDefinition {
    /** Whether the member is deduced from the properties present, because there is no discriminator. */
    val deduction: Boolean get() = discriminator == null && subtypes.isNotEmpty()
}

/**
 * An object without declared properties, which accepts any payload.
 *
 * @property name Name of the generated type.
 * @property packageName Package the type is generated in.
 * @property description Contract description as written.
 */
internal data class OpenObjectModel(
    override val name: String,
    override val packageName: String,
    override val description: String?,
) : ModelDefinition

/**
 * A property of an [ObjectModel], with what its schema, or the schema it references, says about it.
 *
 * @property wireName Name in the JSON payload.
 * @property identifier Source identifier of the property.
 * @property type Type of the property.
 * @property required Whether the payload must contain the property.
 * @property nullable Whether the schema allows `null`.
 * @property description Contract description as written; null without one.
 * @property access Read-only or write-only access; null when neither is declared.
 * @property constraints Validation constraints of the value.
 * @property default Default value from the schema; null without a supported one.
 */
internal data class PropertyDefinition(
    val wireName: String,
    val identifier: String,
    val type: TypeRef,
    val required: Boolean,
    val nullable: Boolean = false,
    val description: String? = null,
    val access: PropertyAccess? = null,
    val constraints: Constraints = Constraints.None,
    val default: DefaultValue? = null,
)

/** A schema default value. */
internal sealed interface DefaultValue {
    /** A constant of the generated enum [enumName]. */
    data class EnumEntry(
        val enumName: String,
        val constant: String,
    ) : DefaultValue

    data class BooleanLiteral(
        val value: String,
    ) : DefaultValue

    data class IntegerLiteral(
        val value: String,
    ) : DefaultValue

    data class NumberLiteral(
        val value: String,
    ) : DefaultValue

    /**
     * A string default of a date, time, or UUID property: [value] is parsed into the [scalar] type,
     * not written as a string.
     */
    data class ParsedScalar(
        val scalar: TypeRef.Scalar,
        val value: String,
    ) : DefaultValue

    /** A string literal; [value] is the exact text, not yet escaped. */
    data class StringLiteral(
        val value: String,
    ) : DefaultValue
}
