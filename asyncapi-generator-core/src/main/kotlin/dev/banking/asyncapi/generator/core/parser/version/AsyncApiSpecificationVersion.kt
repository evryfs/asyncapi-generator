package dev.banking.asyncapi.generator.core.parser.version

/**
 * The specification line of a valid AsyncAPI version.
 *
 * @param major the major version
 * @param minor the minor version
 */
internal data class AsyncApiSpecificationVersion(
    val major: Int,
    val minor: Int,
) {
    companion object {
        private val VERSION_PATTERN =
            Regex("""^(\d+)\.(\d+)\.(\d+)(?:-([A-Za-z0-9]+))?$""")

        /** The specification line of [value], or `null` when it is not a `major.minor.patch` version. */
        fun parse(value: String): AsyncApiSpecificationVersion? {
            val (major, minor, patch) =
                VERSION_PATTERN.matchEntire(value)?.destructured?.let { (major, minor, patch) ->
                    Triple(major.toIntOrNull(), minor.toIntOrNull(), patch.toIntOrNull())
                } ?: return null
            return if (major != null && minor != null &&
                patch != null
            ) {
                AsyncApiSpecificationVersion(major, minor)
            } else {
                null
            }
        }
    }
}
