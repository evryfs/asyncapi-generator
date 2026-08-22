package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.parser.node.NodeAddress
import dev.banking.asyncapi.generator.core.repository.SourceRepository
import java.io.File

/**
 * Tracks source files, line registrations, source locations, and code snippets.
 */
internal class SourceTracking {

    val repository = SourceRepository()

    fun registerSource(
        file: File,
        content: String,
    ) {
        repository.registerSource(file, content)
    }

    fun registerSourceAndGetPathId(
        file: File,
        content: String,
    ): String = repository.registerSourceAndGetPathId(file, content)

    fun registerLine(
        path: String,
        line: Int,
    ) {
        repository.registerLine(path, line)
    }

    fun registerLocation(
        path: String,
        location: SourceLocation,
    ) {
        repository.registerLocation(path, location)
    }

    fun registerLocation(
        address: NodeAddress,
        location: SourceLocation,
    ) {
        repository.registerLocation(address, location)
    }

    fun pathSnippet(
        path: String,
        contextLines: Int = 3,
    ): String = repository.pathSnippet(path, contextLines)

    fun sourceSnippet(
        sourceLocation: SourceLocation,
        contextLines: Int = 3,
    ): String = repository.locationSnippet(sourceLocation, contextLines)

    fun getCurrentFile(): File = repository.getCurrentFile()

    fun findFileById(id: String): File? = repository.findFileById(id)
}
