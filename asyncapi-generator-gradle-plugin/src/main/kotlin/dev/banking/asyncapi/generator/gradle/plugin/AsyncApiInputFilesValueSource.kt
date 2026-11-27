package dev.banking.asyncapi.generator.gradle.plugin

import dev.banking.asyncapi.generator.core.AsyncApiGenerator
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

/**
 * Discovers all files read while parsing one AsyncAPI contract.
 */
abstract class AsyncApiInputFilesValueSource : ValueSource<List<String>, AsyncApiInputFilesValueSource.Parameters> {
    /** The contract whose files are discovered. */
    interface Parameters : ValueSourceParameters {
        val inputSpec: RegularFileProperty
    }

    override fun obtain(): List<String> =
        AsyncApiGenerator()
            .inputFiles(parameters.inputSpec.get().asFile)
            .map { file -> file.absolutePath }
            .sorted()
}
