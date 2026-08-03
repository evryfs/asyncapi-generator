package dev.banking.asyncapi.generator.core.parser.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.context.ExternalReferencePathResolver
import dev.banking.asyncapi.generator.core.document.DocumentObject
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
internal class NativeSchemaAssetReader(
    private val asyncApiContext: AsyncApiContext,
) {
    private val pathResolver = ExternalReferencePathResolver()

    fun readIfExternalReference(schemaNode: ParserNode): String? {
        val referenceMember = (schemaNode.node as? DocumentObject)?.member("\$ref") ?: return null
        val referenceNode = schemaNode.member("\$ref", referenceMember.value)
        val reference = referenceNode.expect<String>().takeIf { it.isNotBlank() } ?: return null
        val sourceFile =
            asyncApiContext.findFileById(schemaNode.address.sourceId)
                ?: asyncApiContext.getCurrentFile()
        val file =
            pathResolver.resolve(
                reference = reference,
                sourceFile = sourceFile,
            )?.file ?: return null

        return try {
            asyncApiContext.readNativeSchemaAsset(
                file = file,
                location = referenceNode.sourceLocation,
                path = referenceNode.path,
            )
        } catch (exception: IOException) {
            throw NativeSchemaAssetReadFailure(
                reference = reference,
                path = referenceNode.path,
                context = asyncApiContext,
                reason = exception.message ?: exception::class.simpleName.orEmpty(),
            )
        } catch (exception: SecurityException) {
            throw NativeSchemaAssetReadFailure(
                reference = reference,
                path = referenceNode.path,
                context = asyncApiContext,
                reason = exception.message ?: exception::class.simpleName.orEmpty(),
            )
        }
    }
}
