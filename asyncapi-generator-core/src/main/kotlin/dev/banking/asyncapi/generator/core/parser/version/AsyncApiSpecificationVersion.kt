package dev.banking.asyncapi.generator.core.parser.version

/** Exact AsyncAPI specification version declared by an input document. */
data class AsyncApiSpecificationVersion(
    val raw: String,
    val major: Int,
    val minor: Int,
    val patch: Int,
    val suffix: String?,
) {
    companion object {
        private val VERSION_PATTERN =
            Regex("""^(\d+)\.(\d+)\.(\d+)(?:-([A-Za-z0-9]+))?$""")

        fun parse(value: String): AsyncApiSpecificationVersion? {
            val match = VERSION_PATTERN.matchEntire(value) ?: return null
            val major = match.groupValues[1].toIntOrNull() ?: return null
            val minor = match.groupValues[2].toIntOrNull() ?: return null
            val patch = match.groupValues[3].toIntOrNull() ?: return null
            return AsyncApiSpecificationVersion(
                raw = value,
                major = major,
                minor = minor,
                patch = patch,
                suffix = match.groupValues[4].ifBlank { null },
            )
        }
    }
}
