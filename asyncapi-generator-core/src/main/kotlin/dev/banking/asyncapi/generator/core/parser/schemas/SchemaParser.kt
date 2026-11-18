package dev.banking.asyncapi.generator.core.parser.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SCHEMA
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaFormat
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.parser.diagnostics.parserFailure
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseList
import dev.banking.asyncapi.generator.core.parser.node.parseMap
import dev.banking.asyncapi.generator.core.parser.node.referenceOrNull
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.MULTI_FORMAT_SCHEMA
import dev.banking.asyncapi.generator.core.source.DocumentArray
import dev.banking.asyncapi.generator.core.source.DocumentBoolean
import dev.banking.asyncapi.generator.core.source.DocumentNull
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.SCHEMA as SCHEMA_BINDING

/**
 * Parses AsyncAPI schema objects from parser nodes.
 *
 * @param asyncApiContext shared context for source tracking, model registration, and diagnostics
 */
internal class SchemaParser(
    private val asyncApiContext: AsyncApiContext,
) {
    private val bindingParser = BindingParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser()
    private val nativeSchemaAssetReader = NativeSchemaAssetReader(asyncApiContext)

    /** Parses a Schema Object, a boolean schema, a Multi Format Schema Object or a `$ref` to a schema. */
    fun parseElement(parserNode: ParserNode): SchemaInterface =
        if (parserNode.node is DocumentBoolean) {
            SchemaInterface
                .BooleanSchema(value = parserNode.expect<Boolean>())
                .also { asyncApiContext.register(it, parserNode) }
        } else {
            parserNode.referenceOrNull(SCHEMA, ::parseElement)?.let(SchemaInterface::SchemaReference)
                ?: parserNode
                    .expectObject()
                    .optional("schemaFormat")
                    ?.expect<String>()
                    ?.let { format -> parseMultiFormat(parserNode, format) }
                ?: parseSchema(parserNode)
        }

    private fun parseMultiFormat(
        parserNode: ParserNode,
        format: String,
    ): SchemaInterface {
        val objectNode = parserNode.expectObject()
        objectNode.expectOnlyMembers(MULTI_FORMAT_SCHEMA)
        val schemaFormat =
            SchemaFormat.fromValue(format)
                ?: throw asyncApiContext.parserFailure(
                    ParserDiagnostic.UnknownSchemaFormat(format, parserNode.path, parserNode.declarationLocation),
                )
        val schemaNode = objectNode.required("schema")
        if (schemaFormat.isAsyncApiSchemaObject) return parseElement(schemaNode)
        val schemaContent =
            if (schemaFormat.isNativeAvro || schemaFormat.isNativeProtobuf) {
                nativeSchemaAssetReader.readIfExternalReference(schemaNode) ?: schemaNode.toPlainValue()
            } else {
                schemaNode.toPlainValue()
            }
        val multiFormatSchema = MultiFormatSchema(schemaFormat = format, schema = schemaContent, format = schemaFormat)
        return SchemaInterface
            .MultiFormatSchemaInline(multiFormatSchema)
            .also { asyncApiContext.register(multiFormatSchema, parserNode) }
    }

    /** Parses a Schema Object; its members are read in the order the JSON Schema vocabularies list them. */
    private fun parseSchema(parserNode: ParserNode): SchemaInterface {
        val objectNode = parserNode.expectObject()
        val defaultNode = objectNode.optional("default")
        val itemsNode = objectNode.optional("items")
        val constNode = objectNode.optional("const")
        return SchemaInterface.SchemaInline(
            Schema(
                id = objectNode.optional($$"$id")?.expect<String>(),
                schema = objectNode.optional($$"$schema")?.expect<String>(),
                comment = objectNode.optional($$"$comment")?.expect<String>(),
                title = objectNode.optional("title")?.expect<String>(),
                description = objectNode.optional("description")?.expect<String>(),
                type = objectNode.optional("type")?.let(::parseType),
                format = objectNode.optional("format")?.expect<String>(),
                default = defaultNode?.toPlainValue(),
                defaultSet = defaultNode != null,
                examples = objectNode.optional("examples")?.expect<List<Any?>>(),
                multipleOf = objectNode.optional("multipleOf")?.expect<Number>(),
                maximum = objectNode.optional("maximum")?.expect<Number>(),
                exclusiveMaximum = objectNode.optional("exclusiveMaximum")?.expect<Number>(),
                minimum = objectNode.optional("minimum")?.expect<Number>(),
                exclusiveMinimum = objectNode.optional("exclusiveMinimum")?.expect<Number>(),
                maxLength = objectNode.optional("maxLength")?.expect<Number>(),
                minLength = objectNode.optional("minLength")?.expect<Number>(),
                pattern = objectNode.optional("pattern")?.expect<String>(),
                contentEncoding = objectNode.optional("contentEncoding")?.expect<String>(),
                contentMediaType = objectNode.optional("contentMediaType")?.expect<String>(),
                items = itemsNode?.takeUnless { it.node is DocumentArray }?.let(::parseElement),
                tupleItems = itemsNode?.takeIf { it.node is DocumentArray }?.parseList(::parseElement),
                additionalItems = objectNode.optional("additionalItems")?.let(::parseElement),
                maxItems = objectNode.optional("maxItems")?.expect<Number>(),
                minItems = objectNode.optional("minItems")?.expect<Number>(),
                uniqueItems = objectNode.optional("uniqueItems")?.expect<Boolean>(),
                contains = objectNode.optional("contains")?.let(::parseElement),
                properties = objectNode.optional("properties")?.parseMap(::parseElement),
                patternProperties = objectNode.optional("patternProperties")?.parseMap(::parseElement),
                additionalProperties = objectNode.optional("additionalProperties")?.let(::parseElement),
                propertyNames = objectNode.optional("propertyNames")?.let(::parseElement),
                required = objectNode.optional("required")?.expect<List<String>>(),
                dependencies = objectNode.optional("dependencies")?.let(::parseDependencies),
                definitions = objectNode.optional("definitions")?.parseMap(::parseElement),
                maxProperties = objectNode.optional("maxProperties")?.expect<Number>(),
                minProperties = objectNode.optional("minProperties")?.expect<Number>(),
                allOf = objectNode.optional("allOf")?.parseList(::parseElement),
                anyOf = objectNode.optional("anyOf")?.parseList(::parseElement),
                oneOf = objectNode.optional("oneOf")?.parseList(::parseElement),
                not = objectNode.optional("not")?.let(::parseElement),
                ifSchema = objectNode.optional("if")?.let(::parseElement),
                thenSchema = objectNode.optional("then")?.let(::parseElement),
                elseSchema = objectNode.optional("else")?.let(::parseElement),
                enum = objectNode.optional("enum")?.expect<List<Any?>>(),
                const = constNode?.toPlainValue(),
                constSet = constNode != null,
                discriminator = objectNode.optional("discriminator")?.expect<String>(),
                externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                deprecated = objectNode.optional("deprecated")?.expect<Boolean>(),
                bindings = objectNode.optional("bindings")?.let { bindingParser.parseMap(it, SCHEMA_BINDING) },
                extensions = objectNode.extensions(),
                readOnly = objectNode.optional("readOnly")?.expect<Boolean>(),
                writeOnly = objectNode.optional("writeOnly")?.expect<Boolean>(),
            ).also { asyncApiContext.register(it, parserNode) },
        )
    }

    private fun parseType(parserNode: ParserNode): Any? =
        when (parserNode.node) {
            is DocumentNull -> null
            is DocumentArray -> parserNode.expect<List<String>>()
            else -> parserNode.expect<String>()
        }

    private fun parseDependencies(parserNode: ParserNode): Map<String, Any> =
        parserNode.expectObject().members().mapValues { (_, dependency) ->
            when (dependency.node) {
                is DocumentArray -> dependency.expect<List<String>>().also { asyncApiContext.register(it, dependency) }
                else -> parseElement(dependency)
            }
        }
}
