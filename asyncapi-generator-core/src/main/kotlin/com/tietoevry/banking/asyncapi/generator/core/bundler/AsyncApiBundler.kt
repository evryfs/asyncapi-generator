package com.tietoevry.banking.asyncapi.generator.core.bundler

import com.tietoevry.banking.asyncapi.generator.core.bundler.channels.ChannelBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.components.ComponentBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.info.InfoBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.operations.OperationBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.servers.ServerBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.tags.TagBundler
import com.tietoevry.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument

class AsyncApiBundler {

    private val infoBundler: InfoBundler = InfoBundler()
    private val serverBundler: ServerBundler = ServerBundler()
    private val channelBundler: ChannelBundler = ChannelBundler()
    private val operationBundler: OperationBundler = OperationBundler()
    private val componentBundler: ComponentBundler = ComponentBundler()

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
