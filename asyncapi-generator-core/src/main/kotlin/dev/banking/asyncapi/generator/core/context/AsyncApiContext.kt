package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.repository.ModelRepository
import dev.banking.asyncapi.generator.core.repository.SourceRepository
import dev.banking.asyncapi.generator.core.source.SourceBudget
import dev.banking.asyncapi.generator.core.source.SourceLimits
import dev.banking.asyncapi.generator.core.source.SourceLocation
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationReporter
import java.io.File

/**
 * Mutable context shared across the parser, external loader, and validator stages.
 *
 * A new context is created for each call to [dev.banking.asyncapi.generator.core.loader.AsyncApiDocumentLoader.load].
 *
 * @param sourceLimits limits for reading the documents and native schema assets of this load
 */
internal class AsyncApiContext(
    val sourceLimits: SourceLimits = SourceLimits(),
) {
    val sourceTracking = SourceTracking()
    val modelTracking = ModelTracking(sourceTracking)
    val sourceBudget =
        SourceBudget(sourceLimits) { diagnostic ->
            throw AsyncApiParseException.ParserDiagnosticFailure(diagnostic, this)
        }
    val bindingRegistry = BindingReferenceRegistry()
    val warningCollector = ValidationWarningCollector()

    val externalLoader =
        AsyncApiExternalContext(
            sourceTracking = sourceTracking,
            modelTracking = modelTracking,
            warningCollector = warningCollector,
            sourceBudget = sourceBudget,
            createParser = { AsyncApiParser(this) },
            createValidator = { AsyncApiValidator(this) },
            createReporter = { ValidationReporter(this) },
            createFragmentProcessor = { ExternalFragmentProcessor(this) },
            createDiagnosticException = { diagnostic ->
                AsyncApiParseException.ParserDiagnosticFailure(diagnostic, this)
            },
            readDocument = { file -> AsyncApiRegistry.read(file, this) },
        )

    val sourceRepository: SourceRepository get() = sourceTracking.repository
    val modelRepository: ModelRepository get() = modelTracking.repository

    fun register(
        model: Any,
        node: ParserNode,
    ) {
        modelTracking.register(model, node)

        if (model is Reference) {
            externalLoader.loadExternal(model)
        }
    }

    internal fun registerDocumentSource(
        file: File,
        content: String,
        location: SourceLocation,
    ): String {
        val sourceId = sourceTracking.registerSourceAndGetPathId(file, content)
        sourceBudget.registerDocument(file, content, location)
        return sourceId
    }
}
