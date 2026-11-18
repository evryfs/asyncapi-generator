package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.parser.external.ParsedReferences
import dev.banking.asyncapi.generator.core.parser.linking.ReferenceLinker
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.tracking.ModelTracking
import dev.banking.asyncapi.generator.core.parser.tracking.NodeLocations
import dev.banking.asyncapi.generator.core.parser.tracking.SourceTracking
import dev.banking.asyncapi.generator.core.source.SourceBudget
import dev.banking.asyncapi.generator.core.source.SourceId
import dev.banking.asyncapi.generator.core.source.SourceLimits
import dev.banking.asyncapi.generator.core.source.SourceLocation
import java.io.File

/**
 * The state of one load: its sources, the location of every node and model, its references with
 * their links, and its resource budget.
 *
 * A new context is created for each call to [dev.banking.asyncapi.generator.core.loader.AsyncApiDocumentLoader.load].
 *
 * @param sourceLimits limits for reading the documents and native schema assets of this load
 */
internal class AsyncApiContext(
    val sourceLimits: SourceLimits = SourceLimits(),
) {
    val sourceTracking = SourceTracking()
    val nodeLocations = NodeLocations()
    val modelTracking = ModelTracking(sourceTracking, nodeLocations)
    val parsedReferences = ParsedReferences()
    val referenceLinker = ReferenceLinker(modelTracking)
    val sourceBudget =
        SourceBudget(sourceLimits) { diagnostic ->
            throw AsyncApiParseException.ParserDiagnosticFailure(diagnostic, this)
        }

    /** Records that [model] was parsed from [node]. */
    fun register(
        model: Any,
        node: ParserNode,
    ) {
        modelTracking.register(model, node)
    }

    /** Records that [reference] was parsed from [node] and that [parseTarget] parses the node it points at. */
    fun registerReference(
        reference: Reference,
        node: ParserNode,
        parseTarget: (ParserNode) -> Any,
    ) {
        modelTracking.register(reference, node)
        parsedReferences.add(reference, parseTarget)
    }

    /** Registers a source document of this load and returns its id. */
    fun registerDocumentSource(
        file: File,
        content: String,
        location: SourceLocation,
    ): SourceId {
        val sourceId = sourceTracking.registerSource(file, content)
        sourceBudget.registerDocument(file, content, location)
        return sourceId
    }

    /** A snippet of the source around the node whose display path is [displayPath]. */
    fun snippet(displayPath: String): String =
        nodeLocations.location(displayPath)?.let(sourceTracking::snippet) ?: "(no line found for $displayPath)"
}
