package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.NodeAddress
import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.repository.ModelRepository
import dev.banking.asyncapi.generator.core.repository.SourceRepository
import java.io.File
import java.util.IdentityHashMap
import kotlin.reflect.KProperty0

class AsyncApiContext {
    internal data class BindingReferenceOrigin(
        val location: BindingLocation,
        val protocol: String?,
    )

    val sourceRepository = SourceRepository()
    val modelRepository = ModelRepository(sourceRepository)

    val externalLoader = AsyncApiExternalContext(this)
    private val bindingReferenceOrigins = IdentityHashMap<Reference, BindingReferenceOrigin>()

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
    ): String = sourceRepository.registerSourceAndGetPathId(file, content)

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
}
