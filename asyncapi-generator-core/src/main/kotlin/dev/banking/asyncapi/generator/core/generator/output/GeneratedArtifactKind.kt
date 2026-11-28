package dev.banking.asyncapi.generator.core.generator.output

/**
 * What a generated artifact is to the build that consumes it.
 */
internal enum class GeneratedArtifactKind {
    /** Kotlin or Java source, compiled with the project. */
    SOURCE,

    /** A schema file, such as `.avsc`, `.proto` or `.json`, packaged as a resource. */
    SCHEMA,
}
