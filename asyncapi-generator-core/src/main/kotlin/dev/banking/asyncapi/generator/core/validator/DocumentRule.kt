package dev.banking.asyncapi.generator.core.validator

import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

/** A rule about how objects of a whole document relate, checked after every object has been checked. */
internal fun interface DocumentRule {
    /**
     * Checks [document] and reports findings to [results].
     *
     * @param document the document
     * @param results the collector findings are reported to
     */
    fun check(
        document: AsyncApiDocument,
        results: ValidationCollector,
    )
}
