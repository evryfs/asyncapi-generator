package dev.banking.asyncapi.generator.core.generator.kafka.spring

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
    ): KafkaKeyContract? {
        if (schema == null) return null

        val resolvedSchema = schema.resolve(messageName)
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
            nullable = resolvedSchema.nullable == true || resolvedSchema.type.isTypeNullable(),
        )
    }

    private fun SchemaInterface.resolve(messageName: String): Schema =
        when (this) {
            is SchemaInterface.SchemaInline -> schema
            is SchemaInterface.SchemaReference ->
                reference.model as? Schema
                    ?: throw UnsupportedKafkaKeySchema(
                        messageName = messageName,
                        schemaType = "unresolved schema reference '${reference.ref}'",
                    )
            is SchemaInterface.BooleanSchema ->
                throw UnsupportedKafkaKeySchema(
                    messageName = messageName,
                    schemaType = "boolean schema",
                )
            is SchemaInterface.MultiFormatSchemaInline ->
                throw UnsupportedKafkaKeySchema(
                    messageName = messageName,
                    schemaType = "multi-format schema '${multiFormatSchema.schemaFormat}'",
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
)

private data class KafkaKeyType(
    val javaTypeName: String,
    val kotlinTypeName: String,
    val importName: String? = null,
)
