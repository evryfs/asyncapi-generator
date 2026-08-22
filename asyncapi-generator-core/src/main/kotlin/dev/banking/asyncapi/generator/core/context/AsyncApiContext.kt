package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation
import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding
import dev.banking.asyncapi.generator.core.parser.node.NodeAddress
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.repository.ModelRepository
import dev.banking.asyncapi.generator.core.repository.SourceRepository
import java.io.File
import kotlin.reflect.KProperty0

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

    private val sourceTracking = SourceTracking()
    private val modelTracking = ModelTracking(sourceTracking.repository)
    private val resourceBudget = ResourceBudget(loadResourceLimits, this)
    private val bindingRegistry = BindingReferenceRegistry()
    private val warningCollector = ValidationWarningCollector()

    val externalLoader = AsyncApiExternalContext(this)

    val sourceRepository: SourceRepository get() = sourceTracking.repository
    val modelRepository: ModelRepository get() = modelTracking.repository

    internal fun registerBindingReferenceOrigin(
        reference: Reference,
        location: BindingLocation,
        protocol: String?,
    ) = bindingRegistry.register(reference, location, protocol)

    internal fun getBindingReferenceOrigin(
        reference: Reference
    ): BindingReferenceRegistry.BindingReferenceOrigin? =
        bindingRegistry.getOrigin(reference)

    fun register(
        model: Any,
        node: ParserNode,
    ) {
        modelTracking.register(model, node)

        if (model is Reference) {
            externalLoader.loadExternal(model)
        }
    }

    fun registerSource(
        file: File,
        content: String,
    ) {
        sourceTracking.registerSource(file, content)
    }

    fun registerLine(
        path: String,
        line: Int,
    ) {
        sourceTracking.registerLine(path, line)
    }

    fun registerSourceLocation(
        path: String,
        location: SourceLocation,
    ) {
        sourceTracking.registerLocation(path, location)
    }

    internal fun registerSourceLocation(
        address: NodeAddress,
        location: SourceLocation,
    ) {
        sourceTracking.registerLocation(address, location)
    }

    fun pathSnippet(
        path: String,
        contextLines: Int = 3,
    ): String = sourceTracking.pathSnippet(path, contextLines)

    fun sourceSnippet(
        sourceLocation: SourceLocation,
        contextLines: Int = 3,
    ): String = sourceTracking.sourceSnippet(sourceLocation, contextLines)

    fun getCurrentFile(): File = sourceTracking.getCurrentFile()

    fun findFileById(id: String): File? = sourceTracking.findFileById(id)

    fun <R> getLine(
        model: Any,
        property: KProperty0<R>,
    ): Int? = modelTracking.getLine(model, property)

    fun <R> getSourceLocation(
        model: Any,
        property: KProperty0<R>,
    ): SourceLocation? = modelTracking.getSourceLocation(model, property)

    fun getSourceLocation(
        model: Any,
        fieldName: String,
    ): SourceLocation? = modelTracking.getSourceLocation(model, fieldName)

    fun getSourceLocation(model: Any): SourceLocation? = modelTracking.getSourceLocation(model)

    fun getFieldNames(model: Any): Set<String> = modelTracking.getFieldNames(model)

    fun getFieldValue(model: Any, fieldName: String): Any? =
        modelTracking.getFieldValue(model, fieldName)

    fun findReference(reference: Reference): Any? = modelTracking.findReference(reference)

    internal fun registerDocumentSource(
        file: File,
        content: String,
        location: SourceLocation,
    ): String {
        val sourceId = sourceTracking.registerSourceAndGetPathId(file, content)
        resourceBudget.registerDocument(file, content, location)
        return sourceId
    }

    internal fun registerExternalDocument(
        file: File,
        location: SourceLocation,
    ) {
        resourceBudget.registerExternalDocument(file, location)
    }

    internal fun registerReferenceTarget(
        file: File,
        pointer: String,
        location: SourceLocation,
    ) {
        resourceBudget.registerReferenceTarget(file, pointer, location)
    }

    internal fun <T> withinExternalReference(
        location: SourceLocation,
        block: () -> T,
    ): T = resourceBudget.withinExternalReference(location, block)

    @Throws(java.io.IOException::class)
    internal fun readNativeSchemaAsset(
        file: File,
        location: SourceLocation,
        path: String,
    ): String = resourceBudget.readNativeSchemaAsset(file, location, path)

    internal fun sourceFiles(): Set<File> = resourceBudget.sourceFiles()

    internal fun collectExternalValidationWarnings(warnings: List<ValidationFinding>) {
        warningCollector.collect(warnings)
    }

    internal fun allValidationWarnings(rootWarnings: List<ValidationFinding>): List<ValidationFinding> =
        warningCollector.mergeWith(rootWarnings)
}
