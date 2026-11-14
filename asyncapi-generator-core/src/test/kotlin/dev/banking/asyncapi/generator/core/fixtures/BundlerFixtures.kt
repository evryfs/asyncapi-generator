package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.bundler.BundledAsyncApiDocument
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import java.io.File

/**
 * Fixture facade for bundler and generator tests.
 *
 * It produces validated and bundled AsyncAPI documents from shared test
 * resources, preserving the parser -> validator -> bundler stage flow.
 */
internal class BundlerFixtures(
    context: AsyncApiContext = AsyncApiContext(),
) {
    private val validatorFixtures = ValidatorFixtures(context)
    private val bundler = AsyncApiBundler()

    fun validatedDocument(path: String): AsyncApiDocument =
        validatorFixtures.validatedDocument(path)

    fun validatedDocument(file: File): AsyncApiDocument =
        validatorFixtures.validatedDocument(file)

    fun bundledDocument(path: String): BundledAsyncApiDocument =
        bundledDocument(TestResources.file(path))

    fun bundledDocument(file: File): BundledAsyncApiDocument =
        bundler.bundle(validatedDocument(file))
}
