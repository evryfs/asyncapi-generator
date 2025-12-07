package com.tietoevry.banking.asyncapi.generator.core.validator

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import com.tietoevry.banking.asyncapi.generator.core.validator.channels.ChannelValidator
import com.tietoevry.banking.asyncapi.generator.core.validator.components.ComponentValidator
import com.tietoevry.banking.asyncapi.generator.core.validator.info.InfoValidator
import com.tietoevry.banking.asyncapi.generator.core.validator.operations.OperationValidator
import com.tietoevry.banking.asyncapi.generator.core.validator.servers.ServerValidator
import com.tietoevry.banking.asyncapi.generator.core.validator.tags.TagValidator
import com.tietoevry.banking.asyncapi.generator.core.validator.util.ValidationResults
import com.tietoevry.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

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

        asyncApiDocument.info.let { info -> infoValidator.validate(info, results) }
        asyncApiDocument.channels?.let { channels -> channelValidator.validateChannels(channels, results) }
        asyncApiDocument.servers?.let { servers -> serverValidator.validateServers(servers, results) }
        asyncApiDocument.operations?.let { operations -> operationValidator.validateOperations(operations, results) }
        asyncApiDocument.components?.let { component -> componentValidator.validate(component, results) }
        return results
    }


    private fun validateAsyncApiVersion(node: AsyncApiDocument, results: ValidationResults) {
        val asyncApiVersion = node.asyncapi
        val versionRegex = Regex("""^\d+\.\d+\.\d+(-[A-Za-z0-9]+)?$""")

        if (asyncApiVersion.isBlank()) {
            results.error(
                "The 'asyncapi' field is required and cannot be empty.",
                asyncApiContext.getLine(node, node::asyncapi)
            )
        } else if (!versionRegex.matches(asyncApiVersion)) {
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
        val uriRegex = Regex("""^[a-zA-Z][a-zA-Z0-9+.-]*:.+$""") // RFC3986 format (loosely)

        if (!uriRegex.matches(id)) {
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
        val contentType = node.defaultContentType ?: return
        val mimeRegex = Regex("""^[a-zA-Z0-9!#$&^_.+-]+/[a-zA-Z0-9!#$&^_.+-]+$""")

        if (!mimeRegex.matches(contentType)) {
            results.error(
                "Invalid 'defaultContentType' format '$contentType'. Expected a MIME type (e.g., 'application/json').",
                asyncApiContext.getLine(node, node::defaultContentType)
            )
        }
    }
}
