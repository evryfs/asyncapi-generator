package dev.banking.asyncapi.generator.core.generator.avro

import com.fasterxml.jackson.databind.ObjectMapper
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidNativeAvroSchema
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import org.apache.avro.Schema

/**
 * Parses native Avro `schemaFormat` payloads into Apache Avro schemas.
 *
 * @param objectMapper writes schema content given as YAML or a map as JSON
 */
internal class NativeAvroSchemaParser(
    private val objectMapper: ObjectMapper = ObjectMapper(),
) {
    // Apache Avro reports an invalid schema with several unchecked exception types; each one means
    // the payload's schema is invalid.
    @Suppress("TooGenericExceptionCaught")
    fun parse(
        payloadName: String,
        schema: MultiFormatSchema,
    ): Schema =
        try {
            Schema.Parser().parse(schemaJson(schema.schema))
        } catch (ex: RuntimeException) {
            throw InvalidNativeAvroSchema(
                payloadName = payloadName,
                schemaFormat = schema.schemaFormat,
                reason = ex.message ?: ex::class.simpleName.orEmpty(),
            )
        }

    fun schemaJson(schema: Any?): String {
        requireNotNull(schema) { "Missing Avro schema content" }

        if (schema is String && schema.trim().startsWithJsonContainer()) {
            return schema.trim()
        }

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema)
    }

    private fun String.startsWithJsonContainer(): Boolean = startsWith("{") || startsWith("[") || startsWith("\"")
}
