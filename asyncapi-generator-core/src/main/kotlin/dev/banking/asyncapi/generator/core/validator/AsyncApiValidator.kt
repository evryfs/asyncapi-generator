package dev.banking.asyncapi.generator.core.validator

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.model.validator.ValidationReport
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_CONTENT_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_ID_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_ID_URN
import dev.banking.asyncapi.generator.core.validator.bindings.KafkaSchemaRegistryRule
import dev.banking.asyncapi.generator.core.validator.components.ComponentRules
import dev.banking.asyncapi.generator.core.validator.info.InfoRules
import dev.banking.asyncapi.generator.core.validator.operations.OperationBoundaryRule
import dev.banking.asyncapi.generator.core.validator.servers.ServerRules
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates a parsed [AsyncApiDocument] and returns validation results.
 */
internal class AsyncApiValidator(
    val asyncApiContext: AsyncApiContext,
) {
    private val infoRules = InfoRules(asyncApiContext)
    private val componentRules = ComponentRules(asyncApiContext)
    private val documentRules: List<DocumentRule> =
        listOf(OperationBoundaryRule(asyncApiContext), KafkaSchemaRegistryRule(asyncApiContext))

    /** Validates [asyncApiDocument] and every object it reaches, and returns the findings. */
    fun validate(asyncApiDocument: AsyncApiDocument): ValidationReport {
        val results = ValidationCollector()
        val walk = ValidationWalk(asyncApiContext, results)
        results.visit(asyncApiDocument)

        validateIdentifier(asyncApiDocument, results)
        validateDefaultContentType(asyncApiDocument, results)
        infoRules.check(asyncApiDocument.info, ValidationPath.of("Info"), walk)
        asyncApiDocument.channels?.forEach { (name, channel) ->
            walk.member(channel, CHANNEL, ValidationPath.named("Channel", name))
        }
        asyncApiDocument.servers?.forEach { (name, server) ->
            val path = ValidationPath.named("Server", name)
            ServerRules.checkName(name, server, path, walk)
            walk.member(server, SERVER, path)
        }
        asyncApiDocument.operations?.forEach { (name, operation) ->
            walk.member(operation, OPERATION, ValidationPath.named("Operation", name))
        }
        asyncApiDocument.components?.let { components ->
            componentRules.check(components, ValidationPath.of("Component"), walk)
        }

        walk.drainReferenceTargets()
        documentRules.forEach { rule -> rule.check(asyncApiDocument, results) }
        return results.report()
    }

    private fun validateIdentifier(
        node: AsyncApiDocument,
        results: ValidationCollector,
    ) {
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
                DOCUMENT_ID_URN,
                "It is RECOMMENDED to use a URN for the 'id' field to ensure global uniqueness.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::id),
            )
        }
    }

    private fun validateDefaultContentType(
        node: AsyncApiDocument,
        results: ValidationCollector,
    ) {
        val contentType = node.defaultContentType ?: return
        if (!ValidationFormats.isSpecificMediaType(contentType)) {
            results.error(
                DOCUMENT_CONTENT_TYPE,
                "Invalid 'defaultContentType' format '$contentType'. Expected a MIME type (e.g., 'application/json').",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::defaultContentType),
            )
        }
    }
}
