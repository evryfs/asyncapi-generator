package dev.banking.asyncapi.generator.core.parser.schemas

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SCHEMA
import dev.banking.asyncapi.generator.core.reader.DocumentArray
import dev.banking.asyncapi.generator.core.reader.DocumentBoolean
import dev.banking.asyncapi.generator.core.reader.DocumentNull
import dev.banking.asyncapi.generator.core.reader.DocumentObject
import dev.banking.asyncapi.generator.core.reader.DocumentString
import kotlin.String
import kotlin.collections.Map

/**
 * Parses AsyncAPI schema objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `SchemaParserTest`
 */
class SchemaParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val bindingParser = BindingParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val multiFormatParser = MultiFormatSchemaParser(asyncApiContext)
    private val nativeSchemaAssetReader = NativeSchemaAssetReader(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, SchemaInterface> = buildMap {
        parserNode.members().forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseList(parserNode: ParserNode): List<SchemaInterface> = buildList {
        parserNode.elements().forEach { node ->
            add(parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): SchemaInterface {
        if (isBooleanSchema(parserNode)) {
            val bool = parserNode.expect<Boolean>()
            return SchemaInterface.BooleanSchema(
                value = bool,
            ).also { asyncApiContext.register(it, parserNode) }
        }
        parserNode.optional($$"$ref")?.expect<String>()?.let { reference ->
            return SchemaInterface.SchemaReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = SCHEMA
                )
            ).also { asyncApiContext.register(it.reference, parserNode) }
        }
        parserNode.optional("schemaFormat")?.expect<String>()?.let { format ->
            val schemaFormat = multiFormatParser.parseFormat(format, parserNode.path)
            val schemaNode = parserNode.required("schema")
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


    fun parseSchema(parserNode: ParserNode): SchemaInterface {
        parserNode.optional($$"$ref")?.expect<String>()?.let { reference ->
            return SchemaInterface.SchemaReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = SCHEMA
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }

        val id = parserNode.optional($$"$id")?.expect<String>()
        val schema = parserNode.optional($$"$schema")?.expect<String>()
        val comment = parserNode.optional($$"$comment")?.expect<String>()
        val title = parserNode.optional("title")?.expect<String>()
        val description = parserNode.optional("description")?.expect<String>()
        var type = parserNode.optional("type")?.let(::parseType)
        val format = parserNode.optional("format")?.expect<String>()

        val defaultNode = parserNode.optional("default")
        val default = defaultNode?.toPlainValue()
        val defaultSet = defaultNode != null

        val examples = parserNode.optional("examples")?.expect<List<Any?>>()

        val multipleOf = parserNode.optional("multipleOf")?.expect<Number>()
        val maximum = parserNode.optional("maximum")?.expect<Number>()
        val exclusiveMaximum = parserNode.optional("exclusiveMaximum")?.expect<Number>()
        val minimum = parserNode.optional("minimum")?.expect<Number>()
        val exclusiveMinimum = parserNode.optional("exclusiveMinimum")?.expect<Number>()

        val maxLength = parserNode.optional("maxLength")?.expect<Number>()
        val minLength = parserNode.optional("minLength")?.expect<Number>()
        val pattern = parserNode.optional("pattern")?.expect<String>()
        val contentEncoding = parserNode.optional("contentEncoding")?.expect<String>()
        val contentMediaType = parserNode.optional("contentMediaType")?.expect<String>()

        val items = parserNode.optional("items")?.takeUnless { it.node is DocumentArray }?.let { parseElement(it) }
        val additionalItems = parserNode.optional("additionalItems")?.let { parseElement(it) }
        val maxItems = parserNode.optional("maxItems")?.expect<Number>()
        val minItems = parserNode.optional("minItems")?.expect<Number>()
        val uniqueItems = parserNode.optional("uniqueItems")?.expect<Boolean>()
        val contains = parserNode.optional("contains")?.let { parseElement(it) }

        val properties = parserNode.optional("properties")?.let(::parseMap)
        val patternProperties = parserNode.optional("patternProperties")?.let(::parseMap)
        val additionalProperties = parserNode.optional("additionalProperties")?.let(::parseElement)
        val propertyNames = parserNode.optional("propertyNames")?.let { parseElement(it) }

        val required = parserNode.optional("required")?.expect<List<String>>()

        val dependencies = parserNode.optional("dependencies")?.let(::parseDependencies)
        val definitions = parserNode.optional("definitions")?.let(::parseMap)
        val maxProperties = parserNode.optional("maxProperties")?.expect<Number>()
        val minProperties = parserNode.optional("minProperties")?.expect<Number>()

        val allOf = parserNode.optional("allOf")?.elements()?.map { parseElement(it) }
        val anyOf = parserNode.optional("anyOf")?.elements()?.map { parseElement(it) }
        val oneOf = parserNode.optional("oneOf")?.elements()?.map { parseElement(it) }

        val not = parserNode.optional("not")?.let(::parseElement)

        val ifSchema = parserNode.optional("if")?.let { parseElement(it) }
        val thenSchema = parserNode.optional("then")?.let { parseElement(it) }
        val elseSchema = parserNode.optional("else")?.let { parseElement(it) }

        val enumValues = parserNode.optional("enum")?.expect<List<Any?>>()
        val constValue = parserNode.optional("const")?.toPlainValue()

        val discriminator = parserNode.optional("discriminator")?.expect<String>()
        val externalDocs = parserNode.optional("externalDocs")?.let(externalDocsParser::parseElement)
        val deprecated = parserNode.optional("deprecated")?.expect<Boolean>()
        val bindings = parserNode.optional("bindings")?.let(bindingParser::parseMap)
        val extensions = parserNode.startsWith("x-")?.expect<Map<String, Any?>>()

        val readOnly = parserNode.optional("readOnly")?.expect<Boolean>()
        val writeOnly = parserNode.optional("writeOnly")?.expect<Boolean>()

        if (type == null) {
            if (enumValues != null && !enumValues.isEmpty())
                type = "string"
        }
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

    private fun isBooleanSchema(node: ParserNode): Boolean {
        val value = node.node
        return value is DocumentBoolean ||
                (value is DocumentString && (value.value.equals("true", ignoreCase = true) || value.value.equals(
                    "false",
                    ignoreCase = true
                )))
    }

    private fun parseDependencies(parserNode: ParserNode): Map<String, Any> {
        return parserNode.members().associate { dependency ->
            val dependencyValue = dependency.node
            val parsedValue: Any = when (dependencyValue) {
                is DocumentArray -> dependency.expect<List<String>>()
                is DocumentObject, is DocumentBoolean -> parseElement(dependency)
                else -> dependency.expect<Map<String, Any?>>()
            }
            dependency.name to parsedValue
        }
    }
}
