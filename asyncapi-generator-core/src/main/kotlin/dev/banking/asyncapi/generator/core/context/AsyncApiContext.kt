package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.repository.ModelRepository
import dev.banking.asyncapi.generator.core.repository.SourceRepository
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationReporter
import java.io.File

/**
 * Mutable context shared across the parser, external loader, and validator stages.
 *
 * A new context is created for each call to [dev.banking.asyncapi.generator.core.loader.AsyncApiDocumentLoader.load].
 *
 * @param loadResourceLimits configurable thresholds for document sizes, external references, and schema reads
 */
internal class AsyncApiContext internal constructor(
    loadResourceLimits: ParserLoadResourceLimits,
) {
    constructor() : this(ParserLoadResourceLimits())

    val sourceTracking = SourceTracking()
    val modelTracking = ModelTracking(sourceTracking)
    val resourceBudget = ResourceBudget(loadResourceLimits)
    val bindingRegistry = BindingReferenceRegistry()
    val warningCollector = ValidationWarningCollector()

    val externalLoader = AsyncApiExternalContext(
        sourceTracking = sourceTracking,
        modelTracking = modelTracking,
        warningCollector = warningCollector,
        registerExternalDocument = { file, location -> registerExternalDocument(file, location) },
        registerReferenceTarget = { file, pointer, location -> registerReferenceTarget(file, pointer, location) },
        withinExternalReference = { location, block -> withinExternalReference(location, block) },
        createParser = { AsyncApiParser(this) },
        createValidator = { AsyncApiValidator(this) },
        createReporter = { ValidationReporter(this) },
        createFragmentProcessor = { ExternalFragmentProcessor(this) },
        createDiagnosticException = { diagnostic -> AsyncApiParseException.ParserDiagnosticFailure(diagnostic, this) },
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
        val result = resourceBudget.registerDocument(file, content, location)
        enforceBudgetResult(result, location)
        return sourceId
    }

    internal fun registerExternalDocument(
        file: File,
        location: SourceLocation,
    ) {
        val result = resourceBudget.registerExternalDocument(file, location)
        enforceBudgetResult(result, location)
    }

    internal fun registerReferenceTarget(
        file: File,
        pointer: String,
        location: SourceLocation,
    ) {
        val result = resourceBudget.registerReferenceTarget(file, pointer, location)
        enforceBudgetResult(result, location)
    }

    internal fun <T> withinExternalReference(
        location: SourceLocation,
        block: () -> T,
    ): T = when (val result = resourceBudget.withinExternalReference(location, block)) {
        is ResourceBudgetResult.Success -> result.value
        is ResourceBudgetResult.LimitExceeded -> throwBudgetLimit(result)
    }

    @Throws(java.io.IOException::class)
    internal fun readNativeSchemaAsset(
        file: File,
        location: SourceLocation,
        path: String,
    ): String = when (val result = resourceBudget.readNativeSchemaAsset(file, location, path)) {
        is ResourceBudgetResult.Success -> result.value
        is ResourceBudgetResult.LimitExceeded -> throwBudgetLimit(result)
    }

    private fun enforceBudgetResult(
        result: ResourceBudgetResult<Unit>,
        location: SourceLocation,
    ) {
        if (result is ResourceBudgetResult.LimitExceeded) throwBudgetLimit(result)
    }

    private fun throwBudgetLimit(result: ResourceBudgetResult.LimitExceeded): Nothing {
        throw AsyncApiParseException.ParserDiagnosticFailure(
            diagnostic = ParserDiagnostic.LoadResourceLimitExceeded(
                limit = result.limit,
                maximum = result.maximum,
                observed = result.observed,
                path = result.path,
                sourceLocation = result.sourceLocation,
            ),
            context = this,
        )
    }
}
