package dev.banking.asyncapi.generator.core.validator

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.validator.ValidationReport
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_DEFAULT_CONTENT_TYPE_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_ID_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_ID_URN_RECOMMENDED
import dev.banking.asyncapi.generator.core.validator.bindings.KafkaSchemaRegistryValidator
import dev.banking.asyncapi.generator.core.validator.channels.ChannelValidator
import dev.banking.asyncapi.generator.core.validator.components.ComponentValidator
import dev.banking.asyncapi.generator.core.validator.info.InfoValidator
import dev.banking.asyncapi.generator.core.validator.operations.OperationValidator
import dev.banking.asyncapi.generator.core.validator.servers.ServerValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats

/**
 * Validates a parsed [AsyncApiDocument] and returns validation results.
 */
internal class AsyncApiValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val infoValidator = InfoValidator(asyncApiContext)
    private val channelValidator = ChannelValidator(asyncApiContext)
    private val serverValidator = ServerValidator(asyncApiContext)
    private val operationValidator = OperationValidator(asyncApiContext)
    private val componentValidator = ComponentValidator(asyncApiContext)
    private val referenceTargetTraversal = ReferenceTargetTraversal(asyncApiContext)
    private val kafkaSchemaRegistryValidator = KafkaSchemaRegistryValidator(asyncApiContext)

    fun validate(asyncApiDocument: AsyncApiDocument): ValidationReport {
        val results = ValidationCollector()
        results.visit(asyncApiDocument)

        validateIdentifier(asyncApiDocument, results)
        validateDefaultContentType(asyncApiDocument, results)
        infoValidator.validate(asyncApiDocument.info, "Info", results)
        asyncApiDocument.channels?.forEach { (name, channel) ->
            channelValidator.validateInterface(channel, "Channel '$name'", results)
        }
        asyncApiDocument.servers?.forEach { (name, server) ->
            serverValidator.validateInterface(server, "Server '$name'", results, name)
        }
        asyncApiDocument.operations?.forEach { (name, operation) ->
            operationValidator.validateInterface(
                operation,
                "Operation '$name'",
                results,
                rootChannels = asyncApiDocument.channels.orEmpty(),
            )
        }
        asyncApiDocument.components?.let { components ->
            componentValidator.validateInterface(components, "Component", results)
        }

        referenceTargetTraversal.drain(results)
        kafkaSchemaRegistryValidator.validate(asyncApiDocument, results)
        return results.report()
    }

    private fun validateIdentifier(node: AsyncApiDocument, results: ValidationCollector) {
        val id = node.id ?: return
        val uri = ValidationFormats.absoluteUri(id)
        if (uri == null) {
            results.error(
                DOCUMENT_ID_FORMAT,
                "The 'id' field must conform to the URI format (RFC3986). Got '$id'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::id),
            )
        } else if (!uri.scheme.equals("urn", ignoreCase = true)) {
            results.warn(
                DOCUMENT_ID_URN_RECOMMENDED,
                "It is RECOMMENDED to use a URN for the 'id' field to ensure global uniqueness.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::id),
            )
        }
    }

    private fun validateDefaultContentType(node: AsyncApiDocument, results: ValidationCollector) {
        val contentType = node.defaultContentType ?: return
        if (!ValidationFormats.isSpecificMediaType(contentType)) {
            results.error(
                DOCUMENT_DEFAULT_CONTENT_TYPE_FORMAT,
                "Invalid 'defaultContentType' format '$contentType'. Expected a MIME type (e.g., 'application/json').",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::defaultContentType),
            )
        }
    }
}
