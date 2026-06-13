package dev.banking.asyncapi.generator.core.parser.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.context.ExternalReferencePathResolver
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException.NativeSchemaAssetReadFailure
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import java.io.IOException

/**
 * Reads external native schema assets referenced from multi-format schema
 * content.
 *
 * Native schema assets are not AsyncAPI documents, so they are read as text
 * instead of being routed through the YAML/JSON document reader.
 *
 * Expected behavior is covered by:
 * - `MultiFormatSchemaParserTest`
 */
class NativeSchemaAssetReader(
    private val asyncApiContext: AsyncApiContext,
) {
    private val pathResolver = ExternalReferencePathResolver(asyncApiContext)

    fun readIfExternalReference(schemaNode: ParserNode): String? {
        val reference = schemaNode.externalReferenceValue() ?: return null
        val sourceId = schemaNode.path.substringBefore(".root", missingDelimiterValue = "")
        val file =
            pathResolver.resolveFile(
                reference = reference,
                sourceId = sourceId.ifBlank { null },
            ) ?: return null

        return try {
            file.readText()
        } catch (exception: IOException) {
            throw NativeSchemaAssetReadFailure(
                reference = reference,
                path = schemaNode.path,
                context = asyncApiContext,
                reason = exception.message ?: exception::class.simpleName.orEmpty(),
            )
        }
    }

    private fun ParserNode.externalReferenceValue(): String? {
        val map = node as? Map<*, *> ?: return null
        val reference = map["\$ref"] as? String ?: return null
        return reference.takeIf { it.isNotBlank() }
    }
}
