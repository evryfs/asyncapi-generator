package dev.banking.asyncapi.generator.core.source

import java.io.File
import kotlin.text.Charsets.UTF_8

/**
 * Resource limits for reading the sources of one load.
 *
 * The document limits apply to every YAML or JSON document on its own. The load limits apply to all
 * documents and native schema assets read while loading one root document.
 *
 * @property maxDocumentBytes maximum size of one document in UTF-8 bytes
 * @property maxDocumentCharacters maximum length of one document in characters
 * @property maxNestingDepth maximum object and array nesting depth of one document
 * @property maxAliasesForCollections maximum number of YAML aliases to collections in one document
 * @property maxNumberCharacters maximum length of one numeric token
 * @property maxSourceDocuments maximum number of documents in one load
 * @property maxReferenceTargets maximum number of distinct reference targets in one load
 * @property maxExternalReferenceDepth maximum nesting of external references in one load
 * @property maxAggregateSourceBytes maximum total size of all documents and assets in one load
 * @property maxNativeSchemaAssetBytes maximum size of one native Avro or Protobuf schema asset
 */
internal data class SourceLimits(
    val maxDocumentBytes: Int = DOCUMENT_SIZE,
    val maxDocumentCharacters: Int = DOCUMENT_SIZE,
    val maxNestingDepth: Int = 100,
    val maxAliasesForCollections: Int = 50,
    val maxNumberCharacters: Int = 1000,
    val maxSourceDocuments: Int = 256,
    val maxReferenceTargets: Int = 4096,
    val maxExternalReferenceDepth: Int = 64,
    val maxAggregateSourceBytes: Long = AGGREGATE_SIZE,
    val maxNativeSchemaAssetBytes: Int = DOCUMENT_SIZE,
) {
    init {
        require(maxSourceDocuments >= 0)
        require(maxReferenceTargets >= 0)
        require(maxExternalReferenceDepth >= 0)
        require(maxAggregateSourceBytes >= 0)
        require(maxNativeSchemaAssetBytes >= 0)
    }

    /** Fails when the content of [source] exceeds the document size limits. */
    fun requireDocumentSize(source: DocumentSource) {
        if (source.content.toByteArray(UTF_8).size > maxDocumentBytes) {
            throw documentLimitExceeded(source.file, DocumentResourceLimit.DOCUMENT_BYTES, maxDocumentBytes)
        }
        if (source.content.length > maxDocumentCharacters) {
            throw documentLimitExceeded(source.file, DocumentResourceLimit.DOCUMENT_CHARACTERS, maxDocumentCharacters)
        }
    }

    /** Fails when the numeric token [value] at [location] is longer than [maxNumberCharacters]. */
    fun requireNumberLength(
        value: String,
        location: SourceLocation,
    ) {
        if (value.length > maxNumberCharacters) {
            throw DocumentReadException.ResourceLimitExceeded(
                file = location.file,
                limit = DocumentResourceLimit.NUMBER_CHARACTERS,
                maximum = maxNumberCharacters.toLong(),
                cause = IllegalArgumentException("Maximum numeric token length is $maxNumberCharacters characters"),
                location = location,
            )
        }
    }

    /** The failure for a [file] that exceeds the per-document [limit] of [maximum]. */
    fun documentLimitExceeded(
        file: File,
        limit: DocumentResourceLimit,
        maximum: Int,
    ): DocumentReadException.ResourceLimitExceeded =
        DocumentReadException.ResourceLimitExceeded(
            file = file,
            limit = limit,
            maximum = maximum.toLong(),
            cause = IllegalArgumentException("Maximum ${limit.description} is $maximum"),
        )

    private companion object {
        const val MEBIBYTE = 1024 * 1024
        const val DOCUMENT_SIZE = 20 * MEBIBYTE
        const val AGGREGATE_SIZE = 64L * MEBIBYTE
    }
}
