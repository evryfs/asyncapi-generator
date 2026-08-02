package dev.banking.asyncapi.generator.core.validator

import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiSpecificationVersion

/** Semantic validation rules implemented for one AsyncAPI major/minor line. */
enum class AsyncApiValidationProfile(
    val major: Int,
    val minor: Int,
) {
    V3_0(major = 3, minor = 0),
    ;

    companion object {
        fun select(document: AsyncApiDocument): AsyncApiValidationProfile {
            val version = requireNotNull(AsyncApiSpecificationVersion.parse(document.asyncapi)) {
                "Parsed AsyncAPI document has an invalid specification version '${document.asyncapi}'"
            }
            return entries.singleOrNull { profile ->
                profile.major == version.major && profile.minor == version.minor
            } ?: error("No validation profile for AsyncAPI ${version.major}.${version.minor}.x")
        }
    }
}
