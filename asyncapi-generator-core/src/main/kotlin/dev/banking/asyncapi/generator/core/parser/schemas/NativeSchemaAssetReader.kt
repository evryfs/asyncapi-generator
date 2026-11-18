package dev.banking.asyncapi.generator.core.parser.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException.NativeSchemaAssetReadFailure
import dev.banking.asyncapi.generator.core.model.references.parseReference
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.source.DocumentObject
import java.io.IOException

/**
 * Reads external native schema assets referenced from multi-format schema
 * content.
 *
 * Native schema assets are not AsyncAPI documents, so they are read as text
 * instead of being routed through the YAML/JSON document reader.
 */
internal class NativeSchemaAssetReader(
    private val asyncApiContext: AsyncApiContext,
) {
    fun readIfExternalReference(schemaNode: ParserNode): String? {
        val referenceMember = (schemaNode.node as? DocumentObject)?.member("\$ref") ?: return null
        val referenceNode = schemaNode.member("\$ref", referenceMember.value)
        val reference = referenceNode.expect<String>().takeIf { it.isNotBlank() } ?: return null
        val sourceFile =
            asyncApiContext.sourceTracking.file(schemaNode.address.sourceId)
                ?: asyncApiContext.sourceTracking.currentFile()
        val file = reference.parseReference().resolveDocumentAgainst(sourceFile) ?: return null

        return try {
            asyncApiContext.sourceBudget.readNativeSchemaAsset(
                file = file,
                location = referenceNode.sourceLocation,
                path = referenceNode.path,
            )
        } catch (exception: IOException) {
            throw NativeSchemaAssetReadFailure(
                reference = reference,
                path = referenceNode.path,
                sourceLocation = referenceNode.sourceLocation,
                context = asyncApiContext,
                reason = exception.message ?: exception::class.simpleName.orEmpty(),
            )
        } catch (exception: SecurityException) {
            throw NativeSchemaAssetReadFailure(
                reference = reference,
                path = referenceNode.path,
                sourceLocation = referenceNode.sourceLocation,
                context = asyncApiContext,
                reason = exception.message ?: exception::class.simpleName.orEmpty(),
            )
        }
    }
}
