package dev.banking.asyncapi.generator.core.generator.ir

import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.schema.getPrimaryType
import dev.banking.asyncapi.generator.core.generator.schema.isOpenPayload
import dev.banking.asyncapi.generator.core.generator.util.SourceIdentifierMapper
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.PropertyNameCollision
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Builds the language-neutral [ModelDefinition]s of the analyzed schemas.
 *
 * Schemas that need no generated type of their own, such as scalar aliases, yield nothing.
 */
internal class ModelDefinitionFactory(
    private val input: GenerationInput,
    private val packageName: String,
    private val annotation: QualifiedTypeName? = null,
) {
    private val schemas: Map<String, Schema> = input.schemas
    private val typeMapper = ModelTypeMapper(schemas)
    private val enumLiteralNormalizer = EnumLiteralNormalizer(packageName)

    private val parsedScalars =
        setOf(TypeRef.Scalar.DATE, TypeRef.Scalar.TIME, TypeRef.Scalar.DATE_TIME, TypeRef.Scalar.UUID)

    fun create(): List<ModelDefinition> = input.schemas.mapNotNull { (name, schema) -> create(name, schema) }

    private fun create(
        name: String,
        schema: Schema,
    ): ModelDefinition? {
        val primaryType = schema.type.getPrimaryType()
        return when {
            primaryType == "string" && !schema.enum.isNullOrEmpty() ->
                EnumModel(
                    name = name,
                    packageName = packageName,
                    description = schema.description,
                    constants = enumLiteralNormalizer.normalize(name, schema.enum),
                )
            !schema.oneOf.isNullOrEmpty() || !schema.anyOf.isNullOrEmpty() ->
                UnionModel(
                    name = name,
                    packageName = packageName,
                    description = schema.description,
                    discriminator = schema.discriminator,
                    subtypes = unionSubtypes(schema),
                )
            schema.isOpenPayload() ->
                OpenObjectModel(name = name, packageName = packageName, description = schema.description)
            primaryType == "object" ->
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
                    annotation = annotation,
                )
            else -> null
        }
    }

    /**
     * Without a discriminator the subtypes are only listed so that Jackson can deduce them, which
     * needs a generated class: members that are scalars, unions, or open objects have none.
     */
    private fun unionSubtypes(schema: Schema): List<UnionSubtype> =
        UnionSubtypes.of(schema, schemas).filter { subtype ->
            schema.discriminator != null ||
                schemas[subtype.type]
                    ?.let { it.type.getPrimaryType() == "object" && !it.isOpenPayload() } == true
        }

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
            identifier = SourceIdentifierMapper.toIdentifier(propertyName),
            type = type,
            required = propertyName in requiredProperties,
            schema = schema,
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
        val value = raw.toString().trimStart('"', '\'', '|', '>').removeSurrounding("\"")
        if (type is TypeRef.Named && schemas[type.name]?.enum?.isNotEmpty() == true) {
            return DefaultValue.EnumEntry(type.name, EnumLiteralNormalizer.constantName(value) ?: value)
        }
        return when (schema.type.getPrimaryType()) {
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
}
