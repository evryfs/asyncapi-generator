package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.kafka.KafkaKeySchemaResolver
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.hasMultipleNonNullTypes
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.isTypeNullable
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedKafkaKeySchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Resolves an AsyncAPI Kafka key schema into its generated client-contract type.
 *
 * Expected behavior is exercised by:
 * - `KafkaKeyContractResolverTest`
 * - `SpringKafkaClientApprovalTest`
 */
internal object KafkaKeyContractResolver {
    fun resolve(
        messageName: String,
        schema: SchemaInterface?,
        modelPackage: String? = null,
    ): KafkaKeyContract? {
        if (schema == null) return null

        val resolved = KafkaKeySchemaResolver.resolve(messageName, schema)
        val resolvedSchema = resolved.schema
        if (resolvedSchema.type.hasMultipleNonNullTypes()) {
            throw UnsupportedKafkaKeySchema(
                messageName = messageName,
                schemaType = "union with multiple non-null types",
            )
        }

        val keyType =
            KafkaScalarTypeResolver.resolve(resolvedSchema)
                ?.let { scalarType ->
                    KafkaKeyType(
                        javaTypeName = scalarType.javaTypeName,
                        kotlinTypeName = scalarType.kotlinTypeName,
                        importName = scalarType.importName,
                    )
                }
                ?: when (val schemaType = resolvedSchema.type.getPrimaryType()) {
                    "object" -> {
                        val modelName =
                            requireNotNull(resolved.modelName) {
                                "Object Kafka key model name was not resolved for '$messageName'"
                            }
                        KafkaKeyType(
                            javaTypeName = modelName,
                            kotlinTypeName = modelName,
                            importName = modelPackage?.let { "$it.$modelName" },
                            isModel = true,
                        )
                    }
                    else ->
                        throw UnsupportedKafkaKeySchema(
                            messageName = messageName,
                            schemaType = schemaType ?: "unspecified",
                        )
                }

        return KafkaKeyContract(
            schema = resolvedSchema,
            javaTypeName = keyType.javaTypeName,
            kotlinTypeName = keyType.kotlinTypeName,
            importName = keyType.importName,
            nullable = resolvedSchema.type.isTypeNullable(),
            isModel = keyType.isModel,
        )
    }
}

internal data class KafkaKeyContract(
    val schema: Schema,
    val javaTypeName: String,
    val kotlinTypeName: String,
    val importName: String? = null,
    val nullable: Boolean = false,
    val isModel: Boolean = false,
)

private data class KafkaKeyType(
    val javaTypeName: String,
    val kotlinTypeName: String,
    val importName: String? = null,
    val isModel: Boolean = false,
)
