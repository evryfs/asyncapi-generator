package dev.banking.asyncapi.generator.core.validator.servers

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.HOSTNAME
import dev.banking.asyncapi.generator.core.constants.RegexPatterns.PARAMETER_PLACEHOLDER
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.security.SecuritySchemeValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class ServerValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagValidator = TagValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val bindingValidator = BindingValidator(asyncApiContext)
    private val securitySchemeValidator = SecuritySchemeValidator(asyncApiContext)
    private val serverVariableValidator = ServerVariableValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: ServerInterface, contextString: String, results: ValidationResults) {
        when (node) {
            is ServerInterface.ServerInline ->
                validate(node.server, contextString, results)

            is ServerInterface.ServerReference ->
                referenceResolver.resolve(node.reference, contextString, results)
        }
    }

    private fun validate(node: Server, contextString: String, results: ValidationResults) {
        validateHost(node, contextString, results)
        validateProtocol(node, contextString, results)
        validateProtocolVersion(node, contextString, results)
        validateVariables(node, contextString, results)
        validateSecurity(node, contextString, results)
        validateTags(node, contextString, results)
        validateExternalDocs(node, contextString, results)
        validateBindings(node, contextString, results)
    }

    private fun validateHost(node: Server, contextString: String, results: ValidationResults) {
        val host = node.host.let(::sanitizeString)
        if (host.isBlank()) {
            results.error(
                "$contextString must define a non-empty 'host'.",
                asyncApiContext.getLine(node, node::host)
            )
        } else {
            if (host.startsWith("http://") || host.startsWith("https://")) {
                results.warn(
                    "$contextString host '$host' includes scheme/protocol. 'host' should typically be the hostname " +
                        "(e.g. api.example.com) as protocol is defined separately.",
                    asyncApiContext.getLine(node, node::host)
                )
            }
            if (!HOSTNAME.matches(host)) {
                results.warn(
                    "$contextString host '$host' looks unusual. Expected format 'hostname[:port]' or URL with " +
                        "variables/path. Found invalid characters.",
                    asyncApiContext.getLine(node, node::host)
                )
            }
        }
        // Variable Matching Logic
        val definedVars = node.variables?.keys ?: emptySet()
        val hostVars = PARAMETER_PLACEHOLDER
            .findAll(host)
            .map { it.groupValues[1] }
            .toSet()
        val missing = hostVars - definedVars
        if (missing.isNotEmpty()) {
            results.error(
                "$contextString host uses variables $missing which are not defined in 'variables'.",
                asyncApiContext.getLine(node, node::host)
            )
        }

        val unused = definedVars - hostVars
        if (unused.isNotEmpty()) {
            results.warn(
                "$contextString defines variables $unused which are not used in the host '$host'.",
                asyncApiContext.getLine(node, node::variables)
            )
        }
    }

    private fun validateProtocol(node: Server, contextString: String, results: ValidationResults) {
        val protocol = node.protocol.let(::sanitizeString)
        if (protocol.isBlank()) {
            results.error(
                "$contextString must define the 'protocol' it supports.",
                asyncApiContext.getLine(node, node::protocol)
            )
        }
    }

    private fun validateProtocolVersion(node: Server, contextString: String, results: ValidationResults) {
        val version = node.protocolVersion?.let(::sanitizeString) ?: return
        if (version.isBlank()) {
            results.warn(
                "$contextString defines an empty 'protocolVersion' — omit if unknown.",
                asyncApiContext.getLine(node, node::protocolVersion)
            )
        }
    }

    private fun validateVariables(node: Server, contextString: String, results: ValidationResults) {
        val variables = node.variables ?: return
        variables.forEach { (serverVariableName, serverVariableInterface) ->
            val contextString = "$contextString Server Variable '$serverVariableName'"
            when (serverVariableInterface) {
                is ServerVariableInterface.ServerVariableInline ->
                    serverVariableValidator.validate(serverVariableInterface.serverVariable, contextString, results)

                is ServerVariableInterface.ServerVariableReference ->
                    referenceResolver.resolve(serverVariableInterface.reference, contextString, results)
            }
        }
    }

    private fun validateSecurity(node: Server, contextString: String, results: ValidationResults) {
        val security = node.security ?: return
        security.forEachIndexed { index, securitySchemeInterface ->
            val contextString = "$contextString Security Scheme[$index]"
            when (securitySchemeInterface) {
                is SecuritySchemeInterface.SecuritySchemeInline ->
                    securitySchemeValidator.validate(securitySchemeInterface.security, contextString, results)

                is SecuritySchemeInterface.SecuritySchemeReference ->
                    referenceResolver.resolve(securitySchemeInterface.reference, contextString, results)
            }
        }
    }

    private fun validateTags(node: Server, contextString: String, results: ValidationResults) {
        val tags = node.tags ?: return
        tags.forEachIndexed { index, tagInterface ->
            val contextString = "$contextString Tag[$index]"
            when (tagInterface) {
                is TagInterface.TagInline ->
                    tagValidator.validate(tagInterface.tag, contextString, results)

                is TagInterface.TagReference ->
                    referenceResolver.resolve(tagInterface.reference, contextString, results)
            }
        }
    }

    private fun validateExternalDocs(node: Server, contextString: String, results: ValidationResults) {
        val contextString = "$contextString ExternalDocs"
        when (val docs = node.externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(docs.externalDoc, contextString, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(docs.reference, contextString, results)

            null -> {}
        }
    }

    private fun validateBindings(node: Server, contextString: String, results: ValidationResults) {
        val bindings = node.bindings ?: return
        bindings.forEach { (bindingName, bindingInterface) ->
            val contextString = "$contextString Binding '$bindingName'"
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingInterface.binding, contextString, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(bindingInterface.reference, contextString, results)
            }
        }
    }
}
