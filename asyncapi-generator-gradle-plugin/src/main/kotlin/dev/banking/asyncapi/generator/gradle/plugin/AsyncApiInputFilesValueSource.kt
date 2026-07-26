package dev.banking.asyncapi.generator.gradle.plugin

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

/**
 * Discovers all files read while parsing one AsyncAPI contract.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiInputFilesValueSource :
    ValueSource<List<String>, AsyncApiInputFilesValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val inputSpec: RegularFileProperty
    }

    override fun obtain(): List<String> {
        val context = AsyncApiContext()
        val root = AsyncApiRegistry.read(parameters.inputSpec.get().asFile, context)

        AsyncApiParser(context).parse(root)

        return context.sourceRepository
            .getAllSources()
            .map { source ->
                source.file
                    .toPath()
                    .toAbsolutePath()
                    .normalize()
                    .toString()
            }.distinct()
            .sorted()
    }
}
