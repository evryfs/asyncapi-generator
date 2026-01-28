package dev.banking.asyncapi.generator.core.validator.servers

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

    fun validateServers(servers: Map<String, ServerInterface>, results: ValidationResults) {
        servers.forEach { (serverName, serverInterface) ->
            when (serverInterface) {
                is ServerInterface.ServerInline ->
                    validate(serverInterface.server, serverName, results)

                is ServerInterface.ServerReference ->
                    referenceResolver.resolve(serverInterface.reference, "Server", results)
            }
        }
    }

    private fun validate(node: Server, serverName: String, results: ValidationResults) {
        validateHost(node, serverName, results)
        validateProtocol(node, serverName, results)
        validateProtocolVersion(node, serverName, results)
        validateVariables(node, serverName, results)
        validateSecurity(node, serverName, results)
        validateTags(node, serverName, results)
        validateExternalDocs(node, serverName, results)
        validateBindings(node, serverName, results)
    }

    private fun validateHost(node: Server, serverName: String, results: ValidationResults) {
        val host = node.host.let(::sanitizeString)
        if (host.isBlank()) {
            results.error(
                "Server '$serverName' must define a non-empty 'host'.",
                asyncApiContext.getLine(node, node::host)
            )
        } else {
            if (host.startsWith("http://") || host.startsWith("https://")) {
                results.warn(
                    "Server '$serverName' host '$host' includes scheme/protocol. 'host' should typically be the hostname (e.g. api.example.com) as protocol is defined separately.",
                    asyncApiContext.getLine(node, node::host)
                )
            }
            val hostRegex = Regex("""^[\w\-.:/{}\[\]%~@*!$'()+,;=?#]+(:\d+)?$""")
            if (!hostRegex.matches(host)) {
                results.warn(
                    "Server '$serverName' host '$host' looks unusual. Expected format 'hostname[:port]' or URL with variables/path. Found invalid characters.",
                    asyncApiContext.getLine(node, node::host)
                )
            }
        }

        // Variable Matching Logic
        val definedVars = node.variables?.keys ?: emptySet()
        val hostVars = Regex("""\{([^}]+)}""").findAll(host)
            .map { it.groupValues[1] }
            .toSet()

        val missing = hostVars - definedVars
        if (missing.isNotEmpty()) {
            results.error(
                "Server '$serverName' host uses variables $missing which are not defined in 'variables'.",
                asyncApiContext.getLine(node, node::host)
            )
        }

        val unused = definedVars - hostVars
        if (unused.isNotEmpty()) {
            results.warn(
                "Server '$serverName' defines variables $unused which are not used in the host '$host'.",
                asyncApiContext.getLine(node, node::variables)
            )
        }
    }

    private fun validateProtocol(node: Server, serverName: String, results: ValidationResults) {
        val protocol = node.protocol.let(::sanitizeString)
        if (protocol.isBlank()) {
            results.error(
                "Server '$serverName' must define the 'protocol' it supports.",
                asyncApiContext.getLine(node, node::protocol)
            )
        }
    }

    private fun validateProtocolVersion(node: Server, serverName: String, results: ValidationResults) {
        val version = node.protocolVersion?.let(::sanitizeString)
            ?: return
        if (version.isBlank()) {
            results.warn(
                "Server '$serverName' defines an empty 'protocolVersion' — omit if unknown.",
                asyncApiContext.getLine(node, node::protocolVersion)
            )
        }
    }

    private fun validateVariables(node: Server, serverName: String, results: ValidationResults) {
        val variables = node.variables
            ?: return
        if (variables.isEmpty()) {
            results.warn(
                "Server '$serverName' defines an empty 'variables' map — omit if unused.",
                asyncApiContext.getLine(node, node::variables)
            )
        }
        variables.forEach { (varName, variableInterface) ->
            when (variableInterface) {
                is ServerVariableInterface.ServerVariableInline ->
                    serverVariableValidator.validate(varName, variableInterface.serverVariable, results)

                is ServerVariableInterface.ServerVariableReference ->
                    referenceResolver.resolve(variableInterface.reference, "Server Variable", results)
            }
        }
    }

    private fun validateSecurity(node: Server, serverName: String, results: ValidationResults) {
        val security = node.security
            ?: return
        if (security.isEmpty()) {
            results.warn(
                "Server '$serverName' defines an empty 'security' list — omit if unused.",
                asyncApiContext.getLine(node, node::security)
            )
        }
        security.forEach { secInterface ->
            when (secInterface) {
                is SecuritySchemeInterface.SecuritySchemeInline ->
                    securitySchemeValidator.validate(secInterface.security, serverName, results)

                is SecuritySchemeInterface.SecuritySchemeReference ->
                    referenceResolver.resolve(secInterface.reference, "Server Security", results)
            }
        }
    }

    private fun validateTags(node: Server, serverName: String, results: ValidationResults) {
        val tags = node.tags
            ?: return
        if (tags.isEmpty()) {
            results.warn(
                "Server '$serverName' defines an empty 'tags' list.",
                asyncApiContext.getLine(node, node::tags)
            )
        }
        tags.forEach { tagInterface ->
            when (tagInterface) {
                is TagInterface.TagInline ->
                    tagValidator.validate(tagInterface.tag, serverName, results)

                is TagInterface.TagReference ->
                    referenceResolver.resolve(tagInterface.reference, "Server Tag", results)
            }
        }
    }

    private fun validateExternalDocs(node: Server, serverName: String, results: ValidationResults) {
        when (val docs = node.externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(docs.externalDoc, serverName, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(docs.reference, "Server ExternalDocs", results)

            null -> {}
        }
    }

    private fun validateBindings(node: Server, serverName: String, results: ValidationResults) {
        val bindings = node.bindings
            ?: return
        if (bindings.isEmpty()) {
            results.warn(
                "Server '$serverName' defines an empty 'bindings' object.",
                asyncApiContext.getLine(node, node::bindings)
            )
            return
        }
        bindings.forEach { (bindingName, bindingInterface) ->
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingInterface.binding, bindingName, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(bindingInterface.reference, "Server Binding", results)
            }
        }
    }
}
