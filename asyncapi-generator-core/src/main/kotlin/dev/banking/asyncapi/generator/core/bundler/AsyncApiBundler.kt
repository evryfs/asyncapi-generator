package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.loader.AsyncApiDocumentLoadResult
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument

/**
 * Bundles a parsed and validated [AsyncApiDocument].
 *
 * The bundler stage resolves already-registered references into the model shape
 * expected by generator stages. It does not read files, parse YAML or JSON,
 * validate AsyncAPI semantics, or generate code.
 */
class AsyncApiBundler {
    private val walk = BundlingWalk()

    /** Bundles a loaded and validated document. */
    fun bundle(loaded: AsyncApiDocumentLoadResult): BundledAsyncApiDocument = bundle(loaded.document)

    /** Bundles [document]; every call starts a new [BundlingRun]. */
    internal fun bundle(document: AsyncApiDocument): BundledAsyncApiDocument {
        val run = BundlingRun(rootSchemas = document.components?.schemas.orEmpty())
        val scope = run.rootScope()
        val bundledInfo = walk.info.bundle(document.info, scope)
        val bundledServers = walk.members(document.servers, walk.server, scope)
        val bundledChannels = walk.members(document.channels, walk.channel, scope)
        val bundledOperations = walk.members(document.operations, walk.operation, scope)
        val bundledComponents = document.components?.let { walk.components.bundle(it, scope) }

        return BundledAsyncApiDocument(
            document.copy(
                info = bundledInfo,
                servers = bundledServers,
                channels = bundledChannels,
                operations = bundledOperations,
                components = walk.components.withPromotedSchemas(bundledComponents, run.schemaPromotions.schemas()),
            ),
        )
    }
}
