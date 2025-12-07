package dev.banking.asyncapi.generator.core.bundler.servers

import dev.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import dev.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import dev.banking.asyncapi.generator.core.bundler.security.SecuritySchemeBundler
import dev.banking.asyncapi.generator.core.bundler.tags.TagBundler
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface

class ServerBundler {

    private val tagBundler = TagBundler()
    private val externalDocsBundler = ExternalDocsBundler()
    private val securitySchemeBundler = SecuritySchemeBundler()
    private val serverVariableBundler = ServerVariableBundler()
    private val bindingBundler = BindingBundler()

    fun bundleServers(
        servers: Map<String, ServerInterface>?,
        visited: Set<String>,
    ): Map<String, ServerInterface>? {
        if (servers == null) return null

        return servers.mapValues { (_, serverInterface) ->
            when (serverInterface) {
                is ServerInterface.ServerInline ->
                    ServerInterface.ServerInline(
                        bundleServer(serverInterface.server, visited)
                    )

                is ServerInterface.ServerReference -> {
                    val ref = serverInterface.reference.ref
                    if (visited.contains(ref)) {
                        serverInterface
                    } else {
                        val serverModel = serverInterface.reference.requireModel<Server>()
                        val newVisited = visited + ref
                        val bundledServer = bundleServer(serverModel, newVisited)
                        serverInterface.reference.model = bundledServer
                        serverInterface.reference.inline()
                        serverInterface
                    }
                }
            }
        }
    }

    fun bundleServer(server: Server, visited: Set<String>): Server {
        val bundledVariables = serverVariableBundler.bundleMap(server.variables, visited)
        val bundledSecurity = securitySchemeBundler.bundleList(server.security, visited)
        val bundledTags = tagBundler.bundleList(server.tags, visited)
        val bundledExternalDocs = server.externalDocs?.let { externalDocsBundler.bundle(it, visited) }
        val bundledBindings = bindingBundler.bundleMap(server.bindings, visited)
        return server.copy(
            variables = bundledVariables,
            security = bundledSecurity,
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
            bindings = bundledBindings
        )
    }
}
