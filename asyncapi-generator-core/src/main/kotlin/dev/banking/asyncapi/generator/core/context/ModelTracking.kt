package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.repository.ModelRepository
import kotlin.reflect.KProperty0

/**
 * Tracks model registrations, source locations, field metadata, and reference lookups.
 *
 * @param sourceTracking shared source tracking for cross-referencing models with source locations
 */
internal class ModelTracking(sourceTracking: SourceTracking) {

    val repository = ModelRepository(sourceTracking.repository)

    fun register(
        model: Any,
        node: ParserNode,
    ) {
        repository.register(model, node)
    }

    fun <R> getLine(
        model: Any,
        property: KProperty0<R>,
    ): Int? = repository.getLine(model, property) ?: repository.getLine(model)

    fun <R> getSourceLocation(
        model: Any,
        property: KProperty0<R>,
    ): SourceLocation? =
        repository.getSourceLocation(model, property)
            ?: repository.getSourceLocation(model)

    fun getSourceLocation(
        model: Any,
        fieldName: String,
    ): SourceLocation? =
        repository.getSourceLocation(model, fieldName)
            ?: repository.getSourceLocation(model)

    fun getSourceLocation(model: Any): SourceLocation? = repository.getSourceLocation(model)

    fun getFieldNames(model: Any): Set<String> = repository.getFieldNames(model)

    fun getFieldValue(model: Any, fieldName: String): Any? =
        repository.getFieldValue(model, fieldName)

    fun findReference(reference: Reference): Any? = repository.findByReference(reference)

    fun getReferenceOrigin(reference: Reference): ModelRepository.ReferenceOrigin? =
        repository.getReferenceOrigin(reference)
}
