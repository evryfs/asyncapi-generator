package dev.banking.asyncapi.generator.core.generator.output

/**
 * Builds repository-relative paths for generated artifacts.
 *
 * Dot-separated namespaces, such as JVM packages and Avro namespaces, are
 * converted into directory segments before appending the generated file name.
 *
 * Expected behavior is covered by:
 * - `GeneratedArtifactPathsTest`
 */
object GeneratedArtifactPaths {
    fun fromNamespace(
        namespace: String,
        fileName: String,
    ): String {
        require(fileName.isNotBlank()) {
            "Generated artifact file name cannot be blank"
        }

        val namespacePath = namespace.trim().replace('.', '/')
        return if (namespacePath.isBlank()) {
            fileName
        } else {
            "$namespacePath/$fileName"
        }
    }
}
