package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.bundler.channels.ChannelBundler
import dev.banking.asyncapi.generator.core.bundler.components.ComponentBundler
import dev.banking.asyncapi.generator.core.bundler.info.InfoBundler
import dev.banking.asyncapi.generator.core.bundler.operations.OperationBundler
import dev.banking.asyncapi.generator.core.bundler.servers.ServerBundler
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument

class AsyncApiBundler {

    private val infoBundler = InfoBundler()
    private val serverBundler = ServerBundler()
    private val channelBundler = ChannelBundler()
    private val operationBundler = OperationBundler()
    private val componentBundler = ComponentBundler()

    fun bundle(document: AsyncApiDocument): AsyncApiDocument {
        val visited = emptySet<String>()
        return document.copy(
            info = infoBundler.bundle(document.info, visited),
            servers = serverBundler.bundleServers(document.servers, visited),
            channels = channelBundler.bundleMap(document.channels, visited),
            operations = operationBundler.bundleMap(document.operations, visited),
            components = componentBundler.bundleComponents(document.components, visited),
        )
    }
}
