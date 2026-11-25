package dev.banking.asyncapi.generator.core.generator.ir

/** Converts names from the contract into the casing of generated type, member and constant names. */
internal object NameCase {
    private val nonAlphanumeric = Regex("[^A-Za-z0-9]")
    private val acronymBoundary = Regex("([A-Z]+)([A-Z][a-z])")
    private val wordBoundary = Regex("([a-z0-9])([A-Z])")

    /** `order_created` and `order-created` become `OrderCreated`; existing capitals are kept. */
    fun toPascalCase(raw: String): String =
        raw
            .split(nonAlphanumeric)
            .filter { it.isNotBlank() }
            .joinToString("") { part ->
                part.replaceFirstChar { it.uppercase() }
            }

    /** `order-id` becomes `orderId`; an all-capital word such as `ID` is lowered first. */
    fun toCamelCase(raw: String): String =
        raw
            .split(nonAlphanumeric)
            .filter { it.isNotBlank() }
            .map { part -> if (part.all(Char::isUpperCase)) part.lowercase() else part }
            .mapIndexed { index, part ->
                if (index == 0) {
                    part.replaceFirstChar { it.lowercase() }
                } else {
                    part.replaceFirstChar { it.uppercase() }
                }
            }.joinToString("")

    /** `accountEvents` and `account.events` become `ACCOUNT_EVENTS`. */
    fun toUpperSnakeCase(raw: String): String =
        raw
            .replace(acronymBoundary, "$1_$2")
            .replace(wordBoundary, "$1_$2")
            .split(nonAlphanumeric)
            .filter { it.isNotBlank() }
            .joinToString("_") { part -> part.uppercase() }
}
