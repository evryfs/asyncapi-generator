package dev.banking.asyncapi.generator.core.model.parameters

/** Matches a `{name}` parameter placeholder in a channel address or server host and captures the name. */
internal val PARAMETER_PLACEHOLDER = Regex("""\{([^}]+)}""")
