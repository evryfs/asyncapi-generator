package com.tietoevry.banking.asyncapi.generator.core.bundler.servers

import com.tietoevry.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.security.SecuritySchemeBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.tags.TagBundler
import com.tietoevry.banking.asyncapi.generator.core.model.servers.Server
import com.tietoevry.banking.asyncapi.generator.core.model.servers.ServerInterface

class ServerBundler {

    private val tagBundler: TagBundler = TagBundler()
    private val externalDocsBundler: ExternalDocsBundler = ExternalDocsBundler()
    private val securitySchemeBundler: SecuritySchemeBundler = SecuritySchemeBundler()
    private val serverVariableBundler = ServerVariableBundler()
    private val bindingBundler = BindingBundler()

    fun bundleServers(
        servers: Map<String, ServerInterface>?,
        visited: Set<String>, // Accept visited here
    ): Map<String, ServerInterface>? {
        if (servers == null) return null

        return servers.mapValues { (_, serverInterface) ->
            when (serverInterface) {
                is ServerInterface.ServerInline ->
                    ServerInterface.ServerInline(
                        bundleServer(serverInterface.server, visited) // Pass visited
                    )

                is ServerInterface.ServerReference -> {
                    val ref = serverInterface.reference.ref
                    if (visited.contains(ref)) {
                        // Cycle detected! Return as is.
                        serverInterface
                    } else {
                        val serverModel = serverInterface.reference.requireModel<Server>()
                        val newVisited = visited + ref // Add current ref to visited
                        val bundledServer = bundleServer(serverModel, newVisited) // Pass newVisited
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
