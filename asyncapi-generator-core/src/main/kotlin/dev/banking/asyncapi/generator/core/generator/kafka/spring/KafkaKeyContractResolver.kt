package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.messaging.kafka.KafkaKey
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ModelTypeMapper
import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedKafkaKeySchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema

/** Resolves an AsyncAPI Kafka key schema into its generated client-contract type. */
internal object KafkaKeyContractResolver {
    fun resolve(kafkaKey: KafkaKey?): KafkaKeyContract? {
        if (kafkaKey == null) return null

        val messageName = kafkaKey.messageName
        val resolvedSchema = kafkaKey.requireSchema()
        if (resolvedSchema.type?.hasMultipleNonNullTypes == true) {
            throw UnsupportedKafkaKeySchema(
                messageName = messageName,
                schemaType = "union with multiple non-null types",
            )
        }

        return KafkaKeyContract(
            schema = resolvedSchema,
            type = keyType(kafkaKey, resolvedSchema),
            nullable = resolvedSchema.type?.allowsNull == true,
        )
    }

    private fun keyType(
        kafkaKey: KafkaKey,
        resolvedSchema: Schema,
    ): TypeRef =
        ModelTypeMapper.scalarType(resolvedSchema)
            ?: kafkaKey.modelName?.let { modelName -> TypeRef.Named(name = modelName, isModel = true) }
            ?: throw UnsupportedKafkaKeySchema(
                messageName = kafkaKey.messageName,
                schemaType = resolvedSchema.type?.primary ?: "unspecified",
            )
}

/**
 * The record key parameter of a Spring Kafka client method.
 *
 * @param schema The key schema.
 * @param type Type of the key parameter: a scalar, or the generated key model.
 * @param nullable Whether the key schema allows `null`.
 */
internal data class KafkaKeyContract(
    val schema: Schema,
    val type: TypeRef,
    val nullable: Boolean = false,
) {
    /** Whether the key is a generated model. */
    val isModel: Boolean get() = type is TypeRef.Named
}
