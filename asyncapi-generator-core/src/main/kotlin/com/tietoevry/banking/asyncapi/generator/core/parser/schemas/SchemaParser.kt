package com.tietoevry.banking.asyncapi.generator.core.parser.schemas

import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import com.tietoevry.banking.asyncapi.generator.core.parser.bindings.BindingParser
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException.UnexpectedValue
import kotlin.String
import kotlin.collections.Map

class SchemaParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val bindingParser = BindingParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val multiFormatParser = MultiFormatSchemaParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, SchemaInterface> = buildMap {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseList(parserNode: ParserNode): List<SchemaInterface> = buildList {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            add(parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): SchemaInterface {
        parserNode.optional($$"$ref")?.coerce<String>()?.let { reference ->
            return SchemaInterface.SchemaReference(
                Reference(
                    ref = reference,
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }
        parserNode.optional("schemaFormat")?.coerce<String>()?.let { format ->
            multiFormatParser.validateFormat(format, parserNode.path)
            return parseElement(parserNode.mandatory("schema"))
        }
        if (isBooleanSchema(parserNode)) {
            val bool = parserNode.coerce<Boolean>()
            return SchemaInterface.BooleanSchema(
                value = bool,
            ).also { asyncApiContext.register(it, parserNode) }
        }
        return parseSchema(parserNode)
    }


    fun parseSchema(parserNode: ParserNode): SchemaInterface {
        parserNode.optional($$"$ref")?.coerce<String>()?.let { reference ->
            return SchemaInterface.SchemaReference(
                Reference(
                    ref = reference,
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }

        val id = parserNode.optional($$"$id")?.coerce<String>()
        val schema = parserNode.optional($$"$schema")?.coerce<String>()
        val comment = parserNode.optional($$"$comment")?.coerce<String>()
        val title = parserNode.optional("title")?.coerce<String>()
        val description = parserNode.optional("description")?.coerce<String>()
        val type = parserNode.optional("type")?.coerce<Any>()
        val format = parserNode.optional("format")?.coerce<String>()
        val default = parserNode.optional("default")?.coerce<Any>()
        val examples = parserNode.optional("examples")?.coerce<List<Any?>>()

        val multipleOf = parserNode.optional("multipleOf")?.coerce<Number>()
        val maximum = parserNode.optional("maximum")?.coerce<Number>()
        val exclusiveMaximum = parserNode.optional("exclusiveMaximum")?.coerce<Number>()
        val minimum = parserNode.optional("minimum")?.coerce<Number>()
        val exclusiveMinimum = parserNode.optional("exclusiveMinimum")?.coerce<Number>()

        val maxLength = parserNode.optional("maxLength")?.coerce<Number>()
        val minLength = parserNode.optional("minLength")?.coerce<Number>()
        val pattern = parserNode.optional("pattern")?.coerce<String>()
        val contentEncoding = parserNode.optional("contentEncoding")?.coerce<String>()
        val contentMediaType = parserNode.optional("contentMediaType")?.coerce<String>()

        val items = parserNode.optional("items")?.let { parseElement(it) }
        val additionalItems = parserNode.optional("additionalItems")?.let { parseElement(it) }
        val maxItems = parserNode.optional("maxItems")?.coerce<Number>()
        val minItems = parserNode.optional("minItems")?.coerce<Number>()
        val uniqueItems = parserNode.optional("uniqueItems")?.coerce<Boolean>()
        val contains = parserNode.optional("contains")?.let { parseElement(it) }

        val properties = parserNode.optional("properties")?.let(::parseMap)
        val patternProperties = parserNode.optional("patternProperties")?.let(::parseMap)
        val additionalProperties = parserNode.optional("additionalProperties")?.let(::parseElement)
        val propertyNames = parserNode.optional("propertyNames")?.let { parseElement(it) }

        val required = parserNode.optional("required")?.coerce<List<String>>()

        val dependencies = parserNode.optional("dependencies")?.let(::parseDependencies)
        val definitions = parserNode.optional("definitions")?.let(::parseMap)
        val maxProperties = parserNode.optional("maxProperties")?.coerce<Number>()
        val minProperties = parserNode.optional("minProperties")?.coerce<Number>()

        val allOf = parserNode.optional("allOf")?.extractNodes()?.map { parseElement(it) }
        val anyOf = parserNode.optional("anyOf")?.extractNodes()?.map { parseElement(it) }
        val oneOf = parserNode.optional("oneOf")?.extractNodes()?.map { parseElement(it) }

        val not = parserNode.optional("not")?.let(::parseElement)

        val ifSchema = parserNode.optional("if")?.let { parseElement(it) }
        val thenSchema = parserNode.optional("then")?.let { parseElement(it) }
        val elseSchema = parserNode.optional("else")?.let { parseElement(it) }

        val enumValues = parserNode.optional("enum")?.coerce<List<Any?>>()
        val constValue = parserNode.optional("const")?.coerce<Any?>()

        val discriminator = parserNode.optional("discriminator")?.coerce<String>()
        val externalDocs = parserNode.optional("externalDocs")?.let(externalDocsParser::parseElement)
        val deprecated = parserNode.optional("deprecated")?.coerce<Boolean>()
        val bindings = parserNode.optional("bindings")?.let(bindingParser::parseMap)

        val nullable = parserNode.optional("nullable")?.coerce<Boolean>()
        val readOnly = parserNode.optional("readOnly")?.coerce<Boolean>()
        val writeOnly = parserNode.optional("writeOnly")?.coerce<Boolean>()

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
                nullable = nullable,
                readOnly = readOnly,
                writeOnly = writeOnly,
                discriminator = discriminator,
                deprecated = deprecated,
                externalDocs = externalDocs,
                bindings = bindings
            ).also { asyncApiContext.register(it, parserNode) }
        )
    }

    private fun isBooleanSchema(node: ParserNode): Boolean {
        val value = node.node
        return value is Boolean ||
            (value is String && (value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true)))
    }

    private fun parseDependencies(parserNode: ParserNode): Map<String, Any> {
        parserNode.coerce<Map<*, *>>()
        val nodes = parserNode.extractNodes()
        return nodes.associate { dependency ->
            val dependencyValue = dependency.node
            val parsedValue: Any = when (dependencyValue) {
                is List<*> -> dependency.coerce<List<String>>()
                is Map<*, *> -> parseElement(dependency)
                else -> throw UnexpectedValue(javaClass.simpleName, "Map/List", dependency.path, asyncApiContext)
            }
            dependency.name to parsedValue
        }
    }
}
