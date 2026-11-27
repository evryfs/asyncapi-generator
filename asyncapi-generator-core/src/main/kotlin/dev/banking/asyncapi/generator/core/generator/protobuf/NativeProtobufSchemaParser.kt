package dev.banking.asyncapi.generator.core.generator.protobuf

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidNativeProtobufSchema
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema

/**
 * Extracts the Protobuf declarations required by native Protobuf generators.
 *
 * The generator does not compile `.proto` files directly, but it needs stable
 * access to the package, Java package, Java file mode, and top-level message
 * declarations before generated Kafka APIs can reference Protobuf types.
 * Declarations are read however they are spread over lines, as `protoc` reads them.
 */
internal class NativeProtobufSchemaParser {
    fun parse(
        payloadName: String,
        schema: MultiFormatSchema,
    ): NativeProtobufSchema {
        val content = schemaContent(payloadName, schema)
        val statements = topLevelStatements(content)

        return NativeProtobufSchema(
            content = content,
            protoPackageName = statements.declared("package"),
            javaPackageName = statements.option("java_package")?.let(::stringValue),
            javaMultipleFiles = statements.option("java_multiple_files")?.toBooleanStrictOrNull(),
            messageNames = statements.filter { it.opensBlock && it.isDeclarationOf("message") }.map { it.tokens[1] },
        )
    }

    /**
     * Splits [content] into its top-level statements, those outside every `{ }` block. Comments are
     * skipped and string literals are kept whole, so braces inside them do not count. A statement
     * ends at `;`, or at the `{` that opens its block.
     */
    private fun topLevelStatements(content: String): List<Statement> =
        buildList {
            val tokens = mutableListOf<String>()
            var depth = 0
            for (token in TOKEN_REGEX.findAll(content).map(MatchResult::value).filterNot(::isComment)) {
                when {
                    token == "{" -> {
                        if (depth == 0) add(Statement(tokens.toList(), opensBlock = true))
                        tokens.clear()
                        depth++
                    }
                    token == "}" -> depth = (depth - 1).coerceAtLeast(0)
                    depth > 0 -> Unit
                    token == ";" -> {
                        add(Statement(tokens.toList(), opensBlock = false))
                        tokens.clear()
                    }
                    else -> tokens += token
                }
            }
        }

    /** The name in the first `[keyword] name;` statement, such as the `package`. */
    private fun List<Statement>.declared(keyword: String): String? =
        firstOrNull { !it.opensBlock && it.isDeclarationOf(keyword) }?.tokens?.get(1)

    /** The value of the first `option [name] = value;` statement, as written. */
    private fun List<Statement>.option(name: String): String? =
        firstOrNull { statement ->
            !statement.opensBlock && statement.tokens.size == OPTION_TOKENS &&
                statement.tokens[0] == "option" && statement.tokens[1] == name && statement.tokens[2] == "="
        }?.tokens?.get(OPTION_TOKENS - 1)

    private fun isComment(token: String): Boolean = token.startsWith("//") || token.startsWith("/*")

    /** The text of a string literal token without its quotes; null for any other token. */
    private fun stringValue(token: String): String? =
        token.takeIf { it.length >= 2 && it.first() in "\"'" }?.substring(1, token.length - 1)

    private fun schemaContent(
        payloadName: String,
        schema: MultiFormatSchema,
    ): String {
        val content =
            schema.schema as? String
                ?: throw InvalidNativeProtobufSchema(
                    payloadName = payloadName,
                    schemaFormat = schema.schemaFormat,
                    reason = "Native Protobuf schemas must be provided as .proto text.",
                )

        if (content.isBlank()) {
            throw InvalidNativeProtobufSchema(
                payloadName = payloadName,
                schemaFormat = schema.schemaFormat,
                reason = "Native Protobuf schema content cannot be blank.",
            )
        }

        return content
    }

    /**
     * One top-level statement: its tokens up to `;`, or up to the `{` of the block it opens.
     *
     * @param tokens the identifiers, string literals and `=` signs of the statement
     * @param opensBlock whether the statement opens a block, as `message Name {` does
     */
    private data class Statement(
        val tokens: List<String>,
        val opensBlock: Boolean,
    ) {
        /** True for `[keyword] name`, such as `message Order` or `package com.example`. */
        fun isDeclarationOf(keyword: String): Boolean = tokens.size == 2 && tokens[0] == keyword
    }

    private companion object {
        const val OPTION_TOKENS = 4
        private const val IDENTIFIER = """[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*"""
        val TOKEN_REGEX =
            Regex(
                """"(?:[^"\\\n]|\\.)*"|'(?:[^'\\\n]|\\.)*'|//[^\n]*|/\*[\s\S]*?\*/|$IDENTIFIER|[{};=]""",
            )
    }
}

/**
 * The declarations of a `.proto` file that generation needs.
 *
 * @property content the file content as written
 * @property protoPackageName the `package` declaration; null without one
 * @property javaPackageName the `java_package` option; null without one
 * @property javaMultipleFiles the `java_multiple_files` option; null when not set
 * @property messageNames the top-level message names, in file order
 */
internal data class NativeProtobufSchema(
    val content: String,
    val protoPackageName: String?,
    val javaPackageName: String?,
    val javaMultipleFiles: Boolean?,
    val messageNames: List<String>,
)
