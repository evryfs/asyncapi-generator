package dev.banking.asyncapi.generator.core.parser.version

/** Major and minor specification line selected from a valid AsyncAPI version. */
internal data class AsyncApiSpecificationVersion(
    val major: Int,
    val minor: Int,
) {
    companion object {
        private val VERSION_PATTERN =
            Regex("""^(\d+)\.(\d+)\.(\d+)(?:-([A-Za-z0-9]+))?$""")

        fun parse(value: String): AsyncApiSpecificationVersion? {
            val match = VERSION_PATTERN.matchEntire(value) ?: return null
            val major = match.groupValues[1].toIntOrNull() ?: return null
            val minor = match.groupValues[2].toIntOrNull() ?: return null
            match.groupValues[3].toIntOrNull() ?: return null
            return AsyncApiSpecificationVersion(
                major = major,
                minor = minor,
            )
        }
    }
}
