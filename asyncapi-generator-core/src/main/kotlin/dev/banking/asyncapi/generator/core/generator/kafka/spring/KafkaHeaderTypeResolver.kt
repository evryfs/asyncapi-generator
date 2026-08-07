package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.hasMultipleNonNullTypes
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.isTypeNullable
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
            KafkaScalarTypeResolver.resolve(resolvedSchema)
                ?: throw unsupported(
                    headerContractName = headerContractName,
                    wireName = wireName,
                    schemaType = resolvedSchema.type.getPrimaryType() ?: "unspecified",
                )

        return KafkaHeaderType(
            javaTypeName = scalarType.javaTypeName,
            kotlinTypeName = scalarType.kotlinTypeName,
            importName = scalarType.importName,
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
                when (val model = reference.model) {
                    is Schema -> model
                    is SchemaInterface.SchemaInline -> model.schema
                    else ->
                        throw unsupported(
                            headerContractName = headerContractName,
                            wireName = wireName,
                            schemaType = "unresolved reference '${reference.ref}'",
                        )
                }
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

internal data class KafkaHeaderType(
    val javaTypeName: String,
    val kotlinTypeName: String,
    val importName: String? = null,
    val schemaNullable: Boolean = false,
    val description: String? = null,
)
