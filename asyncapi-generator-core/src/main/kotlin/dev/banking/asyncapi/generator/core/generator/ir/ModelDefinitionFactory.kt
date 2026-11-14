package dev.banking.asyncapi.generator.core.generator.ir

import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.model.EnumLiteralNormalizer
import dev.banking.asyncapi.generator.core.generator.model.UnionSubtypes
import dev.banking.asyncapi.generator.core.generator.schema.isOpenPayload
import dev.banking.asyncapi.generator.core.generator.schema.isScalarAlias
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.generator.util.SourceIdentifierMapper
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
    private val context: GeneratorContext = input.schemaContext
    private val typeMapper = ModelTypeMapper(context)
    private val enumLiteralNormalizer = EnumLiteralNormalizer(packageName)

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
                    subtypes = UnionSubtypes.of(schema, context),
                )
            schema.isOpenPayload() ->
                OpenObjectModel(name = name, packageName = packageName, description = schema.description)
            primaryType == "object" ->
                ObjectModel(
                    name = name,
                    packageName = packageName,
                    description = schema.description,
                    properties =
                        schema.properties.orEmpty().map { (propertyName, propertySchema) ->
                            createProperty(propertyName, propertySchema, schema.required.orEmpty())
                        },
                    unions = input.polymorphicRelationships[name].orEmpty(),
                    annotation = annotation,
                )
            else -> null
        }
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
                val schema = context.findSchemaByName(reference.name)
                val type = if (schema?.isScalarAlias() == true) typeMapper.map(propertyName, schema) else reference
                schema to type
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
        if (type is TypeRef.Named && context.findSchemaByName(type.name)?.enum?.isNotEmpty() == true) {
            return DefaultValue.EnumEntry(type.name, EnumLiteralNormalizer.constantName(value) ?: value)
        }
        return when (schema.type.getPrimaryType()) {
            "boolean" -> DefaultValue.BooleanLiteral(value)
            "integer" -> DefaultValue.IntegerLiteral(value)
            "number" -> DefaultValue.NumberLiteral(value)
            "string" -> DefaultValue.StringLiteral(value)
            else -> null
        }
    }
}
