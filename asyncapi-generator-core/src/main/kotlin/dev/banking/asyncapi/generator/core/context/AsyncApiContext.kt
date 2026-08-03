package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation
import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.validator.ValidationConcern
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity
import dev.banking.asyncapi.generator.core.parser.node.NodeAddress
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.repository.ModelRepository
import dev.banking.asyncapi.generator.core.repository.SourceRepository
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.util.IdentityHashMap
import kotlin.reflect.KProperty0

internal class AsyncApiContext internal constructor(
    loadResourceLimits: ParserLoadResourceLimits,
) {
    constructor() : this(ParserLoadResourceLimits())

    internal data class BindingReferenceOrigin(
        val location: BindingLocation,
        val protocol: String?,
    )

    val sourceRepository = SourceRepository()
    val modelRepository = ModelRepository(sourceRepository)

    val externalLoader = AsyncApiExternalContext(this)
    private val loadResourceBudget = ParserLoadResourceBudget(loadResourceLimits)
    private val bindingReferenceOrigins = IdentityHashMap<Reference, BindingReferenceOrigin>()
    private val externalValidationWarnings = linkedMapOf<ValidationFindingIdentity, ValidationFinding>()

    internal fun registerBindingReferenceOrigin(
        reference: Reference,
        location: BindingLocation,
        protocol: String?,
    ) {
        bindingReferenceOrigins[reference] = BindingReferenceOrigin(location, protocol)
    }

    internal fun getBindingReferenceOrigin(reference: Reference): BindingReferenceOrigin? =
        bindingReferenceOrigins[reference]

    fun register(
        model: Any,
        node: ParserNode,
    ) {
        modelRepository.register(model, node)

        if (model is Reference) {
            externalLoader.loadExternal(model)
        }
    }

    fun registerSource(
        file: File,
        content: String,
    ) {
        sourceRepository.registerSource(file, content)
    }

    internal fun registerDocumentSource(
        file: File,
        content: String,
        location: SourceLocation,
    ): String {
        val sourceId = sourceRepository.registerSourceAndGetPathId(file, content)
        enforceLoadResourceLimits(location) {
            loadResourceBudget.registerDocument(
                file = file,
                sourceBytes = content.toByteArray(UTF_8).size.toLong(),
            )
        }
        return sourceId
    }

    internal fun registerExternalDocument(
        file: File,
        location: SourceLocation,
    ) {
        enforceLoadResourceLimits(location) {
            loadResourceBudget.registerExternalDocument(file)
        }
    }

    internal fun registerReferenceTarget(
        file: File,
        pointer: String,
        location: SourceLocation,
    ) {
        enforceLoadResourceLimits(location) {
            loadResourceBudget.registerReferenceTarget(file, pointer)
        }
    }

    internal fun <T> withinExternalReference(
        location: SourceLocation,
        block: () -> T,
    ): T = enforceLoadResourceLimits(location) {
        loadResourceBudget.withinExternalReference(block)
    }

    @Throws(IOException::class)
    internal fun readNativeSchemaAsset(
        file: File,
        location: SourceLocation,
        path: String,
    ): String = enforceLoadResourceLimits(location, path) {
        loadResourceBudget.readNativeSchemaAsset(file)
    }

    internal fun collectExternalValidationWarnings(warnings: List<ValidationFinding>) {
        warnings.forEach { warning ->
            externalValidationWarnings.putIfAbsent(ValidationFindingIdentity.of(warning), warning)
        }
    }

    internal fun allValidationWarnings(rootWarnings: List<ValidationFinding>): List<ValidationFinding> =
        buildMap {
            putAll(externalValidationWarnings)
            rootWarnings.forEach { warning -> putIfAbsent(ValidationFindingIdentity.of(warning), warning) }
        }.values.toList()

    internal fun sourceFiles(): Set<File> = loadResourceBudget.sourceFiles()

    fun registerLine(
        path: String,
        line: Int,
    ) {
        sourceRepository.registerLine(path, line)
    }

    fun registerSourceLocation(
        path: String,
        location: SourceLocation,
    ) {
        sourceRepository.registerLocation(path, location)
    }

    internal fun registerSourceLocation(
        address: NodeAddress,
        location: SourceLocation,
    ) {
        sourceRepository.registerLocation(address, location)
    }

    fun <R> getLine(
        model: Any,
        property: KProperty0<R>,
    ): Int? = modelRepository.getLine(model, property) ?: modelRepository.getLine(model)

    fun <R> getSourceLocation(
        model: Any,
        property: KProperty0<R>,
    ): SourceLocation? =
        modelRepository.getSourceLocation(model, property)
            ?: modelRepository.getSourceLocation(model)

    fun getSourceLocation(
        model: Any,
        fieldName: String,
    ): SourceLocation? =
        modelRepository.getSourceLocation(model, fieldName)
            ?: modelRepository.getSourceLocation(model)

    fun getSourceLocation(model: Any): SourceLocation? = modelRepository.getSourceLocation(model)

    fun getFieldNames(model: Any): Set<String> = modelRepository.getFieldNames(model)

    fun getFieldValue(model: Any, fieldName: String): Any? =
        modelRepository.getFieldValue(model, fieldName)

    fun pathSnippet(
        path: String,
        contextLines: Int = 3,
    ): String = sourceRepository.pathSnippet(path, contextLines)

    fun sourceSnippet(
        sourceLocation: SourceLocation,
        contextLines: Int = 3,
    ): String = sourceRepository.locationSnippet(sourceLocation, contextLines)

    fun findReference(reference: Reference): Any? = modelRepository.findByReference(reference)

    fun getCurrentFile(): File = sourceRepository.getCurrentFile()

    fun findFileById(id: String): File? = sourceRepository.findFileById(id)

    private fun <T> enforceLoadResourceLimits(
        location: SourceLocation,
        path: String = location.path,
        block: () -> T,
    ): T =
        try {
            block()
        } catch (exception: ParserLoadResourceLimitExceeded) {
            throw AsyncApiParseException.ParserDiagnosticFailure(
                diagnostic = ParserDiagnostic.LoadResourceLimitExceeded(
                    limit = exception.limit,
                    maximum = exception.maximum,
                    observed = exception.observed,
                    path = path,
                    sourceLocation = location,
                ),
                context = this,
            )
        }

    private data class ValidationFindingIdentity(
        val code: String,
        val concern: ValidationConcern,
        val severity: ValidationSeverity,
        val documentation: String,
        val file: String?,
        val path: String?,
        val line: Int?,
        val column: Int?,
    ) {
        companion object {
            fun of(finding: ValidationFinding): ValidationFindingIdentity =
                ValidationFindingIdentity(
                    code = finding.code,
                    concern = finding.concern,
                    severity = finding.severity,
                    documentation = finding.documentation,
                    file = finding.sourceLocation?.file?.canonicalPath,
                    path = finding.path,
                    line = finding.line,
                    column = finding.sourceLocation?.column,
                )
        }
    }
}
