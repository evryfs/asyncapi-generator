package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.validator.ValidationReport
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationReporter
import java.io.File

/**
 * Fixture facade for validator-stage tests.
 *
 * It builds parsed documents through [ParserFixtures] and exposes validation
 * helpers that keep validator tests focused on expected validation results.
 */
internal class ValidatorFixtures(
    private val context: AsyncApiContext = AsyncApiContext(),
) {
    private val parserFixtures = ParserFixtures(context)
    private val validator = AsyncApiValidator(context)

    fun document(path: String): AsyncApiDocument =
        parserFixtures.document(path)

    fun document(file: File): AsyncApiDocument =
        parserFixtures.document(file)

    fun validate(document: AsyncApiDocument): ValidationReport =
        validator.validate(document)

    fun validate(path: String): ValidationReport =
        validate(document(path))

    fun validate(file: File): ValidationReport =
        validate(document(file))

    fun validatedDocument(path: String): AsyncApiDocument =
        validatedDocument(TestResources.file(path))

    fun validatedDocument(file: File): AsyncApiDocument {
        val document = document(file)
        val report = validate(document)
        ValidationReporter(context).logWarnings(report)
        ValidationReporter(context).throwErrors(report)
        return document
    }
}
