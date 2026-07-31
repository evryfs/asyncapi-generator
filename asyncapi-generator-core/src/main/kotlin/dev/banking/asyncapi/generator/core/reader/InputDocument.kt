package dev.banking.asyncapi.generator.core.reader

/**
 * Format-independent document produced by the reader stage.
 *
 * [InputDocument] is the parser input. It contains the semantic document tree
 * with source locations needed for diagnostics, but it does not contain
 * parsed AsyncAPI model objects.
 *
 * Expected behavior is covered by:
 * - `DocumentReaderContractTest`
 * - `YamlDocumentReaderTest`
 */
data class InputDocument(
    val source: DocumentSource,
    val root: DocumentObject,
)
