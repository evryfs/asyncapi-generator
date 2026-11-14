package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument

/**
 * An [AsyncApiDocument] whose references have been bundled.
 *
 * Only [AsyncApiBundler] creates it. It is the input of the generator, so an unbundled document
 * cannot reach code generation.
 */
class BundledAsyncApiDocument internal constructor(
    val document: AsyncApiDocument,
)
