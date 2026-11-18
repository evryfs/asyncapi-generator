package dev.banking.asyncapi.generator.core.parser.external

import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey

/** A document or fragment loaded because a reference points into it. */
internal sealed interface LoadedTarget {
    /**
     * An external AsyncAPI document, parsed as a whole.
     *
     * @property document the parsed document
     */
    data class Document(
        val document: AsyncApiDocument,
    ) : LoadedTarget

    /**
     * A fragment of a non-AsyncAPI file, parsed as the kind of object its reference expects.
     *
     * @property category the kind of object the fragment was parsed as
     * @property name the label of the fragment node in diagnostics
     * @property model the parsed object, a reference or an inline model
     */
    data class Fragment(
        val category: ReferenceCategoryKey,
        val name: String,
        val model: Any,
    ) : LoadedTarget
}
