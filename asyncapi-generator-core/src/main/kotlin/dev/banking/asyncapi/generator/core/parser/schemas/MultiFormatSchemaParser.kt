package dev.banking.asyncapi.generator.core.parser.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException.UnexpectedSchemaFormat
import dev.banking.asyncapi.generator.core.model.schemas.SchemaFormat

/**
 * [MultiFormatSchemaParser] parses the `schemaFormat` value from AsyncAPI schema objects.
 *
 * Its primary responsibility is to translate known schema format strings into a typed
 * [SchemaFormat], so later parser and generator stages can distinguish AsyncAPI schema
 * objects from native schema formats such as Avro and Protobuf.
 *
 * **Current State:**
 * - Standard AsyncAPI schema formats are parsed and then handled by the regular [SchemaParser].
 * - Other known formats are preserved as multi-format schemas. Generator support for these
 *   formats is handled by later stages.
 * - For any unknown `schemaFormat` string, it throws an [UnexpectedSchemaFormat] error.
 *
 * Expected behavior is covered by:
 * - `MultiFormatSchemaParserTest`
 * - `SchemaParserTest`
 */
class MultiFormatSchemaParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseFormat(format: String, path: String): SchemaFormat =
        SchemaFormat.fromValue(format)
            ?: throw UnexpectedSchemaFormat(format, path, asyncApiContext)
}
