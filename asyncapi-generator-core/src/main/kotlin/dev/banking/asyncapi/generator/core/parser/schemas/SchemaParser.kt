package dev.banking.asyncapi.generator.core.parser.schemas

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.SCHEMA as SCHEMA_BINDING
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SCHEMA
import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentBoolean
import dev.banking.asyncapi.generator.core.document.DocumentNull
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException.UnexpectedSchemaFormat
import dev.banking.asyncapi.generator.core.model.schemas.SchemaFormat
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.MULTI_FORMAT_SCHEMA

/**
 * Parses AsyncAPI schema objects from parser nodes.
 */
internal class SchemaParser(
    private val asyncApiContext: AsyncApiContext,
) {

    private val bindingParser = BindingParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val nativeSchemaAssetReader = NativeSchemaAssetReader(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, SchemaInterface> =
        parserNode.expectObject().members().associate { node ->
            node.name to parseElement(node)
        }

    fun parseList(parserNode: ParserNode): List<SchemaInterface> =
        parserNode.expectArray().elements().map(::parseElement)

    fun parseElement(parserNode: ParserNode): SchemaInterface {
        if (parserNode.node is DocumentBoolean) {
            val bool = parserNode.expect<Boolean>()
            return SchemaInterface.BooleanSchema(
                value = bool,
            ).also { asyncApiContext.register(it, parserNode) }
        }
        val objectNode = parserNode.expectObject()
        objectNode.optional($$"$ref")?.expect<String>()?.let { reference ->
            return SchemaInterface.SchemaReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = SCHEMA
                )
            ).also { asyncApiContext.register(it.reference, parserNode) }
        }
        objectNode.optional("schemaFormat")?.expect<String>()?.let { format ->
            objectNode.expectOnlyMembers(MULTI_FORMAT_SCHEMA)
            val schemaFormat = SchemaFormat.fromValue(format)
                ?: throw UnexpectedSchemaFormat(
                    format,
                    parserNode.path,
                    parserNode.sourceLocation,
                    asyncApiContext,
                )
            val schemaNode = objectNode.required("schema")
            if (schemaFormat.isAsyncApiSchemaObject) {
                return parseElement(schemaNode)
            }
            val schemaContent =
                if (schemaFormat.isNativeAvro || schemaFormat.isNativeProtobuf) {
                    nativeSchemaAssetReader.readIfExternalReference(schemaNode) ?: schemaNode.toPlainValue()
                } else {
                    schemaNode.toPlainValue()
                }
            val multiFormatSchema =
                MultiFormatSchema(
                    schemaFormat = format,
                    schema = schemaContent,
                    format = schemaFormat,
                )
            return SchemaInterface.MultiFormatSchemaInline(multiFormatSchema)
                .also { asyncApiContext.register(multiFormatSchema, parserNode) }
        }
        return parseSchema(parserNode)
    }

    private fun parseSchema(parserNode: ParserNode): SchemaInterface {
        val objectNode = parserNode.expectObject()
        val id = objectNode.optional($$"$id")?.expect<String>()
        val schema = objectNode.optional($$"$schema")?.expect<String>()
        val comment = objectNode.optional($$"$comment")?.expect<String>()
        val title = objectNode.optional("title")?.expect<String>()
        val description = objectNode.optional("description")?.expect<String>()
        val type = objectNode.optional("type")?.let(::parseType)
        val format = objectNode.optional("format")?.expect<String>()

        val defaultNode = objectNode.optional("default")
        val default = defaultNode?.toPlainValue()
        val defaultSet = defaultNode != null

        val examples = objectNode.optional("examples")?.expect<List<Any?>>()

        val multipleOf = objectNode.optional("multipleOf")?.expect<Number>()
        val maximum = objectNode.optional("maximum")?.expect<Number>()
        val exclusiveMaximum = objectNode.optional("exclusiveMaximum")?.expect<Number>()
        val minimum = objectNode.optional("minimum")?.expect<Number>()
        val exclusiveMinimum = objectNode.optional("exclusiveMinimum")?.expect<Number>()

        val maxLength = objectNode.optional("maxLength")?.expect<Number>()
        val minLength = objectNode.optional("minLength")?.expect<Number>()
        val pattern = objectNode.optional("pattern")?.expect<String>()
        val contentEncoding = objectNode.optional("contentEncoding")?.expect<String>()
        val contentMediaType = objectNode.optional("contentMediaType")?.expect<String>()

        val itemsNode = objectNode.optional("items")
        val items = itemsNode?.takeUnless { it.node is DocumentArray }?.let(::parseElement)
        val tupleItems = itemsNode?.takeIf { it.node is DocumentArray }?.let(::parseList)
        val additionalItems = objectNode.optional("additionalItems")?.let { parseElement(it) }
        val maxItems = objectNode.optional("maxItems")?.expect<Number>()
        val minItems = objectNode.optional("minItems")?.expect<Number>()
        val uniqueItems = objectNode.optional("uniqueItems")?.expect<Boolean>()
        val contains = objectNode.optional("contains")?.let { parseElement(it) }

        val properties = objectNode.optional("properties")?.let(::parseMap)
        val patternProperties = objectNode.optional("patternProperties")?.let(::parseMap)
        val additionalProperties = objectNode.optional("additionalProperties")?.let(::parseElement)
        val propertyNames = objectNode.optional("propertyNames")?.let { parseElement(it) }

        val required = objectNode.optional("required")?.expect<List<String>>()

        val dependencies = objectNode.optional("dependencies")?.let(::parseDependencies)
        val definitions = objectNode.optional("definitions")?.let(::parseMap)
        val maxProperties = objectNode.optional("maxProperties")?.expect<Number>()
        val minProperties = objectNode.optional("minProperties")?.expect<Number>()

        val allOf = objectNode.optional("allOf")?.let { it.expectArray().elements().map(::parseElement) }
        val anyOf = objectNode.optional("anyOf")?.let { it.expectArray().elements().map(::parseElement) }
        val oneOf = objectNode.optional("oneOf")?.let { it.expectArray().elements().map(::parseElement) }

        val not = objectNode.optional("not")?.let(::parseElement)

        val ifSchema = objectNode.optional("if")?.let { parseElement(it) }
        val thenSchema = objectNode.optional("then")?.let { parseElement(it) }
        val elseSchema = objectNode.optional("else")?.let { parseElement(it) }

        val enumValues = objectNode.optional("enum")?.expect<List<Any?>>()
        val constNode = objectNode.optional("const")
        val constValue = constNode?.toPlainValue()
        val constSet = constNode != null

        val discriminator = objectNode.optional("discriminator")?.expect<String>()
        val externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement)
        val deprecated = objectNode.optional("deprecated")?.expect<Boolean>()
        val bindings = objectNode.optional("bindings")?.let { bindingParser.parseMap(it, SCHEMA_BINDING) }
        val extensions = objectNode
            .membersStartingWith("x-")
            .associateTo(linkedMapOf()) { extension ->
                extension.name to extension.toPlainValue()
            }
            .takeIf { it.isNotEmpty() }

        val readOnly = objectNode.optional("readOnly")?.expect<Boolean>()
        val writeOnly = objectNode.optional("writeOnly")?.expect<Boolean>()

        return SchemaInterface.SchemaInline(
            Schema(
                id = id,
                schema = schema,
                comment = comment,
                title = title,
                description = description,
                type = type,
                format = format,
                default = default,
                defaultSet = defaultSet,
                examples = examples,
                multipleOf = multipleOf,
                maximum = maximum,
                exclusiveMaximum = exclusiveMaximum,
                minimum = minimum,
                exclusiveMinimum = exclusiveMinimum,
                maxLength = maxLength,
                minLength = minLength,
                pattern = pattern,
                contentEncoding = contentEncoding,
                contentMediaType = contentMediaType,
                items = items,
                tupleItems = tupleItems,
                additionalItems = additionalItems,
                maxItems = maxItems,
                minItems = minItems,
                uniqueItems = uniqueItems,
                contains = contains,
                properties = properties,
                patternProperties = patternProperties,
                required = required,
                additionalProperties = additionalProperties,
                propertyNames = propertyNames,
                dependencies = dependencies,
                definitions = definitions,
                maxProperties = maxProperties,
                minProperties = minProperties,
                enum = enumValues,
                const = constValue,
                constSet = constSet,
                allOf = allOf,
                anyOf = anyOf,
                oneOf = oneOf,
                not = not,
                ifSchema = ifSchema,
                thenSchema = thenSchema,
                elseSchema = elseSchema,
                readOnly = readOnly,
                writeOnly = writeOnly,
                discriminator = discriminator,
                deprecated = deprecated,
                externalDocs = externalDocs,
                bindings = bindings,
                extensions = extensions,
            ).also { asyncApiContext.register(it, parserNode) }
        )
    }

    private fun parseType(parserNode: ParserNode): Any? =
        when (parserNode.node) {
            is DocumentNull -> null
            is DocumentArray -> parserNode.expect<List<String>>()
            else -> parserNode.expect<String>()
        }

    private fun parseDependencies(parserNode: ParserNode): Map<String, Any> {
        val objectNode = parserNode.expectObject()
        return objectNode.members().associate { dependency ->
            val parsedValue: Any = when (dependency.node) {
                is DocumentArray -> dependency.expect<List<String>>().also { asyncApiContext.register(it, dependency) }
                else -> parseElement(dependency)
            }
            dependency.name to parsedValue
        }
    }
}
