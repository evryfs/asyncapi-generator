package com.tietoevry.banking.asyncapi.generator.core.repository

import com.tietoevry.banking.asyncapi.generator.core.constants.AsyncApiConstants.ROOT
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import kotlin.reflect.KProperty0

class ModelRepository(
    private val sourceRepository: SourceRepository,
) {

    data class Model(
        val model: Any,
        val fieldLines: Map<String, Int>,
        val fieldName: String?,
        val parentPath: String?,
        val nodePath: String?,
    )

    private val modelsByInstance = LinkedHashMap<Any, Model>()
    private val modelsByPath = LinkedHashMap<String, Any>()

    fun register(model: Any, node: ParserNode) {
        val fieldLines = collectFieldLines(node)
        val fieldName = node.path.substringAfterLast('.', node.path)
        val parentPath = node.path.substringBeforeLast('.', missingDelimiterValue = "")
        val path = node.path

        if (model is Reference) {
            model.sourceId = path.substringBefore(".root", path)
        }

        modelsByInstance[model] = Model(model, fieldLines, fieldName, parentPath, path)
        modelsByPath[path] = model
    }

    fun <R> getLine(model: Any, property: KProperty0<R>): Int? {
        val fieldName = property.name
        return modelsByInstance[model]?.fieldLines?.get(fieldName)
    }

    fun getModelsByInstance() = modelsByInstance.toMap()
    fun getModelsByPath() = modelsByPath.toMap()

    fun findByReference(reference: Reference): Any? {
        val normalized = normalize(reference) ?: return null
        return modelsByPath[normalized]
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
