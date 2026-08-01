package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.reader.DocumentArray
import dev.banking.asyncapi.generator.core.reader.DocumentNode
import dev.banking.asyncapi.generator.core.reader.DocumentNull
import dev.banking.asyncapi.generator.core.reader.DocumentObject
import dev.banking.asyncapi.generator.core.reader.DocumentString
import dev.banking.asyncapi.generator.core.reader.toValue
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@PublishedApi
internal object ParserValueExpectation {

    @PublishedApi
    internal fun expect(
        node: DocumentNode,
        expectedType: KType,
        path: String,
        context: AsyncApiContext,
    ): Any? {
        if (node is DocumentNull) {
            if (expectedType.isMarkedNullable) {
                return null
            }
            unexpectedType(node, expectedType, path, context)
        }

        return when (expectedType.classifier) {
            List::class -> expectList(node, expectedType, path, context)
            Map::class -> expectMap(node, expectedType, path, context)
            Any::class -> node.toValue()
            else -> expectScalar(node, expectedType, path, context)
        }
    }

    private fun expectList(
        node: DocumentNode,
        expectedType: KType,
        path: String,
        context: AsyncApiContext,
    ): List<Any?> {
        val array = node as? DocumentArray
            ?: unexpectedType(node, expectedType, path, context)
        val elementType = expectedType.arguments.singleOrNull()?.type ?: typeOf<Any?>()
        return array.elements.mapIndexed { index, element ->
            expect(element, elementType, "$path[$index]", context)
        }
    }

    private fun expectMap(
        node: DocumentNode,
        expectedType: KType,
        path: String,
        context: AsyncApiContext,
    ): Map<String, Any?> {
        val objectNode = node as? DocumentObject
            ?: unexpectedType(node, expectedType, path, context)
        val keyType = expectedType.arguments.getOrNull(0)?.type ?: typeOf<Any?>()
        val valueType = expectedType.arguments.getOrNull(1)?.type ?: typeOf<Any?>()
        return objectNode.members.mapValuesTo(linkedMapOf()) { (name, member) ->
            expect(
                node = DocumentString(name, member.keyLocation),
                expectedType = keyType,
                path = "$path.$name",
                context = context,
            )
            expect(member.value, valueType, "$path.$name", context)
        }
    }

    private fun expectScalar(
        node: DocumentNode,
        expectedType: KType,
        path: String,
        context: AsyncApiContext,
    ): Any {
        if (expectedType.arguments.isNotEmpty()) {
            unexpectedType(node, expectedType, path, context)
        }
        val value = node.toValue()
            ?: unexpectedType(node, expectedType, path, context)
        val expectedClass = expectedType.classifier as? KClass<*>
            ?: unexpectedType(node, expectedType, path, context)
        return value.takeIf(expectedClass::isInstance)
            ?: unexpectedType(node, expectedType, path, context)
    }

    internal fun unexpectedType(
        node: DocumentNode,
        expectedType: KType,
        path: String,
        context: AsyncApiContext,
    ): Nothing =
        throw AsyncApiParseException.ParserDiagnosticFailure(
            diagnostic = ParserDiagnostic.UnexpectedValueType(
                expectedType = expectedType,
                actualType = actualType(node),
                actualValue = node.toValue(),
                path = path,
                sourceLocation = node.location,
            ),
            context = context,
        )

    private fun actualType(node: DocumentNode): KClass<*>? =
        when (node) {
            is DocumentObject -> Map::class
            is DocumentArray -> List::class
            is DocumentNull -> null
            else -> node.toValue()?.let { value -> value::class }
        }
}
