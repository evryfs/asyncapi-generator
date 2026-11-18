package dev.banking.asyncapi.generator.core.source

/**
 * A per-document resource limit that a reader enforces.
 *
 * @property description name of the resource, as used in the failure message
 */
internal enum class DocumentResourceLimit(
    val description: String,
) {
    DOCUMENT_BYTES("UTF-8 document bytes"),
    DOCUMENT_CHARACTERS("document characters"),
    NESTING_DEPTH("nesting depth"),
    COLLECTION_ALIASES("collection aliases"),
    NUMBER_CHARACTERS("numeric token characters"),
}
