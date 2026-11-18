package dev.banking.asyncapi.generator.core.source

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserLoadResourceLimit
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserLoadResourceLimit.AGGREGATE_SOURCE_BYTES
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserLoadResourceLimit.EXTERNAL_REFERENCE_DEPTH
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserLoadResourceLimit.NATIVE_SCHEMA_ASSET_BYTES
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserLoadResourceLimit.REFERENCE_TARGETS
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserLoadResourceLimit.SOURCE_DOCUMENTS
import java.io.File
import java.io.IOException

/**
 * Accounts for the documents, reference targets, and native schema assets read during one load.
 *
 * Each operation checks the load limits from [SourceLimits] before it takes effect. A limit that would be
 * exceeded is passed to [reportLimitExceeded], which ends the load. Documents and assets are identified by
 * their canonical path, so reading the same file twice is counted once.
 *
 * @param limits limits for the whole load
 * @param reportLimitExceeded reports the exceeded limit to the user and does not return
 */
internal class SourceBudget(
    private val limits: SourceLimits,
    private val reportLimitExceeded: (ParserDiagnostic.LoadResourceLimitExceeded) -> Nothing,
) {
    private val documentBytes = mutableMapOf<String, Long>()
    private val aggregateSources = mutableMapOf<String, Long>()
    private val referenceTargets = mutableSetOf<ReferenceTarget>()
    private val nativeSchemaAssets = mutableMapOf<String, String>()
    private var aggregateSourceBytes = 0L
    private var externalReferenceDepth = 0

    /** Canonical files of all documents and assets read so far, in the order they were first read. */
    fun sourceFiles(): Set<File> = aggregateSources.keys.mapTo(linkedSetOf(), ::File)

    /** Counts a document whose [content] has been read; [location] is where the document is referenced. */
    fun registerDocument(
        file: File,
        content: String,
        location: SourceLocation,
    ) {
        registerDocument(file.canonicalPath, content.toByteArray(Charsets.UTF_8).size.toLong(), location)
    }

    /** Counts an external document before it is read, using its size on disk. */
    @Throws(IOException::class)
    fun registerExternalDocument(
        file: File,
        location: SourceLocation,
    ) {
        val canonicalFile = file.canonicalFile
        registerDocument(canonicalFile.path, canonicalFile.length(), location)
    }

    /** Counts the target of a reference: the [pointer] into [file]. */
    fun registerReferenceTarget(
        file: File,
        pointer: String,
        location: SourceLocation,
    ) {
        val target = ReferenceTarget(file.canonicalPath, pointer)
        if (target in referenceTargets) return
        val observed = referenceTargets.size + 1L
        val maximum = limits.maxReferenceTargets.toLong()
        if (observed > maximum) limitExceeded(REFERENCE_TARGETS, maximum, observed, location)
        referenceTargets += target
    }

    /** Runs [block] one external reference deeper than the caller. */
    fun <T> withinExternalReference(
        location: SourceLocation,
        block: () -> T,
    ): T {
        val observed = externalReferenceDepth + 1L
        val maximum = limits.maxExternalReferenceDepth.toLong()
        if (observed > maximum) limitExceeded(EXTERNAL_REFERENCE_DEPTH, maximum, observed, location)
        externalReferenceDepth++
        return try {
            block()
        } finally {
            externalReferenceDepth--
        }
    }

    /**
     * Reads a native schema asset as UTF-8, once per load.
     *
     * @param file the asset file
     * @param location location of the reference to the asset
     * @param path path of the reference to the asset, used in the diagnostic when a limit is exceeded
     */
    @Throws(IOException::class)
    fun readNativeSchemaAsset(
        file: File,
        location: SourceLocation,
        path: String,
    ): String {
        val asset = file.canonicalFile
        nativeSchemaAssets[asset.path]?.let { return it }

        val maximum = limits.maxNativeSchemaAssetBytes.toLong()
        if (asset.length() > maximum) limitExceeded(NATIVE_SCHEMA_ASSET_BYTES, maximum, asset.length(), location, path)
        requireAggregateCapacity(asset.path, asset.length(), location, path)
        val bytes =
            asset.readBytesAtMost(limits.maxNativeSchemaAssetBytes)
                ?: limitExceeded(NATIVE_SCHEMA_ASSET_BYTES, maximum, maximum + 1, location, path)
        val content = bytes.decodeUtf8Strictly()

        addToAggregate(asset.path, bytes.size.toLong(), location, path)
        nativeSchemaAssets[asset.path] = content
        return content
    }

    private fun registerDocument(
        canonicalPath: String,
        sourceBytes: Long,
        location: SourceLocation,
    ) {
        val knownBytes = documentBytes[canonicalPath]
        if (knownBytes != null && sourceBytes <= knownBytes) return
        if (knownBytes == null) {
            val observed = documentBytes.size + 1L
            val maximum = limits.maxSourceDocuments.toLong()
            if (observed > maximum) limitExceeded(SOURCE_DOCUMENTS, maximum, observed, location)
        }
        addToAggregate(canonicalPath, sourceBytes, location, location.path)
        documentBytes[canonicalPath] = sourceBytes
    }

    /** Adds [sourceBytes] for [canonicalPath] to the aggregate, counting a file again only when it grew. */
    private fun addToAggregate(
        canonicalPath: String,
        sourceBytes: Long,
        location: SourceLocation,
        path: String,
    ) {
        requireAggregateCapacity(canonicalPath, sourceBytes, location, path)
        val knownBytes = aggregateSources[canonicalPath] ?: 0L
        if (sourceBytes > knownBytes) {
            aggregateSources[canonicalPath] = sourceBytes
            aggregateSourceBytes += sourceBytes - knownBytes
        }
    }

    private fun requireAggregateCapacity(
        canonicalPath: String,
        sourceBytes: Long,
        location: SourceLocation,
        path: String,
    ) {
        val additionalBytes = (sourceBytes - (aggregateSources[canonicalPath] ?: 0L)).coerceAtLeast(0L)
        val observed = aggregateSourceBytes + additionalBytes
        if (observed > limits.maxAggregateSourceBytes) {
            limitExceeded(AGGREGATE_SOURCE_BYTES, limits.maxAggregateSourceBytes, observed, location, path)
        }
    }

    private fun limitExceeded(
        limit: ParserLoadResourceLimit,
        maximum: Long,
        observed: Long,
        location: SourceLocation,
        path: String = location.path,
    ): Nothing =
        reportLimitExceeded(
            ParserDiagnostic.LoadResourceLimitExceeded(
                limit = limit,
                maximum = maximum,
                observed = observed,
                path = path,
                sourceLocation = location,
            ),
        )

    private data class ReferenceTarget(
        val canonicalPath: String,
        val pointer: String,
    )
}
