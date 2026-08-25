package dev.banking.asyncapi.generator.core.loader

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationFindingFormatter
import dev.banking.asyncapi.generator.core.validator.util.ValidationReporter
import java.io.File

/**
 * Public entry point for loading a validated AsyncAPI document from a file.
 *
 * The loader owns reader selection, source tracking, structural parsing,
 * external reference loading, and semantic validation. Bundling and generation
 * are separate downstream stages.
 */
class AsyncApiDocumentLoader {

    fun load(file: File): AsyncApiDocumentLoadResult {
        val context = AsyncApiContext()
        val inputDocument = DocumentReaderRegistry.read(file)
        val root = ParserNodeFactory.root(inputDocument, context)
        val document = AsyncApiParser(context).parse(root)
        val validationResults = AsyncApiValidator(context).validate(document)

        ValidationReporter(context).throwErrors(validationResults)
        val warnings = context.warningCollector.mergeWith(validationResults.warnings)
        val formattedWarnings =
            if (warnings.isEmpty()) {
                ""
            } else {
                ValidationFindingFormatter.format(
                    title = "Validation found ${warnings.size} warning(s):",
                    findings = warnings,
                    asyncApiContext = context,
                )
            }

        return AsyncApiDocumentLoadResult(
            document = document,
            warnings = warnings,
            sourceFiles = context.resourceBudget.sourceFiles(),
            formattedWarnings = formattedWarnings,
        )
    }
}
