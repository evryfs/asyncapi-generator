package dev.banking.asyncapi.generator.core.document

/**
 * Format-independent document produced by the reader stage.
 *
 * [InputDocument] is the parser input. Its immutable node tree contains both
 * semantic values and their source locations, but no parsed AsyncAPI models.
 *
 * Expected behavior is covered by:
 * - `DocumentReaderContractTest`
 * - `YamlDocumentReaderTest`
 */
data class InputDocument(
    val source: DocumentSource,
    val root: DocumentObject,
)
