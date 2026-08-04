package dev.banking.asyncapi.generator.core.validator.servers

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.PARAMETER_PLACEHOLDER
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_HOST_CONTAINS_PROTOCOL
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_HOST_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_NAME_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_PROTOCOL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_UNDEFINED
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.security.SecuritySchemeValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats

internal class ServerValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagValidator = TagValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val bindingValidator = BindingValidator(asyncApiContext)
    private val securitySchemeValidator = SecuritySchemeValidator(asyncApiContext)
    private val serverVariableValidator = ServerVariableValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(
        node: ServerInterface,
        contextString: String,
        results: ValidationCollector,
        name: String? = null,
    ) {
        if (name != null && !ValidationFormats.isReusableObjectName(name)) {
            val model = when (node) {
                is ServerInterface.ServerInline -> node.server
                is ServerInterface.ServerReference -> node.reference
            }
            results.error(
                SERVER_NAME_FORMAT,
                "$contextString name must contain only letters, digits, underscores, or hyphens.",
                sourceLocation = asyncApiContext.getSourceLocation(model),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serversObject",
            )
        }
        when (node) {
            is ServerInterface.ServerInline ->
                validate(node.server, contextString, results)

            is ServerInterface.ServerReference ->
                referenceResolver.resolve(node.reference, SERVER, contextString, results)
        }
    }

    private fun validate(node: Server, contextString: String, results: ValidationCollector) {
        if (!results.visit(node)) return
        validateHost(node, contextString, results)
        validateProtocol(node, contextString, results)
        validateVariables(node, contextString, results)
        validateSecurity(node, contextString, results)
        validateTags(node, contextString, results)
        validateExternalDocs(node, contextString, results)
        validateBindings(node, contextString, results)
    }

    private fun validateHost(node: Server, contextString: String, results: ValidationCollector) {
        val host = node.host
        if (host.isBlank()) {
            results.error(
                SERVER_HOST_REQUIRED,
                "$contextString must define a non-empty 'host'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::host),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverObject",
            )
        } else if (host.contains("://")) {
            results.warn(
                SERVER_HOST_CONTAINS_PROTOCOL,
                "$contextString host '$host' includes scheme/protocol. 'host' should typically be the hostname " +
                    "(e.g. api.example.com) as protocol is defined separately.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::host),
            )
        }
        val definedVars = node.variables?.keys ?: emptySet()
        val hostVars = PARAMETER_PLACEHOLDER.findAll(host).map { it.groupValues[1] }.toSet()
        val pathVars = node.pathname
            ?.let(PARAMETER_PLACEHOLDER::findAll)
            ?.map { it.groupValues[1] }
            ?.toSet()
            ?: emptySet()
        val missingHostVars = hostVars - definedVars
        if (missingHostVars.isNotEmpty()) {
            results.error(
                SERVER_VARIABLE_UNDEFINED,
                "$contextString host uses variables $missingHostVars which are not defined in 'variables'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::host),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverObject",
            )
        }
        val missingPathVars = pathVars - definedVars
        if (missingPathVars.isNotEmpty()) {
            results.error(
                SERVER_VARIABLE_UNDEFINED,
                "$contextString pathname uses variables $missingPathVars which are not defined in 'variables'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::pathname),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverObject",
            )
        }
    }

    private fun validateProtocol(node: Server, contextString: String, results: ValidationCollector) {
        val protocol = node.protocol
        if (protocol.isBlank()) {
            results.error(
                SERVER_PROTOCOL_REQUIRED,
                "$contextString must define the 'protocol' it supports.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::protocol),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverObject",
            )
        }
    }

    private fun validateVariables(node: Server, contextString: String, results: ValidationCollector) {
        serverVariableValidator.validateMap(node.variables, contextString, results)
    }

    private fun validateSecurity(node: Server, contextString: String, results: ValidationCollector) {
        securitySchemeValidator.validateList(
            node.security ?: return,
            "$contextString Security Scheme",
            results,
        )
    }

    private fun validateTags(node: Server, contextString: String, results: ValidationCollector) {
        tagValidator.validateList(node.tags, contextString, results)
    }

    private fun validateExternalDocs(node: Server, contextString: String, results: ValidationCollector) {
        val externalDocs = node.externalDocs ?: return
        externalDocsValidator.validateInterface(externalDocs, "$contextString ExternalDocs", results)
    }

    private fun validateBindings(node: Server, contextString: String, results: ValidationCollector) {
        bindingValidator.validateMap(node.bindings, contextString, results)
    }
}
