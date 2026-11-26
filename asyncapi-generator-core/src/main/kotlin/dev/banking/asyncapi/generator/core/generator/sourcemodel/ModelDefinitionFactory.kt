package dev.banking.asyncapi.generator.core.generator.sourcemodel

import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.schema.isOpenPayload
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.PropertyNameCollision
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Builds the language-neutral [ModelDefinition]s of the analyzed schemas.
 *
 * Schemas that need no generated type of their own, such as scalar aliases, yield nothing.
 *
 * @param input Analyzed schemas and the unions they belong to.
 * @param packageName Package of the generated models.
 * @param naming Naming rules of the generated sources, for property identifiers and enum constants.
 */
internal class ModelDefinitionFactory(
    private val input: GenerationInput,
    private val packageName: String,
    private val naming: SourceNaming,
) {
    private val schemas: Map<String, Schema> = input.schemas
    private val typeMapper = ModelTypeMapper(schemas)
    private val enumLiteralNormalizer = EnumLiteralNormalizer(packageName, naming)

    private val parsedScalars =
        setOf(TypeRef.Scalar.DATE, TypeRef.Scalar.TIME, TypeRef.Scalar.DATE_TIME, TypeRef.Scalar.UUID)

    fun create(): List<ModelDefinition> = input.schemas.mapNotNull { (name, schema) -> create(name, schema) }

    private fun create(
        name: String,
        schema: Schema,
    ): ModelDefinition? {
        val primaryType = schema.type?.primary
        return when {
            primaryType == "string" && !schema.enum.isNullOrEmpty() ->
                enumModel(name, schema)
            !schema.oneOf.isNullOrEmpty() || !schema.anyOf.isNullOrEmpty() -> unionModel(name, schema)
            schema.isOpenPayload() ->
                OpenObjectModel(name = name, packageName = packageName, description = schema.description)
            primaryType == "object" -> objectModel(name, schema)
            else -> null
        }
    }

    private fun enumModel(
        name: String,
        schema: Schema,
    ): EnumModel {
        val constants = enumLiteralNormalizer.normalize(name, schema.enum.orEmpty())
        val values = constants.map { it.jsonValue ?: it.name }
        return EnumModel(
            name = name,
            packageName = packageName,
            description = schema.description,
            constants = constants,
            default = schema.default?.toString()?.takeIf { it in values },
        )
    }

    /**
     * Without a discriminator the subtypes are only listed so that Jackson can deduce them, which
     * needs a generated class: members that are scalars, unions, or open objects have none.
     */
    private fun unionModel(
        name: String,
        schema: Schema,
    ): UnionModel {
        val members = UnionSubtypes.of(schema, schemas)
        return UnionModel(
            name = name,
            packageName = packageName,
            description = schema.description,
            discriminator = schema.discriminator,
            subtypes =
                members.filter { subtype ->
                    schema.discriminator != null ||
                        schemas[subtype.type]?.let { it.type?.primary == "object" && !it.isOpenPayload() } == true
                },
            members = members.map(UnionSubtype::type),
        )
    }

    private fun objectModel(
        name: String,
        schema: Schema,
    ): ObjectModel =
        ObjectModel(
            name = name,
            packageName = packageName,
            description = schema.description,
            properties =
                schema.properties
                    .orEmpty()
                    .map { (propertyName, propertySchema) ->
                        createProperty(propertyName, propertySchema, schema.required.orEmpty())
                    }.also { failOnIdentifierCollision(name, it) },
            unions = input.polymorphicRelationships[name].orEmpty(),
        )

    private fun failOnIdentifierCollision(
        schemaName: String,
        properties: List<PropertyDefinition>,
    ) {
        properties
            .groupBy(PropertyDefinition::identifier, PropertyDefinition::wireName)
            .entries
            .firstOrNull { (_, wireNames) -> wireNames.size > 1 }
            ?.let { (identifier, wireNames) -> throw PropertyNameCollision(schemaName, wireNames, identifier) }
    }

    private fun createProperty(
        propertyName: String,
        propertySchema: SchemaInterface,
        requiredProperties: List<String>,
    ): PropertyDefinition {
        val (schema, type) = resolveSchemaAndType(propertyName, propertySchema)
        return PropertyDefinition(
            wireName = propertyName,
            identifier = naming.propertyIdentifier(propertyName),
            type = type,
            required = propertyName in requiredProperties,
            nullable = schema?.type?.allowsNull == true,
            description = schema?.description,
            access = PropertyAccess.of(schema),
            constraints = Constraints.of(schema),
            default = schema?.let { defaultValue(it, type) },
        )
    }

    private fun resolveSchemaAndType(
        propertyName: String,
        propertySchema: SchemaInterface,
    ): Pair<Schema?, TypeRef> =
        when (propertySchema) {
            is SchemaInterface.SchemaInline ->
                propertySchema.schema to typeMapper.map(propertyName, propertySchema.schema)
            is SchemaInterface.SchemaReference -> {
                val reference = typeMapper.mapReference(propertySchema.reference)
                val schema = schemas[reference.name]
                schema to typeMapper.resolveReference(propertyName, propertySchema.reference)
            }
            is SchemaInterface.BooleanSchema -> null to TypeRef.Scalar.BOOLEAN
            else -> null to TypeRef.Scalar.ANY
        }

    private fun defaultValue(
        schema: Schema,
        type: TypeRef,
    ): DefaultValue? {
        val raw = schema.default ?: return null
        val value = raw.toString()
        return if (type is TypeRef.Named && schemas[type.name]?.enum?.isNotEmpty() == true) {
            DefaultValue.EnumEntry(type.name, naming.enumConstant(value) ?: value)
        } else {
            literalDefault(schema, type, value)
        }
    }

    /** A default of a boolean, number, or string property; null for other types. */
    private fun literalDefault(
        schema: Schema,
        type: TypeRef,
        value: String,
    ): DefaultValue? =
        when (schema.type?.primary) {
            "boolean" -> DefaultValue.BooleanLiteral(value)
            "integer" -> DefaultValue.IntegerLiteral(value)
            "number" -> DefaultValue.NumberLiteral(value)
            "string" ->
                if (type in parsedScalars) {
                    DefaultValue.ParsedScalar(type as TypeRef.Scalar, value)
                } else {
                    DefaultValue.StringLiteral(value)
                }
            else -> null
        }
}
