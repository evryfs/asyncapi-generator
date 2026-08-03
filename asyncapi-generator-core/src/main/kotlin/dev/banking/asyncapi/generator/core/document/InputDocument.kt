package dev.banking.asyncapi.generator.core.document

/**
 * Format-independent document produced by the reader stage.
 *
 * [InputDocument] is the parser input. Its immutable node tree contains both
 * semantic values and their source locations, but no parsed AsyncAPI models.
 * The root may be any node representable by the input format; consumers own
 * domain-specific root-shape requirements.
 *
 * @property source content and identity from which this document was read
 * @property root source-located root value, which is not required to be an object
 *
 * Expected behavior is covered by:
 * - `DocumentReaderContractTest`
 * - `YamlDocumentReaderTest`
 */
internal data class InputDocument(
    val source: DocumentSource,
    val root: DocumentNode,
)
