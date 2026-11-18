package dev.banking.asyncapi.generator.core.loader

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.parser.external.ExternalReferenceLoader
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.source.SourceReader
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import dev.banking.asyncapi.generator.core.validator.LoadedTargetValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationFindingFormatter
import dev.banking.asyncapi.generator.core.validator.util.ValidationReporter
import dev.banking.asyncapi.generator.core.validator.util.ValidationWarningCollector
import java.io.File

/**
 * Public entry point for loading a validated AsyncAPI document from a file.
 *
 * The loader reads and parses the root document, loads the documents and
 * fragments its references point at, links every reference to its target,
 * and validates them all: each loaded target first, then the root document. Bundling and generation
 * are separate downstream stages.
 */
class AsyncApiDocumentLoader {
    /**
     * Fails when [file] does not exist or is not a regular file.
     *
     * Frontends call this before validating their configuration, so a wrong
     * input path is reported before anything that depends on it.
     */
    fun requireInputFile(file: File) {
        SourceReader.requireInputFile(file)
    }

    fun load(file: File): AsyncApiDocumentLoadResult {
        val context = AsyncApiContext()
        val inputDocument = SourceReader.read(file, context.sourceLimits)
        val root = ParserNodeFactory.root(inputDocument, context)
        val document = AsyncApiParser(context).parse(root)
        val loadedTargets = ExternalReferenceLoader(context).loadAll()
        context.referenceLinker.linkAll(context.parsedReferences.since(0).map { it.reference })
        val warningCollector = ValidationWarningCollector()
        warningCollector.collect(LoadedTargetValidator(context).validate(loadedTargets))
        val validationResults = AsyncApiValidator(context).validate(document)

        ValidationReporter(context).throwErrors(validationResults)
        val warnings = warningCollector.mergeWith(validationResults.warnings)
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
            sourceFiles = context.sourceBudget.sourceFiles(),
            formattedWarnings = formattedWarnings,
        )
    }
}
