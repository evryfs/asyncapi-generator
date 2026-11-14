package dev.banking.asyncapi.generator.core.generator.ir

import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.model.EnumConstant
import dev.banking.asyncapi.generator.core.generator.model.UnionSubtype
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.isTypeNullable
import dev.banking.asyncapi.generator.core.model.schemas.Schema

/**
 * A payload model to generate, independent of the source language it is rendered in.
 *
 * @property description Contract description as written, not yet formatted as documentation.
 */
internal sealed interface ModelDefinition {
    val name: String
    val packageName: String
    val description: String?
}

/**
 * An object with properties.
 *
 * @property unions Union models this object is a member of.
 * @property annotation Configured annotation for every generated model class.
 */
internal data class ObjectModel(
    override val name: String,
    override val packageName: String,
    override val description: String?,
    val properties: List<PropertyDefinition>,
    val unions: List<String>,
    val annotation: QualifiedTypeName? = null,
) : ModelDefinition

internal data class EnumModel(
    override val name: String,
    override val packageName: String,
    override val description: String?,
    val constants: List<EnumConstant>,
) : ModelDefinition

/**
 * A `oneOf`/`anyOf` union.
 *
 * @property discriminator Property whose value selects the member; null without one.
 */
internal data class UnionModel(
    override val name: String,
    override val packageName: String,
    override val description: String?,
    val discriminator: String?,
    val subtypes: List<UnionSubtype>,
) : ModelDefinition

/** An object without declared properties, which accepts any payload. */
internal data class OpenObjectModel(
    override val name: String,
    override val packageName: String,
    override val description: String?,
) : ModelDefinition

/**
 * A property of an [ObjectModel].
 *
 * @property wireName Name in the JSON payload.
 * @property identifier Source identifier of the property.
 * @property schema The property schema, or the schema it references; drives constraints, access,
 *   nullability, and documentation.
 * @property default Default value from the schema; null without a supported one.
 */
internal data class PropertyDefinition(
    val wireName: String,
    val identifier: String,
    val type: TypeRef,
    val required: Boolean,
    val schema: Schema?,
    val default: DefaultValue? = null,
) {
    /** Whether the schema allows `null`. */
    val nullableInSchema: Boolean get() = schema?.type.isTypeNullable()

    val description: String? get() = schema?.description
}

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

    /** A string literal; [value] is the exact text, not yet escaped. */
    data class StringLiteral(
        val value: String,
    ) : DefaultValue
}
