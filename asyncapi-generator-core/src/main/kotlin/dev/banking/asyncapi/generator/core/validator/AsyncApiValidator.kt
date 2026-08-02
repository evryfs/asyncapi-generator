package dev.banking.asyncapi.generator.core.validator

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.MIME_TYPE
import dev.banking.asyncapi.generator.core.constants.RegexPatterns.URI
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.validator.ValidationReport
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_DEFAULT_CONTENT_TYPE_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_ID_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_ID_URN_RECOMMENDED
import dev.banking.asyncapi.generator.core.validator.channels.ChannelValidator
import dev.banking.asyncapi.generator.core.validator.components.ComponentValidator
import dev.banking.asyncapi.generator.core.validator.info.InfoValidator
import dev.banking.asyncapi.generator.core.validator.operations.OperationValidator
import dev.banking.asyncapi.generator.core.validator.servers.ServerValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

/**
 * Validates a parsed [AsyncApiDocument] and returns validation results.
 *
 * Expected behavior is covered by:
 * - `AsyncApiValidatorTest`
 */
class AsyncApiValidator(
    val asyncApiContext: AsyncApiContext,
) : ValidationStage {

    private val infoValidator = InfoValidator(asyncApiContext)
    private val channelValidator = ChannelValidator(asyncApiContext)
    private val serverValidator = ServerValidator(asyncApiContext)
    private val operationValidator = OperationValidator(asyncApiContext)
    private val componentValidator = ComponentValidator(asyncApiContext)

    override fun validate(asyncApiDocument: AsyncApiDocument): ValidationReport {
        val results = ValidationCollector(AsyncApiValidationProfile.select(asyncApiDocument))

        validateIdentifier(asyncApiDocument, results)
        validateDefaultContentType(asyncApiDocument, results)

        asyncApiDocument.info.let { info ->
            val contextString = "Info"
            infoValidator.validate(info, contextString, results)
        }
        asyncApiDocument.channels?.forEach { (channelName, channelInterface) ->
            val contextString = "Channel '$channelName'"
            channelValidator.validateInterface(channelInterface, contextString, results)
        }
        asyncApiDocument.servers?.forEach { (serverName, serverInterface) ->
            val contextString = "Server '$serverName'"
            serverValidator.validateInterface(serverInterface, contextString, results)
        }
        asyncApiDocument.operations?.forEach { (operationName, operationInterface) ->
            val contextString = "Operation '$operationName'"
            operationValidator.validateInterface(operationInterface, contextString, results)
        }
        asyncApiDocument.components?.let { component ->
            val contextString = "Component"
            componentValidator.validateInterface(component, contextString, results)
        }
        return results.report()
    }

    private fun validateIdentifier(node: AsyncApiDocument, results: ValidationCollector) {
        val id = node.id?.let(::sanitizeString) ?: return
        // RFC3986 format (loosely)
        if (!URI.matches(id)) {
            results.error(
                DOCUMENT_ID_FORMAT,
                "The 'id' field must conform to the URI format (RFC3986). Got '$id'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::id),
            )
        } else if (!id.startsWith("urn:")) {
            results.warn(
                DOCUMENT_ID_URN_RECOMMENDED,
                "It is RECOMMENDED to use a URN for the 'id' field to ensure global uniqueness.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::id),
            )
        }
    }

    private fun validateDefaultContentType(node: AsyncApiDocument, results: ValidationCollector) {
        val contentType = node.defaultContentType?.let(::sanitizeString) ?: return
        if (!MIME_TYPE.matches(contentType)) {
            results.error(
                DOCUMENT_DEFAULT_CONTENT_TYPE_FORMAT,
                "Invalid 'defaultContentType' format '$contentType'. Expected a MIME type (e.g., 'application/json').",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::defaultContentType),
            )
        }
    }
}
