package dev.banking.asyncapi.generator.core.repository

import dev.banking.asyncapi.generator.core.constants.AsyncApiConstants.ROOT
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.reader.SourceLocation
import java.util.IdentityHashMap
import kotlin.reflect.KProperty0

/**
 * Tracks parsed model instances back to parser paths and source locations.
 *
 * Expected behavior is covered by:
 * - `AsyncApiParserTest`
 */
class ModelRepository(
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

    private val modelsByInstance = IdentityHashMap<Any, Model>()
    private val modelsByPath = LinkedHashMap<String, Any>()

    fun register(model: Any, node: ParserNode) {
        val fieldNames = collectFieldNames(node)
        val fieldValues = collectFieldValues(node)
        val fieldLocations = collectFieldLocations(node)
        val fieldLines = if (fieldLocations.isNotEmpty()) {
            fieldLocations.mapValues { (_, location) -> location.line }
        } else {
            collectFieldLines(node)
        }
        val fieldName = node.path.substringAfterLast('.', node.path)
        val parentPath = node.path.substringBeforeLast('.', missingDelimiterValue = "")
        val path = node.path
        val sourceLocation = sourceRepository.getLocation(path)
            ?: sourceRepository.findNearestLocation(path)

        if (model is Reference) {
            model.sourceId = path.substringBefore(".root", path)
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
        modelsByPath[path] = model
    }

    fun <R> getLine(model: Any, property: KProperty0<R>): Int? {
        val fieldName = property.name
        val entry = modelsByInstance[model] ?: return null
        return entry.fieldLocations[fieldName]?.line ?: entry.fieldLines[fieldName]
    }

    fun getLine(model: Any): Int? {
        val entry = modelsByInstance[model] ?: return null
        return getSourceLocation(model)?.line
            ?: entry.nodePath?.let { sourceRepository.getLine(it) }
    }

    fun <R> getSourceLocation(model: Any, property: KProperty0<R>): SourceLocation? {
        val fieldName = property.name
        return modelsByInstance[model]?.fieldLocations?.get(fieldName)
    }

    fun getSourceLocation(model: Any, fieldName: String): SourceLocation? {
        val entry = modelsByInstance[model] ?: return null
        return entry.fieldLocations[fieldName]
            ?: entry.nodePath?.let { path -> sourceRepository.getLocation("$path.$fieldName") }
    }

    fun getSourceLocation(model: Any): SourceLocation? {
        val entry = modelsByInstance[model] ?: return null
        return entry.sourceLocation ?: entry.nodePath?.let { sourceRepository.findNearestLocation(it) }
    }

    fun getFieldNames(model: Any): Set<String> =
        modelsByInstance[model]?.fieldNames.orEmpty()

    fun getFieldValue(model: Any, fieldName: String): Any? =
        modelsByInstance[model]?.fieldValues?.get(fieldName)

    fun getModelsByInstance(): Map<Any, Model> = IdentityHashMap(modelsByInstance)
    fun getModelsByPath() = modelsByPath.toMap()

    fun findByReference(reference: Reference): Any? {
        val normalized = normalize(reference) ?: return null
        return modelsByPath[normalized]
    }

    private fun collectFieldLocations(node: ParserNode): Map<String, SourceLocation> {
        val result = mutableMapOf<String, SourceLocation>()
        val basePath = node.path
        val normalizedPath = basePath.replace("[", ".").replace("]", "")
        when (val raw = node.node) {
            is Map<*, *> -> {
                for (key in raw.keys.filterIsInstance<String>()) {
                    val possiblePaths = sequenceOf("$basePath.$key", "$normalizedPath.$key")
                    val location = possiblePaths.mapNotNull(sourceRepository::getLocation).firstOrNull()
                    if (location != null) result[key] = location
                }
            }

            is List<*> -> {
                raw.forEachIndexed { index, _ ->
                    val possiblePaths = sequenceOf("$basePath[$index]", "$normalizedPath.$index")
                    val location = possiblePaths.mapNotNull(sourceRepository::getLocation).firstOrNull()
                    if (location != null) result["[$index]"] = location
                }
            }

            else -> {
                (sourceRepository.getLocation(basePath)
                    ?: sourceRepository.getLocation(normalizedPath))
                    ?.let { location -> result["<value>"] = location }
            }
        }
        return result
    }

    private fun collectFieldNames(node: ParserNode): Set<String> =
        when (val raw = node.node) {
            is Map<*, *> -> raw.keys.filterIsInstance<String>().toCollection(linkedSetOf())
            is List<*> -> raw.indices.mapTo(linkedSetOf()) { index -> "[$index]" }
            else -> setOf("<value>")
        }

    private fun collectFieldValues(node: ParserNode): Map<String, Any?> =
        when (val raw = node.node) {
            is Map<*, *> ->
                raw.entries
                    .filter { (key, _) -> key is String }
                    .associate { (key, value) -> key as String to value }

            is List<*> -> raw.mapIndexed { index, value -> "[$index]" to value }.toMap()
            else -> mapOf("<value>" to raw)
        }

    private fun collectFieldLines(node: ParserNode): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val basePath = node.path
        val normalizedPath = basePath.replace("[", ".").replace("]", "")
        when (val raw = node.node) {
            is Map<*, *> -> {
                for (key in raw.keys.filterIsInstance<String>()) {
                    val possiblePaths = sequenceOf("$basePath.$key", "$normalizedPath.$key")
                    val line = possiblePaths.mapNotNull(sourceRepository::getLine).firstOrNull()
                    if (line != null) result[key] = line
                }
            }

            is List<*> -> {
                raw.forEachIndexed { index, _ ->
                    val possiblePaths = sequenceOf("$basePath.$index", "$normalizedPath.$index")
                    val line = possiblePaths.mapNotNull(sourceRepository::getLine).firstOrNull()
                    if (line != null) result["[$index]"] = line
                }
            }

            else -> {
                (sourceRepository.getLine(basePath)
                    ?: sourceRepository.getLine(normalizedPath))
                    ?.let { line -> result["<value>"] = line }
            }
        }
        return result
    }

    private fun normalize(reference: Reference): String? {
        val rawRef = reference.ref
        val clean = rawRef.trim().trimStart('\'', '"', '|', '>')
        if (clean.isEmpty()) return null
        if (clean.startsWith("#/")) {
            val fileId = reference.sourceId
                ?: throw IllegalArgumentException("Reference requires a ID")
            val suffix = clean
                .removePrefix("#/")
                .replace("/", ".")
            return "$fileId.$ROOT.$suffix"
        }

        val docPart = clean.substringBefore('#').trim()
        val pointer = clean.substringAfter('#', missingDelimiterValue = "").ifEmpty { return null }
        val fileId = sourceRepository.fileIdForName(docPart) ?: return null
        val suffix = pointer
            .removePrefix("/")
            .replace("/", ".")
        return "$fileId.$ROOT.$suffix"
    }
}
