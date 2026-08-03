package dev.banking.asyncapi.generator.gradle.plugin

import dev.banking.asyncapi.generator.core.loader.AsyncApiDocumentLoader
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
        return AsyncApiDocumentLoader()
            .load(parameters.inputSpec.get().asFile)
            .sourceFiles
            .map { file -> file.absolutePath }
            .sorted()
    }
}
