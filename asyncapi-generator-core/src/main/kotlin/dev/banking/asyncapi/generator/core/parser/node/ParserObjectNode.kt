package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiParserProfile
import dev.banking.asyncapi.generator.core.parser.version.objectMemberPolicy

/**
 * Object-shaped view of a [ParserNode].
 *
 * This view owns member navigation and object-member policy after the source
 * node has been checked by [ParserNode.expectObject].
 */
internal class ParserObjectNode internal constructor(
    private val parserNode: ParserNode,
    private val documentObject: DocumentObject,
) {

    fun required(memberName: String): ParserNode {
        val memberAddress = parserNode.address.member(memberName)
        val member = documentObject[memberName]
            ?: throw AsyncApiParseException.ParserDiagnosticFailure(
                diagnostic = ParserDiagnostic.MissingRequiredMember(
                    memberName = memberName,
                    path = memberAddress.displayPath,
                    sourceLocation = documentObject.location.copy(path = parserNode.address.documentPath),
                ),
                context = parserNode.context,
            )
        return parserNode.member(memberName, member)
    }

    fun optional(memberName: String): ParserNode? {
        val member = documentObject[memberName] ?: return null
        return parserNode.member(memberName, member)
    }

    fun members(): List<ParserNode> =
        documentObject.members.map { (memberName, member) ->
            parserNode.member(
                name = memberName,
                node = member.value,
            )
        }

    fun membersStartingWith(prefix: String): List<ParserNode> =
        members().filter { member -> member.name.startsWith(prefix) }

    fun expectOnlyMembers(
        objectType: String,
        allowedMembers: Set<String>,
        specificationExtensionsAllowed: Boolean = true,
    ) {
        val unexpectedMember = documentObject.members.entries.firstOrNull { (memberName, _) ->
            memberName !in allowedMembers &&
                !(specificationExtensionsAllowed && memberName.matches(SPECIFICATION_EXTENSION_NAME))
        } ?: return
        val (memberName, member) = unexpectedMember
        val memberPath = parserNode.address.member(memberName).displayPath
        throw AsyncApiParseException.ParserDiagnosticFailure(
            diagnostic = ParserDiagnostic.UnexpectedObjectMember(
                memberName = memberName,
                objectType = objectType,
                specificationExtensionsAllowed = specificationExtensionsAllowed,
                path = memberPath,
                sourceLocation = member.keyLocation.copy(path = parserNode.address.member(memberName).documentPath),
            ),
            context = parserNode.context,
        )
    }

    /** Enforces the fixed-member policy selected by this node's AsyncAPI profile. */
    internal fun expectOnlyMembers(objectType: AsyncApiObjectType) {
        val profile = parserNode.profile ?: AsyncApiParserProfile.V3_0
        val policy = profile.objectMemberPolicy(objectType)
        expectOnlyMembers(
            objectType = objectType.displayName,
            allowedMembers = policy.allowedMembers,
            specificationExtensionsAllowed = policy.specificationExtensionsAllowed,
        )
    }

    private companion object {
        val SPECIFICATION_EXTENSION_NAME = Regex("""^x-[A-Za-z0-9._-]+$""")
    }
}
