package dev.banking.asyncapi.generator.core.parser.version

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.parser.diagnostics.parserFailure
import dev.banking.asyncapi.generator.core.parser.node.ParserNode

/**
 * Parser behavior implemented for one AsyncAPI major/minor specification line.
 *
 * @param displayName the specification line, such as `3.0.x`
 */
internal enum class AsyncApiParserProfile(
    val displayName: String,
) {
    V3_0(displayName = "3.0.x"),
}

/**
 * Published AsyncAPI specification lines known to this parser.
 *
 * @param major the major version
 * @param minor the minor version
 * @param parserProfile the profile that parses this line, or `null` when it is not supported
 */
internal enum class AsyncApiSpecificationLine(
    val major: Int,
    val minor: Int,
    val parserProfile: AsyncApiParserProfile?,
) {
    V3_0(major = 3, minor = 0, parserProfile = AsyncApiParserProfile.V3_0),
    V3_1(major = 3, minor = 1, parserProfile = null),
    ;

    companion object {
        val supportedVersionLines: List<String> =
            entries
                .mapNotNull(AsyncApiSpecificationLine::parserProfile)
                .map(AsyncApiParserProfile::displayName)

        /** [root] with the parser profile of the AsyncAPI version it declares. */
        fun select(root: ParserNode): ParserNode {
            val versionNode = root.expectObject().required("asyncapi")
            val declaredVersion = versionNode.expect<String>()
            val version =
                AsyncApiSpecificationVersion.parse(declaredVersion)
                    ?: throw root.context.parserFailure(
                        ParserDiagnostic.InvalidSpecificationVersion(
                            declaredVersion = declaredVersion,
                            path = versionNode.path,
                            sourceLocation = versionNode.sourceLocation,
                        ),
                    )
            return root.withProfile(profile(version, versionNode, declaredVersion))
        }

        private fun profile(
            version: AsyncApiSpecificationVersion,
            versionNode: ParserNode,
            declaredVersion: String,
        ): AsyncApiParserProfile {
            val specificationLine =
                entries.firstOrNull { candidate ->
                    candidate.major == version.major && candidate.minor == version.minor
                }
            return specificationLine?.parserProfile
                ?: throw versionNode.context.parserFailure(
                    ParserDiagnostic.UnsupportedSpecificationVersion(
                        declaredVersion = declaredVersion,
                        knownVersionLine = specificationLine != null,
                        supportedVersionLines = supportedVersionLines,
                        path = versionNode.path,
                        sourceLocation = versionNode.sourceLocation,
                    ),
                )
        }
    }
}
