package dev.banking.asyncapi.generator.core.validator

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.MIME_TYPE
import dev.banking.asyncapi.generator.core.constants.RegexPatterns.SEMANTIC_VERSION
import dev.banking.asyncapi.generator.core.constants.RegexPatterns.URI
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.validator.channels.ChannelValidator
import dev.banking.asyncapi.generator.core.validator.components.ComponentValidator
import dev.banking.asyncapi.generator.core.validator.info.InfoValidator
import dev.banking.asyncapi.generator.core.validator.operations.OperationValidator
import dev.banking.asyncapi.generator.core.validator.servers.ServerValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class AsyncApiValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val infoValidator = InfoValidator(asyncApiContext)
    private val channelValidator = ChannelValidator(asyncApiContext)
    private val serverValidator = ServerValidator(asyncApiContext)
    private val operationValidator = OperationValidator(asyncApiContext)
    private val componentValidator = ComponentValidator(asyncApiContext)

    fun validate(asyncApiDocument: AsyncApiDocument): ValidationResults {
        val results = ValidationResults(asyncApiContext)

        validateAsyncApiVersion(asyncApiDocument, results)
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
        return results
    }

    private fun validateAsyncApiVersion(node: AsyncApiDocument, results: ValidationResults) {
        val asyncApiVersion = node.asyncapi.let(::sanitizeString)

        if (asyncApiVersion.isBlank()) {
            results.error(
                "The 'asyncapi' field is required and cannot be empty.",
                asyncApiContext.getLine(node, node::asyncapi)
            )
        } else if (!SEMANTIC_VERSION.matches(asyncApiVersion)) {
            results.error(
                "Invalid AsyncAPI version format '$asyncApiVersion'. Expected 'major.minor.patch' (e.g., 3.0.0).",
                asyncApiContext.getLine(node, node::asyncapi)
            )
        } else if (!asyncApiVersion.startsWith("3.")) {
            results.error(
                "AsyncAPI version '$asyncApiVersion' is not be supported by this plugin.",
                asyncApiContext.getLine(node, node::asyncapi)
            )
        }
    }

    private fun validateIdentifier(node: AsyncApiDocument, results: ValidationResults) {
        val id = node.id?.let(::sanitizeString) ?: return
        // RFC3986 format (loosely)
        if (!URI.matches(id)) {
            results.error(
                "The 'id' field must conform to the URI format (RFC3986). Got '$id'.",
                asyncApiContext.getLine(node, node::id)
            )
        } else if (!id.startsWith("urn:")) {
            results.warn(
                "It is RECOMMENDED to use a URN for the 'id' field to ensure global uniqueness.",
                asyncApiContext.getLine(node, node::id)
            )
        }
    }

    private fun validateDefaultContentType(node: AsyncApiDocument, results: ValidationResults) {
        val contentType = node.defaultContentType?.let(::sanitizeString) ?: return
        if (!MIME_TYPE.matches(contentType)) {
            results.error(
                "Invalid 'defaultContentType' format '$contentType'. Expected a MIME type (e.g., 'application/json').",
                asyncApiContext.getLine(node, node::defaultContentType)
            )
        }
    }
}
