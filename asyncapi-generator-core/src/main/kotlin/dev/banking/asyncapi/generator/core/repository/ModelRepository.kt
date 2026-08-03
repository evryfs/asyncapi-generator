package dev.banking.asyncapi.generator.core.repository

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.parseReference
import dev.banking.asyncapi.generator.core.parser.node.NodeAddress
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.document.toValue
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiParserProfile
import java.io.File
import java.util.IdentityHashMap
import kotlin.reflect.KProperty0

/**
 * Tracks parsed model instances back to parser paths and source locations.
 *
 * Expected behavior is covered by:
 * - `AsyncApiParserTest`
 */
internal class ModelRepository(
    private val sourceRepository: SourceRepository,
) {

    data class Model(
        val model: Any,
        val sourceLocation: SourceLocation?,
        val fieldNames: Set<String>,
        val fieldValues: Map<String, Any?>,
        val fieldLocations: Map<String, SourceLocation>,
        val fieldLines: Map<String, Int>,
        val fieldName: String?,
        val parentPath: String?,
        val nodePath: String?,
    )

    internal data class ReferenceOrigin(
        val file: File,
        val sourcePathId: String,
        val parserPath: String,
        val sourceLocation: SourceLocation?,
        val parserProfile: AsyncApiParserProfile?,
    )

    private val modelsByInstance = IdentityHashMap<Any, Model>()
    private val addressesByInstance = IdentityHashMap<Any, NodeAddress>()
    private val modelsByAddress = LinkedHashMap<NodeAddress, Any>()
    private val modelsByPath = LinkedHashMap<String, Any>()
    private val referenceOrigins = IdentityHashMap<Reference, ReferenceOrigin>()

    fun register(model: Any, node: ParserNode) {
        val fieldNames = collectFieldNames(node)
        val fieldValues = collectFieldValues(node)
        val fieldLocations = collectFieldLocations(node)
        val fieldLines = if (fieldLocations.isNotEmpty()) {
            fieldLocations.mapValues { (_, location) -> location.line }
        } else {
            collectFieldLines(node)
        }
        val fieldName = node.name
        val parentPath = node.address.parent?.displayPath
        val path = node.path
        val sourceLocation = sourceRepository.getLocation(node.address)
            ?: sourceRepository.findNearestLocation(node.address)

        if (model is Reference) {
            val sourcePathId = node.address.sourceId
            model.sourceId = sourceRepository.findStableIdByPathId(sourcePathId) ?: sourcePathId
            sourceRepository.findFileById(sourcePathId)?.let { sourceFile ->
                referenceOrigins[model] = ReferenceOrigin(
                    file = sourceFile,
                    sourcePathId = sourcePathId,
                    parserPath = path,
                    sourceLocation = sourceLocation,
                    parserProfile = node.profile,
                )
            }
        }

        modelsByInstance[model] =
            Model(
                model,
                sourceLocation,
                fieldNames,
                fieldValues,
                fieldLocations,
                fieldLines,
                fieldName,
                parentPath,
                path,
            )
        addressesByInstance[model] = node.address
        modelsByAddress[node.address] = model
        modelsByPath[path] = model
    }

    fun <R> getLine(model: Any, property: KProperty0<R>): Int? {
        val fieldName = property.name
        val entry = modelsByInstance[model] ?: return null
        return entry.fieldLocations[fieldName]?.line
            ?: entry.fieldLines[fieldName]
            ?: addressesByInstance[model]?.member(fieldName)?.let(sourceRepository::getLocation)?.line
    }

    fun getLine(model: Any): Int? {
        val entry = modelsByInstance[model] ?: return null
        return getSourceLocation(model)?.line
    }

    fun <R> getSourceLocation(model: Any, property: KProperty0<R>): SourceLocation? {
        val fieldName = property.name
        return modelsByInstance[model]?.fieldLocations?.get(fieldName)
    }

    fun getSourceLocation(model: Any, fieldName: String): SourceLocation? {
        val entry = modelsByInstance[model] ?: return null
        return entry.fieldLocations[fieldName]
            ?: addressesByInstance[model]?.member(fieldName)?.let(sourceRepository::getLocation)
    }

    fun getSourceLocation(model: Any): SourceLocation? {
        val entry = modelsByInstance[model] ?: return null
        return entry.sourceLocation
            ?: addressesByInstance[model]?.let(sourceRepository::findNearestLocation)
    }

    fun getFieldNames(model: Any): Set<String> =
        modelsByInstance[model]?.fieldNames.orEmpty()

    fun getFieldValue(model: Any, fieldName: String): Any? =
        modelsByInstance[model]?.fieldValues?.get(fieldName)

    fun getModelsByInstance(): Map<Any, Model> = IdentityHashMap(modelsByInstance)
    fun getModelsByPath() = modelsByPath.toMap()

    internal fun getReferenceOrigin(reference: Reference): ReferenceOrigin? =
        referenceOrigins[reference]

    fun findByReference(reference: Reference): Any? {
        val normalized = normalize(reference) ?: return null
        return modelsByAddress[normalized]
    }

    private fun collectFieldLocations(node: ParserNode): Map<String, SourceLocation> {
        val result = mutableMapOf<String, SourceLocation>()
        when (val raw = node.node) {
            is DocumentObject -> {
                for ((key, member) in raw.members) {
                    val address = node.address.member(key)
                    val location = sourceRepository.getLocation(address)
                        ?: member.keyLocation
                    result[key] = location.copy(path = address.displayPath)
                }
            }

            is DocumentArray -> {
                raw.elements.forEachIndexed { index, element ->
                    val address = node.address.index(index)
                    val location = sourceRepository.getLocation(address)
                        ?: element.location
                    result["[$index]"] = location.copy(path = address.displayPath)
                }
            }

            else -> {
                sourceRepository.getLocation(node.address)
                    ?.let { location -> result["<value>"] = location }
            }
        }
        return result
    }

    private fun collectFieldNames(node: ParserNode): Set<String> =
        when (val raw = node.node) {
            is DocumentObject -> raw.members.keys.toCollection(linkedSetOf())
            is DocumentArray -> raw.elements.indices.mapTo(linkedSetOf()) { index -> "[$index]" }
            else -> setOf("<value>")
        }

    private fun collectFieldValues(node: ParserNode): Map<String, Any?> =
        when (val raw = node.node) {
            is DocumentObject ->
                raw.members.mapValues { (_, member) -> member.value.toValue() }

            is DocumentArray -> raw.elements.mapIndexed { index, value -> "[$index]" to value.toValue() }.toMap()
            else -> mapOf("<value>" to raw.toValue())
        }

    private fun collectFieldLines(node: ParserNode): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        when (val raw = node.node) {
            is DocumentObject -> {
                for (key in raw.members.keys) {
                    val line = sourceRepository.getLocation(node.address.member(key))?.line
                    if (line != null) result[key] = line
                }
            }

            is DocumentArray -> {
                raw.elements.forEachIndexed { index, _ ->
                    val line = sourceRepository.getLocation(node.address.index(index))?.line
                    if (line != null) result["[$index]"] = line
                }
            }

            else -> {
                sourceRepository.getLocation(node.address)?.line
                    ?.let { line -> result["<value>"] = line }
            }
        }
        return result
    }

    private fun normalize(reference: Reference): NodeAddress? {
        val origin = referenceOrigins[reference]
        val sourcePathId = origin?.sourcePathId ?: reference.sourceId ?: return null
        val sourceFile = origin?.file ?: sourceRepository.findFileById(sourcePathId) ?: return null
        val parsed = runCatching { reference.ref.parseReference() }.getOrNull() ?: return null
        val targetFile = parsed.resolveDocumentAgainst(sourceFile)
        val fileId =
            if (targetFile == null) {
                sourcePathId
            } else {
                sourceRepository.findIdByFile(targetFile) ?: return null
            }
        val segments = parsed.pointerSegments()
        return sourceRepository.resolveAddress(fileId, segments)
    }
}
