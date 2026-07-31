package dev.banking.asyncapi.generator.core.model.exceptions

sealed class AsyncApiBundlingException(message: String) : Exception(message) {

    class ExternalSchemaNameUnavailable(
        reference: String,
    ) : AsyncApiBundlingException(
        buildString {
            appendLine()
            appendLine("AsyncAPI bundling failed for recursive external schema reference '$reference'.")
            appendLine("The reference target does not provide a schema name that can be promoted to components.schemas.")
            appendLine("Reference a named schema fragment so the recursive schema can be preserved in bundled output.")
            appendLine()
        }.trimEnd(),
    )

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
