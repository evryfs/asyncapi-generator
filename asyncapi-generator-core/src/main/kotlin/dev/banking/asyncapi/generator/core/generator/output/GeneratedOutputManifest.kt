package dev.banking.asyncapi.generator.core.generator.output

import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap

/**
 * Records which execution generated which files, so files an execution no longer generates are removed.
 *
 * One manifest serves every execution of a build, because executions may share an output
 * directory and even generate the same file. A file is removed only when the execution that
 * generated it stops generating it and no other execution still claims it. Files that no
 * execution ever generated are never touched. Paths are stored relative to the manifest, so
 * a moved project never removes files from its old location.
 *
 * @property file Manifest file; created on first use.
 * @property owner Stable name of the execution, such as the Maven execution id.
 */
data class GeneratedOutputManifest(
    val file: File,
    val owner: String,
) {
    init {
        require(owner.isNotBlank() && owner.none { it == '\t' || it == '\n' || it == '\r' }) {
            "Manifest owner must be a non-blank single-line name without tabs: '$owner'"
        }
    }

    /**
     * Claims [generatedFiles] for [owner] and deletes files [owner] generated before but no longer
     * generates, unless another owner still claims them.
     */
    fun update(generatedFiles: Collection<File>) {
        val manifestPath = file.toPath().toAbsolutePath().normalize()
        val base = manifestPath.parent
        Files.createDirectories(base)
        // Threads of one JVM (parallel Gradle tasks) wait on the monitor; separate processes on the file lock.
        synchronized(monitors.computeIfAbsent(manifestPath) { Any() }) {
            updateLocked(manifestPath, base, generatedFiles)
        }
    }

    private fun updateLocked(manifestPath: Path, base: Path, generatedFiles: Collection<File>) {
        FileChannel.open(
            base.resolve("${manifestPath.fileName}.lock"),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
        ).use { lockChannel ->
            lockChannel.lock().use {
                val entries = read(manifestPath)
                val current = generatedFiles.map { base.relativize(it.toPath().toAbsolutePath().normalize()).toString() }.toSet()
                val previous = entries.filter { it.owner == owner }.map(Entry::path).toSet()
                val claimedByOthers = entries.filter { it.owner != owner }.map(Entry::path).toSet()

                (previous - current - claimedByOthers).forEach { stale -> delete(base, stale) }

                val updated = entries.filter { it.owner != owner } + current.map { Entry(owner, it) }
                write(manifestPath, updated)
            }
        }
    }

    private fun read(manifestPath: Path): List<Entry> {
        if (!Files.isRegularFile(manifestPath)) return emptyList()
        return Files.readAllLines(manifestPath)
            .filter { line -> line.isNotBlank() && !line.startsWith("#") }
            .mapNotNull { line ->
                val separator = line.indexOf('\t')
                if (separator <= 0) null else Entry(line.substring(0, separator), line.substring(separator + 1))
            }
    }

    private fun write(manifestPath: Path, entries: List<Entry>) {
        val content =
            buildString {
                appendLine(HEADER)
                entries.sortedWith(compareBy(Entry::owner, Entry::path)).forEach { appendLine("${it.owner}\t${it.path}") }
            }
        val temporary = Files.createTempFile(manifestPath.parent, ".asyncapi-generator-", ".tmp")
        try {
            Files.writeString(temporary, content)
            Files.move(temporary, manifestPath, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    /** Deletes one stale generated file, then the directories it leaves empty. */
    private fun delete(base: Path, stale: String) {
        val target = base.resolve(stale).normalize()
        if (!Files.isRegularFile(target)) return
        try {
            Files.delete(target)
            var directory = target.parent
            while (directory != null && directory != base && isEmptyDirectory(directory)) {
                Files.delete(directory)
                directory = directory.parent
            }
        } catch (exception: IOException) {
            throw IOException("Failed to remove stale generated file '$target'", exception)
        }
    }

    private fun isEmptyDirectory(directory: Path): Boolean =
        Files.isDirectory(directory) && Files.list(directory).use { it.findAny().isEmpty }

    private data class Entry(val owner: String, val path: String)

    private companion object {
        val monitors = ConcurrentHashMap<Path, Any>()
        const val HEADER = "# Files generated by asyncapi-generator, by execution. Used to remove stale generated files."
    }
}
