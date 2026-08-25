package dev.banking.asyncapi.generator.core.context

/**
 * Defaults and test seams for resources consumed by one complete parser load.
 *
 * @property maxSourceDocuments maximum number of source documents that can be loaded
 * @property maxReferenceTargets maximum number of resolved reference targets
 * @property maxExternalReferenceDepth maximum nesting depth for external references
 * @property maxAggregateSourceBytes maximum total bytes across all loaded source documents
 * @property maxNativeSchemaAssetBytes maximum size of a single native schema asset file
 */
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

    internal companion object {
        const val MEBIBYTE = 1024 * 1024
    }
}
