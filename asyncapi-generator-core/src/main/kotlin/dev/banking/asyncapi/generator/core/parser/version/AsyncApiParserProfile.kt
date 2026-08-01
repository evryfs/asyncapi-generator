package dev.banking.asyncapi.generator.core.parser.version

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.parser.node.ParserNode

/** Parser behavior implemented for one AsyncAPI major/minor specification line. */
enum class AsyncApiParserProfile(
    val major: Int,
    val minor: Int,
) {
    V3_0(major = 3, minor = 0),
    ;

    val displayName: String = "$major.$minor.x"
}

/** Published AsyncAPI specification lines known to this parser. */
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
            entries.mapNotNull(AsyncApiSpecificationLine::parserProfile)
                .map(AsyncApiParserProfile::displayName)

        fun select(root: ParserNode): ParserNode {
            val versionNode = root.required("asyncapi")
            val declaredVersion = versionNode.expect<String>()
            val version = AsyncApiSpecificationVersion.parse(declaredVersion)
                ?: throw parserFailure(
                    root = root,
                    diagnostic = ParserDiagnostic.InvalidSpecificationVersion(
                        declaredVersion = declaredVersion,
                        path = versionNode.path,
                        sourceLocation = versionNode.node.location,
                    ),
                )
            val specificationLine = entries.firstOrNull { candidate ->
                candidate.major == version.major && candidate.minor == version.minor
            }
            val profile = specificationLine?.parserProfile
                ?: throw parserFailure(
                    root = root,
                    diagnostic = ParserDiagnostic.UnsupportedSpecificationVersion(
                        declaredVersion = declaredVersion,
                        knownVersionLine = specificationLine != null,
                        supportedVersionLines = supportedVersionLines,
                        path = versionNode.path,
                        sourceLocation = versionNode.node.location,
                    ),
                )
            return root.withProfile(profile)
        }

        private fun parserFailure(
            root: ParserNode,
            diagnostic: ParserDiagnostic,
        ): AsyncApiParseException.ParserDiagnosticFailure =
            AsyncApiParseException.ParserDiagnosticFailure(diagnostic, root.context)
    }
}
