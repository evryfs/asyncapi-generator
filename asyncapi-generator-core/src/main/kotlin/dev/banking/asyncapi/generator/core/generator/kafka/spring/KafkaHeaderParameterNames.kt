package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidKafkaHeaderName
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.KafkaHeaderParameterNameCollision

/** Converts Kafka wire-header names into source identifiers shared by Java and Kotlin output. */
internal object KafkaHeaderParameterNames {
    private val invalidIdentifierCharacters = Regex("[^A-Za-z0-9_]+")

    private val reservedIdentifiers =
        setOf(
            "_",
            "abstract",
            "actual",
            "annotation",
            "as",
            "assert",
            "boolean",
            "break",
            "by",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "companion",
            "const",
            "constructor",
            "continue",
            "crossinline",
            "data",
            "default",
            "delegate",
            "do",
            "double",
            "dynamic",
            "else",
            "enum",
            "expect",
            "extends",
            "external",
            "false",
            "field",
            "final",
            "finally",
            "float",
            "for",
            "fun",
            "get",
            "goto",
            "if",
            "implements",
            "import",
            "in",
            "infix",
            "init",
            "inline",
            "inner",
            "instanceof",
            "int",
            "interface",
            "internal",
            "is",
            "lateinit",
            "long",
            "native",
            "new",
            "noinline",
            "null",
            "object",
            "open",
            "operator",
            "out",
            "override",
            "package",
            "param",
            "permits",
            "private",
            "property",
            "protected",
            "public",
            "receiver",
            "record",
            "reified",
            "return",
            "sealed",
            "set",
            "setparam",
            "short",
            "static",
            "strictfp",
            "super",
            "suspend",
            "switch",
            "synchronized",
            "tailrec",
            "this",
            "throw",
            "throws",
            "transient",
            "true",
            "try",
            "typealias",
            "typeof",
            "val",
            "var",
            "vararg",
            "void",
            "volatile",
            "when",
            "where",
            "while",
            "yield",
        )

    fun resolve(
        headerContractName: String,
        wireNames: Collection<String>,
    ): Map<String, String> {
        val parameterNames =
            wireNames.associateWith { wireName ->
                normalize(
                    headerContractName = headerContractName,
                    wireName = wireName,
                )
            }

        parameterNames
            .entries
            .groupBy(
                keySelector = Map.Entry<String, String>::value,
                valueTransform = Map.Entry<String, String>::key,
            ).entries
            .firstOrNull { (_, matchingWireNames) -> matchingWireNames.size > 1 }
            ?.let { (parameterName, matchingWireNames) ->
                throw KafkaHeaderParameterNameCollision(
                    headerContractName = headerContractName,
                    wireNames = matchingWireNames,
                    parameterName = parameterName,
                )
            }

        return parameterNames
    }

    private fun normalize(
        headerContractName: String,
        wireName: String,
    ): String {
        if (wireName.isBlank()) {
            throw InvalidKafkaHeaderName(
                headerContractName = headerContractName,
                wireName = wireName,
                reason = "The wire-header name is blank.",
            )
        }

        val normalized = wireName.replace(invalidIdentifierCharacters, "_")
        if (normalized.all { character -> character == '_' }) {
            throw InvalidKafkaHeaderName(
                headerContractName = headerContractName,
                wireName = wireName,
                reason = "The wire-header name does not contain an ASCII letter or digit.",
            )
        }

        val identifier =
            if (normalized.first().isDigit()) {
                "_$normalized"
            } else {
                normalized
            }

        return if (identifier in reservedIdentifiers) {
            "${identifier}_"
        } else {
            identifier
        }
    }
}
