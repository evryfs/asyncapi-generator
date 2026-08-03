package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserLoadResourceLimit
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction.REPORT
import java.nio.charset.StandardCharsets.UTF_8

/** Defaults and test seams for resources consumed by one complete parser load. */
internal data class ParserLoadResourceLimits(
    val maxSourceDocuments: Int = 256,
    val maxReferenceTargets: Int = 4096,
    val maxExternalReferenceDepth: Int = 64,
    val maxAggregateSourceBytes: Long = 64L * MEBIBYTE,
    val maxNativeSchemaAssetBytes: Int = 20 * MEBIBYTE,
) {
    init {
        require(maxSourceDocuments >= 0)
        require(maxReferenceTargets >= 0)
        require(maxExternalReferenceDepth >= 0)
        require(maxAggregateSourceBytes >= 0)
        require(maxNativeSchemaAssetBytes >= 0)
    }

    private companion object {
        const val MEBIBYTE = 1024 * 1024
    }
}

/** Mutable accounting owned by exactly one [AsyncApiContext]. */
internal class ParserLoadResourceBudget(
    private val limits: ParserLoadResourceLimits,
) {
    private val sourceDocuments = mutableMapOf<String, Long>()
    private val aggregateSources = mutableMapOf<String, Long>()
    private val referenceTargets = mutableSetOf<ReferenceTargetIdentity>()
    private val nativeSchemaAssets = mutableMapOf<String, String>()
    private var aggregateSourceBytes = 0L
    private var externalReferenceDepth = 0

    fun registerDocument(
        file: File,
        sourceBytes: Long,
    ) {
        require(sourceBytes >= 0)
        val canonicalPath = file.canonicalPath
        val existingBytes = sourceDocuments[canonicalPath]
        if (existingBytes != null) {
            if (sourceBytes > existingBytes) {
                requireAggregateCapacity(canonicalPath, sourceBytes)
                sourceDocuments[canonicalPath] = sourceBytes
                registerAggregateSource(canonicalPath, sourceBytes)
            }
            return
        }

        requireWithinLimit(
            limit = ParserLoadResourceLimit.SOURCE_DOCUMENTS,
            maximum = limits.maxSourceDocuments.toLong(),
            observed = sourceDocuments.size + 1L,
        )
        requireAggregateCapacity(canonicalPath, sourceBytes)

        sourceDocuments[canonicalPath] = sourceBytes
        registerAggregateSource(canonicalPath, sourceBytes)
    }

    @Throws(IOException::class)
    fun registerExternalDocument(file: File) {
        val canonicalFile = file.canonicalFile
        registerDocument(canonicalFile, canonicalFile.length())
    }

    fun registerReferenceTarget(
        file: File,
        pointer: String,
    ) {
        val identity = ReferenceTargetIdentity(file.canonicalPath, pointer)
        if (identity in referenceTargets) return

        requireWithinLimit(
            limit = ParserLoadResourceLimit.REFERENCE_TARGETS,
            maximum = limits.maxReferenceTargets.toLong(),
            observed = referenceTargets.size + 1L,
        )
        referenceTargets += identity
    }

    fun <T> withinExternalReference(block: () -> T): T {
        val nextDepth = externalReferenceDepth + 1L
        requireWithinLimit(
            limit = ParserLoadResourceLimit.EXTERNAL_REFERENCE_DEPTH,
            maximum = limits.maxExternalReferenceDepth.toLong(),
            observed = nextDepth,
        )

        externalReferenceDepth++
        return try {
            block()
        } finally {
            externalReferenceDepth--
        }
    }

    @Throws(IOException::class)
    fun readNativeSchemaAsset(file: File): String {
        val canonicalFile = file.canonicalFile
        nativeSchemaAssets[canonicalFile.absolutePath]?.let { return it }

        val sourceBytes = canonicalFile.length()
        requireWithinLimit(
            limit = ParserLoadResourceLimit.NATIVE_SCHEMA_ASSET_BYTES,
            maximum = limits.maxNativeSchemaAssetBytes.toLong(),
            observed = sourceBytes,
        )
        requireAggregateCapacity(canonicalFile.absolutePath, sourceBytes)

        val bytes = canonicalFile.inputStream().use { input ->
            input.readNBytes(limits.maxNativeSchemaAssetBytes + 1)
        }
        requireWithinLimit(
            limit = ParserLoadResourceLimit.NATIVE_SCHEMA_ASSET_BYTES,
            maximum = limits.maxNativeSchemaAssetBytes.toLong(),
            observed = bytes.size.toLong(),
        )
        val content = UTF_8.newDecoder()
            .onMalformedInput(REPORT)
            .onUnmappableCharacter(REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()

        requireAggregateCapacity(canonicalFile.absolutePath, bytes.size.toLong())
        registerAggregateSource(canonicalFile.absolutePath, bytes.size.toLong())
        nativeSchemaAssets[canonicalFile.absolutePath] = content
        return content
    }

    private fun requireAggregateCapacity(
        canonicalPath: String,
        sourceBytes: Long,
    ) {
        val existingBytes = aggregateSources[canonicalPath] ?: 0L
        val additionalBytes = (sourceBytes - existingBytes).coerceAtLeast(0L)
        requireWithinLimit(
            limit = ParserLoadResourceLimit.AGGREGATE_SOURCE_BYTES,
            maximum = limits.maxAggregateSourceBytes,
            observed = aggregateSourceBytes + additionalBytes,
        )
    }

    private fun registerAggregateSource(
        canonicalPath: String,
        sourceBytes: Long,
    ) {
        val existingBytes = aggregateSources[canonicalPath] ?: 0L
        if (sourceBytes > existingBytes) {
            aggregateSources[canonicalPath] = sourceBytes
            aggregateSourceBytes += sourceBytes - existingBytes
        }
    }

    private fun requireWithinLimit(
        limit: ParserLoadResourceLimit,
        maximum: Long,
        observed: Long,
    ) {
        if (observed > maximum) {
            throw ParserLoadResourceLimitExceeded(limit, maximum, observed)
        }
    }

    private data class ReferenceTargetIdentity(
        val canonicalFile: String,
        val pointer: String,
    )
}

internal class ParserLoadResourceLimitExceeded(
    val limit: ParserLoadResourceLimit,
    val maximum: Long,
    val observed: Long,
) : RuntimeException("Parser load exceeded ${limit.displayName} limit of $maximum: observed $observed")
