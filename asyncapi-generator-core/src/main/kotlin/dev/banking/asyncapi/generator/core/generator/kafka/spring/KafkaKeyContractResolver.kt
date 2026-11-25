package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.messaging.kafka.KafkaKey
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.hasMultipleNonNullTypes
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.isTypeNullable
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedKafkaKeySchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema

/** Resolves an AsyncAPI Kafka key schema into its generated client-contract type. */
internal object KafkaKeyContractResolver {
    fun resolve(
        kafkaKey: KafkaKey?,
        modelPackage: String? = null,
    ): KafkaKeyContract? {
        if (kafkaKey == null) return null

        val messageName = kafkaKey.messageName
        val resolvedSchema = kafkaKey.requireSchema()
        if (resolvedSchema.type.hasMultipleNonNullTypes()) {
            throw UnsupportedKafkaKeySchema(
                messageName = messageName,
                schemaType = "union with multiple non-null types",
            )
        }

        val keyType = keyType(kafkaKey, resolvedSchema, modelPackage)
        return KafkaKeyContract(
            schema = resolvedSchema,
            javaTypeName = keyType.javaTypeName,
            kotlinTypeName = keyType.kotlinTypeName,
            importName = keyType.importName,
            nullable = resolvedSchema.type.isTypeNullable(),
            isModel = keyType.isModel,
        )
    }

    private fun keyType(
        kafkaKey: KafkaKey,
        resolvedSchema: Schema,
        modelPackage: String?,
    ): KafkaKeyType =
        KafkaScalarTypeResolver
            .resolve(resolvedSchema)
            ?.let { scalarType ->
                KafkaKeyType(
                    javaTypeName = scalarType.javaTypeName,
                    kotlinTypeName = scalarType.kotlinTypeName,
                    importName = scalarType.importName,
                )
            }
            ?: kafkaKey.modelName?.let { modelName ->
                KafkaKeyType(
                    javaTypeName = modelName,
                    kotlinTypeName = modelName,
                    importName = modelPackage?.let { "$it.$modelName" },
                    isModel = true,
                )
            }
            ?: throw UnsupportedKafkaKeySchema(
                messageName = kafkaKey.messageName,
                schemaType = resolvedSchema.type.getPrimaryType() ?: "unspecified",
            )
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
