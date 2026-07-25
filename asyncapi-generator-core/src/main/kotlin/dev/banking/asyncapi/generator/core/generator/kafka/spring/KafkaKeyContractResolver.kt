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
 * Expected behavior is covered by:
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
            when (val schemaType = resolvedSchema.type.getPrimaryType()) {
                "string" -> resolveString(resolvedSchema)
                "integer" -> resolveInteger(resolvedSchema)
                "number" -> resolveNumber(resolvedSchema)
                "boolean" -> KafkaKeyType(javaTypeName = "Boolean", kotlinTypeName = "Boolean")
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

    private fun resolveString(schema: Schema): KafkaKeyType =
        when (schema.format) {
            "uuid" -> KafkaKeyType("UUID", "UUID", "java.util.UUID")
            "date-time" -> KafkaKeyType("OffsetDateTime", "OffsetDateTime", "java.time.OffsetDateTime")
            "date" -> KafkaKeyType("LocalDate", "LocalDate", "java.time.LocalDate")
            "time" -> KafkaKeyType("LocalTime", "LocalTime", "java.time.LocalTime")
            else -> KafkaKeyType(javaTypeName = "String", kotlinTypeName = "String")
        }

    private fun resolveInteger(schema: Schema): KafkaKeyType {
        val format = schema.format?.lowercase()
        if (format == "int64") return KafkaKeyType("Long", "Long")
        if (format == "int32") return KafkaKeyType("Integer", "Int")

        val minimum = schema.minimum
        val maximum = schema.maximum
        val fitsInt =
            (minimum == null || minimum.toDouble() >= Int.MIN_VALUE.toDouble()) &&
                (maximum == null || maximum.toDouble() <= Int.MAX_VALUE.toDouble())

        return if (fitsInt) {
            KafkaKeyType("Integer", "Int")
        } else {
            KafkaKeyType("Long", "Long")
        }
    }

    private fun resolveNumber(schema: Schema): KafkaKeyType {
        if (schema.multipleOf != null) {
            return KafkaKeyType("BigDecimal", "BigDecimal", "java.math.BigDecimal")
        }
        return when (schema.format?.lowercase()) {
            "float" -> KafkaKeyType("Float", "Float")
            else -> KafkaKeyType("Double", "Double")
        }
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
