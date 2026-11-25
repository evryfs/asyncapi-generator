package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.schema.getPrimaryType
import dev.banking.asyncapi.generator.core.generator.schema.hasMultipleNonNullTypes
import dev.banking.asyncapi.generator.core.generator.schema.isTypeNullable
import dev.banking.asyncapi.generator.core.generator.schema.linkedSchema
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ModelTypeMapper
import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedKafkaHeaderSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/** Resolves a contract-defined Kafka header schema into a generated parameter type. */
internal object KafkaHeaderTypeResolver {
    fun resolve(
        headerContractName: String,
        wireName: String,
        schema: SchemaInterface,
    ): KafkaHeaderType {
        val resolvedSchema = schema.resolve(headerContractName, wireName)
        if (resolvedSchema.type.hasMultipleNonNullTypes()) {
            throw unsupported(
                headerContractName = headerContractName,
                wireName = wireName,
                schemaType = "union with multiple non-null types",
            )
        }

        val scalarType =
            ModelTypeMapper.scalarType(resolvedSchema)
                ?: throw unsupported(
                    headerContractName = headerContractName,
                    wireName = wireName,
                    schemaType = resolvedSchema.type.getPrimaryType() ?: "unspecified",
                )

        return KafkaHeaderType(
            type = scalarType,
            schemaNullable = resolvedSchema.type.isTypeNullable(),
            description = resolvedSchema.description,
        )
    }

    private fun SchemaInterface.resolve(
        headerContractName: String,
        wireName: String,
    ): Schema =
        when (this) {
            is SchemaInterface.SchemaInline -> schema
            is SchemaInterface.SchemaReference ->
                linkedSchema()
                    ?: throw unsupported(
                        headerContractName = headerContractName,
                        wireName = wireName,
                        schemaType = "unresolved reference '${reference.ref}'",
                    )
            is SchemaInterface.BooleanSchema ->
                throw unsupported(
                    headerContractName = headerContractName,
                    wireName = wireName,
                    schemaType = "boolean schema",
                )
            is SchemaInterface.MultiFormatSchemaInline ->
                throw unsupported(
                    headerContractName = headerContractName,
                    wireName = wireName,
                    schemaType = "multi-format schema",
                )
        }

    private fun unsupported(
        headerContractName: String,
        wireName: String,
        schemaType: String,
    ): UnsupportedKafkaHeaderSchema =
        UnsupportedKafkaHeaderSchema(
            headerContractName = headerContractName,
            wireName = wireName,
            schemaType = schemaType,
        )
}

/**
 * The parameter type of a header.
 *
 * @param type Scalar type of the header.
 * @param schemaNullable Whether the header schema allows `null`.
 * @param description Description of the header schema.
 */
internal data class KafkaHeaderType(
    val type: TypeRef.Scalar,
    val schemaNullable: Boolean = false,
    val description: String? = null,
)
