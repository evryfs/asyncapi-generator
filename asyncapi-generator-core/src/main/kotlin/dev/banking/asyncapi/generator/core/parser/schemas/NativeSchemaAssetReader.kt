package dev.banking.asyncapi.generator.core.parser.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.references.parseReference
import dev.banking.asyncapi.generator.core.parser.diagnostics.parserFailure
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.source.DocumentObject
import java.io.File
import java.io.IOException

/**
 * Reads external native schema assets referenced from multi-format schema
 * content.
 *
 * Native schema assets are not AsyncAPI documents, so they are read as text
 * instead of being routed through the YAML/JSON document reader.
 *
 * @param asyncApiContext the load whose budget the assets count against
 */
internal class NativeSchemaAssetReader(
    private val asyncApiContext: AsyncApiContext,
) {
    /**
     * The content of the file the `$ref` of [schemaNode] points at, or `null` when the node has no
     * `$ref` to a file.
     */
    fun readIfExternalReference(schemaNode: ParserNode): String? {
        val referenceNode =
            (schemaNode.node as? DocumentObject)?.member($$"$ref")?.let { member ->
                schemaNode.member($$"$ref", member.value)
            }
        val reference = referenceNode?.expect<String>()?.takeIf(String::isNotBlank)
        val file = reference?.parseReference()?.resolveDocumentAgainst(sourceFile(schemaNode))
        return if (referenceNode != null && reference != null && file != null) {
            read(referenceNode, reference, file)
        } else {
            null
        }
    }

    private fun sourceFile(schemaNode: ParserNode): File =
        checkNotNull(asyncApiContext.sourceTracking.file(schemaNode.address.sourceId)) {
            "Source ${schemaNode.address.sourceId} is not registered"
        }

    private fun read(
        referenceNode: ParserNode,
        reference: String,
        file: File,
    ): String =
        try {
            asyncApiContext.sourceBudget.readNativeSchemaAsset(
                file = file,
                location = referenceNode.sourceLocation,
                path = referenceNode.path,
            )
        } catch (exception: IOException) {
            throw unreadable(referenceNode, reference, exception)
        } catch (exception: SecurityException) {
            throw unreadable(referenceNode, reference, exception)
        }

    private fun unreadable(
        referenceNode: ParserNode,
        reference: String,
        cause: Exception,
    ) = asyncApiContext
        .parserFailure(
            ParserDiagnostic.NativeSchemaAssetUnreadable(
                reference = reference,
                reason = cause.message ?: cause::class.simpleName.orEmpty(),
                path = referenceNode.path,
                sourceLocation = referenceNode.declarationLocation,
            ),
        ).also { failure -> failure.initCause(cause) }
}
