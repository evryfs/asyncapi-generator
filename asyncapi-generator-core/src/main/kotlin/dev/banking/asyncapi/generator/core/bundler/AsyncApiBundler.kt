package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.bundler.channels.ChannelBundler
import dev.banking.asyncapi.generator.core.bundler.components.ComponentBundler
import dev.banking.asyncapi.generator.core.bundler.info.InfoBundler
import dev.banking.asyncapi.generator.core.bundler.operations.OperationBundler
import dev.banking.asyncapi.generator.core.bundler.servers.ServerBundler
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument

/**
 * Bundles a parsed and validated [AsyncApiDocument].
 *
 * The bundler stage resolves already-registered references into the model shape
 * expected by generator stages. It does not read files, parse YAML or JSON,
 * validate AsyncAPI semantics, or generate code.
 */
class AsyncApiBundler {

    private val infoBundler = InfoBundler()
    private val serverBundler = ServerBundler()
    private val channelBundler = ChannelBundler()
    private val operationBundler = OperationBundler()
    private val componentBundler = ComponentBundler()

    fun bundle(document: AsyncApiDocument): AsyncApiDocument {
        val context = BundlingContext.withRootSchemas(componentBundler.schemas(document.components))
        val bundledInfo = infoBundler.bundle(document.info, context)
        val bundledServers = serverBundler.bundleServers(document.servers, context)
        val bundledChannels = channelBundler.bundleMap(document.channels, context)
        val bundledOperations = operationBundler.bundleMap(document.operations, context)
        val bundledComponents = componentBundler.bundleComponents(document.components, context)
        val componentsWithPromotedSchemas = componentBundler.mergeSchemas(
            components = bundledComponents,
            schemas = context.schemaPromotions.schemas(),
        )

        return document.copy(
            info = bundledInfo,
            servers = bundledServers,
            channels = bundledChannels,
            operations = bundledOperations,
            components = componentsWithPromotedSchemas,
        )
    }
}
