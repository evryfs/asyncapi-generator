package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.parser.diagnostics.parserFailure
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiParserProfile
import dev.banking.asyncapi.generator.core.parser.version.allowedMembers
import dev.banking.asyncapi.generator.core.source.DocumentObject

/**
 * Object-shaped view of a [ParserNode].
 *
 * This view owns member navigation and object-member policy after the source
 * node has been checked by [ParserNode.expectObject].
 *
 * @param parserNode the source node this view wraps
 * @param documentObject the underlying object value
 */
internal class ParserObjectNode(
    private val parserNode: ParserNode,
    private val documentObject: DocumentObject,
) {
    fun required(memberName: String): ParserNode {
        val memberAddress = parserNode.address.member(memberName)
        val member =
            documentObject[memberName]
                ?: throw parserNode.context.parserFailure(
                    ParserDiagnostic.MissingRequiredMember(
                        memberName = memberName,
                        path = memberAddress.displayPath,
                        sourceLocation = documentObject.location.copy(path = parserNode.address.documentPath),
                    ),
                )
        return parserNode.member(memberName, member)
    }

    fun optional(memberName: String): ParserNode? {
        val member = documentObject[memberName] ?: return null
        return parserNode.member(memberName, member)
    }

    /** The members of this object keyed by name, in document order. */
    fun members(): Map<String, ParserNode> =
        documentObject.members.entries.associate { (memberName, member) ->
            memberName to parserNode.member(name = memberName, node = member.value)
        }

    /** The `x-` specification extensions of this object as plain values, or `null` when it has none. */
    fun extensions(): Map<String, Any?>? =
        members()
            .filterKeys { memberName -> memberName.startsWith("x-") }
            .mapValues { (_, member) -> member.toPlainValue() }
            .takeIf { it.isNotEmpty() }

    /** Throws if the object contains members not in [allowedMembers]. */
    fun expectOnlyMembers(
        objectType: String,
        allowedMembers: Set<String>,
        specificationExtensionsAllowed: Boolean = true,
    ) {
        val unexpectedMember =
            documentObject.members.entries.firstOrNull { (memberName, _) ->
                memberName !in allowedMembers &&
                    !(specificationExtensionsAllowed && memberName.matches(SPECIFICATION_EXTENSION_NAME))
            } ?: return
        val (memberName, member) = unexpectedMember
        val memberPath = parserNode.address.member(memberName).displayPath
        throw parserNode.context.parserFailure(
            ParserDiagnostic.UnexpectedObjectMember(
                memberName = memberName,
                objectType = objectType,
                specificationExtensionsAllowed = specificationExtensionsAllowed,
                path = memberPath,
                sourceLocation = member.keyLocation.copy(path = parserNode.address.member(memberName).documentPath),
            ),
        )
    }

    /** Enforces the fixed-member policy selected by this node's AsyncAPI profile. */
    internal fun expectOnlyMembers(objectType: AsyncApiObjectType) {
        val profile = parserNode.profile ?: AsyncApiParserProfile.V3_0
        expectOnlyMembers(
            objectType = objectType.displayName,
            allowedMembers = profile.allowedMembers(objectType),
        )
    }

    private companion object {
        val SPECIFICATION_EXTENSION_NAME = Regex("""^x-[A-Za-z0-9._-]+$""")
    }
}
