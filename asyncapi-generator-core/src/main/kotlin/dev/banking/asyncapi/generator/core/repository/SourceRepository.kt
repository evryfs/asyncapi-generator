package dev.banking.asyncapi.generator.core.repository

import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.parser.node.NodeAddress
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * Stores input sources and source locations using parser-stage paths.
 *
 * Expected behavior is covered by:
 * - `ParserNodeFactoryTest`
 * - `AsyncApiRegistryTest`
 */
class SourceRepository {
    data class Source(
        val file: File,
        val id: String,
        val pathId: String,
        val lines: List<String>,
    )

    // Map of file absolute path → Source
    private val sources = mutableMapOf<String, Source>()
    private val sourcesByPathId = mutableMapOf<String, Source>()

    // Map of node path → line number
    internal val lineMap = mutableMapOf<String, Int>()

    // Parser identity is segment-aware. String paths are presentation indexes only.
    private val locationsByAddress = mutableMapOf<NodeAddress, SourceLocation>()
    private val addressesByDisplayPath = mutableMapOf<String, NodeAddress>()
    private val standaloneLocationsByPath = mutableMapOf<String, SourceLocation>()

    private lateinit var current: Source

    fun registerSource(
        file: File,
        content: String,
    ) {
        register(file, content)
    }

    internal fun registerSourceAndGetPathId(
        file: File,
        content: String,
    ): String = register(file, content)

    private fun register(
        file: File,
        content: String,
    ): String {
        val canonicalFile = file.canonicalFile
        sources[canonicalFile.absolutePath]?.let { existing ->
            current = existing
            return existing.pathId
        }

        val preferredId = canonicalFile.nameWithoutExtension.replace(Regex("[^A-Za-z0-9_]"), "_")
        val pathId = uniquePathId(preferredId, canonicalFile)
        val src = Source(
            file = canonicalFile,
            id = preferredId,
            pathId = pathId,
            lines = content.lines(),
        )
        sources[canonicalFile.absolutePath] = src
        sourcesByPathId[pathId] = src
        current = src
        return pathId
    }

    fun registerLine(
        path: String,
        line: Int,
    ) {
        lineMap[path] = line
    }

    fun registerLocation(
        path: String,
        location: SourceLocation,
    ) {
        standaloneLocationsByPath[path] = location.copy(path = path)
        registerLine(path, location.line)
    }

    internal fun registerLocation(
        address: NodeAddress,
        location: SourceLocation,
    ) {
        val displayPath = address.displayPath
        locationsByAddress[address] = location.copy(path = displayPath)
        addressesByDisplayPath[displayPath] = address
        registerLine(displayPath, location.line)
    }

    fun getLine(path: String): Int? = lineMap[path]

    fun getLocation(path: String): SourceLocation? =
        addressesByDisplayPath[path]?.let(locationsByAddress::get)
            ?: standaloneLocationsByPath[path]

    internal fun getLocation(address: NodeAddress): SourceLocation? =
        locationsByAddress[address]

    fun getCurrentFile(): File = current.file

    fun findFileById(id: String): File? = sourcesByPathId[id]?.file

    fun findIdByFile(file: File): String? = sources[file.canonicalFile.absolutePath]?.pathId

    fun findStableIdByPathId(pathId: String): String? = sourcesByPathId[pathId]?.id

    fun getAllSources(): Collection<Source> = sources.values

    fun getAllLines(): Map<String, Int> = lineMap.toMap()

    fun getAllLocations(): Map<String, SourceLocation> =
        buildMap {
            locationsByAddress.forEach { (address, location) -> put(address.displayPath, location) }
            putAll(standaloneLocationsByPath)
        }

    fun findNearestLine(path: String): Int? {
        findNearestLocation(path)?.let { return it.line }
        return lineMap[path]
    }

    fun findNearestLocation(path: String): SourceLocation? =
        getLocation(path)

    internal fun findNearestLocation(address: NodeAddress): SourceLocation? =
        address.ancestors().mapNotNull(locationsByAddress::get).firstOrNull()

    internal fun resolveAddress(
        sourceId: String,
        pointerSegments: List<String>,
    ): NodeAddress? {
        var address = NodeAddress.root(sourceId)
        if (address !in locationsByAddress) return null

        pointerSegments.forEach { pointerSegment ->
            val memberAddress = address.member(pointerSegment)
            val indexAddress = pointerSegment
                .takeIf(JSON_ARRAY_INDEX::matches)
                ?.toIntOrNull()
                ?.let(address::index)
            address = when {
                memberAddress in locationsByAddress -> memberAddress
                indexAddress != null && indexAddress in locationsByAddress -> indexAddress
                else -> return null
            }
        }
        return address
    }

    fun pathSnippet(
        path: String,
        contextLines: Int = 3,
    ): String {
        val location = findNearestLocation(path)
        val source = location?.let(::sourceFor) ?: current
        val lines = source.lines
        val line = location?.line ?: findNearestLine(path) ?: return "(no line found for $path)"
        return buildSnippet(lines, source.file.name, line, contextLines, path)
    }

    fun locationSnippet(
        location: SourceLocation,
        contextLines: Int = 3,
    ): String {
        val source = sourceFor(location)
        val lines = source.lines
        return buildSnippet(lines, source.file.name, location.line, contextLines, location.path)
    }

    private fun buildSnippet(
        lines: List<String>,
        fileName: String,
        center: Int,
        context: Int,
        label: String,
    ): String {
        val start = max(0, center - context - 1)
        val end = min(lines.size, center + context)
        return buildString {
            appendLine("$fileName ($label)")
            for (i in start until end) {
                val mark = if (i + 1 == center) "→" else " "
                appendLine("$mark ${(i + 1).toString().padStart(4)} | ${lines[i]}")
            }
        }.trimEnd()
    }

    private fun sourceFor(location: SourceLocation): Source =
        sources[location.file.canonicalFile.absolutePath]
            ?: sourcesByPathId[location.sourceId]
            ?: current

    private fun uniquePathId(preferredId: String, file: File): String {
        val existing = sourcesByPathId[preferredId]
        if (existing == null || existing.file == file) return preferredId

        val suffix = UUID.nameUUIDFromBytes(
            file.absolutePath.toByteArray(StandardCharsets.UTF_8),
        ).toString().substringBefore('-')
        val base = "${preferredId}_$suffix"
        var candidate = base
        var discriminator = 2
        while (
            sourcesByPathId[candidate]?.file != null &&
            sourcesByPathId[candidate]?.file != file
        ) {
            candidate = "${base}_${discriminator++}"
        }
        return candidate
    }

    private companion object {
        val JSON_ARRAY_INDEX = Regex("""0|[1-9]\d*""")
    }
}
