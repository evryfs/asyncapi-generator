package dev.banking.asyncapi.generator.core.parser.external

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.parser.node.ParserNode

/**
 * The references of one load in the order the parser met them, each with how to parse its target.
 */
internal class ParsedReferences {
    /**
     * One parsed reference.
     *
     * @property reference the reference
     * @property parseTarget parses the node the reference points at into the model the reference expects
     */
    class Entry(
        val reference: Reference,
        val parseTarget: (ParserNode) -> Any,
    )

    private val entries = mutableListOf<Entry>()

    /** The number of references parsed so far. */
    val size: Int
        get() = entries.size

    /** Records [reference], whose target [parseTarget] parses. */
    fun add(
        reference: Reference,
        parseTarget: (ParserNode) -> Any,
    ) {
        entries += Entry(reference, parseTarget)
    }

    /** The references parsed after the first [count] ones. */
    fun since(count: Int): List<Entry> = entries.subList(count, entries.size).toList()
}
