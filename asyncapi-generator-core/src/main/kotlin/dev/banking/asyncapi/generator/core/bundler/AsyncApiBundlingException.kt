package dev.banking.asyncapi.generator.core.bundler

/**
 * A contract that is valid but cannot be written as one self-contained document.
 *
 * @param message the explanation shown to the user
 */
sealed class AsyncApiBundlingException(
    message: String,
) : Exception(message) {
    /**
     * An external schema that has to be promoted to `components.schemas` has no name to promote it under.
     *
     * @param reference the `$ref` of the schema
     */
    class ExternalSchemaNameUnavailable(
        reference: String,
    ) : AsyncApiBundlingException(
            buildString {
                appendLine()
                appendLine("AsyncAPI bundling failed for recursive external schema reference '$reference'.")
                appendLine(
                    "The reference target does not provide a schema name that can be promoted to components.schemas.",
                )
                appendLine(
                    "Reference a named schema fragment so the recursive schema can be preserved in bundled output.",
                )
                appendLine()
            }.trimEnd(),
        )

    /**
     * Two different schemas would be promoted to `components.schemas` under the same name.
     *
     * @param schemaName the name both schemas would get
     * @param existingOrigin where the schema already holding the name comes from
     * @param incomingOrigin where the second schema comes from
     */
    class PromotedSchemaNameCollision(
        schemaName: String,
        existingOrigin: String,
        incomingOrigin: String,
    ) : AsyncApiBundlingException(
            buildString {
                appendLine()
                appendLine("AsyncAPI bundling failed while promoting recursive external schema '$schemaName'.")
                appendLine("The schema name is already used by a different schema in the bundled document.")
                appendLine("Existing schema: $existingOrigin")
                appendLine("Incoming schema: $incomingOrigin")
                appendLine("Use unique schema names before producing bundled output.")
                appendLine()
            }.trimEnd(),
        )
}
