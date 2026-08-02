package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SchemaKeywordPolicyTest {

    @Test
    fun `all represented Draft 7 and AsyncAPI keywords are supported`() {
        val supportedKeywords = setOf(
            "\$id",
            "\$schema",
            "\$comment",
            "\$ref",
            "title",
            "description",
            "type",
            "format",
            "default",
            "examples",
            "multipleOf",
            "maximum",
            "exclusiveMaximum",
            "minimum",
            "exclusiveMinimum",
            "maxLength",
            "minLength",
            "pattern",
            "contentEncoding",
            "contentMediaType",
            "items",
            "additionalItems",
            "maxItems",
            "minItems",
            "uniqueItems",
            "contains",
            "maxProperties",
            "minProperties",
            "required",
            "properties",
            "patternProperties",
            "additionalProperties",
            "propertyNames",
            "dependencies",
            "definitions",
            "allOf",
            "anyOf",
            "oneOf",
            "not",
            "if",
            "then",
            "else",
            "enum",
            "const",
            "readOnly",
            "writeOnly",
            "discriminator",
            "deprecated",
            "externalDocs",
            "bindings",
        )

        supportedKeywords.forEach { keyword ->
            val classification = SchemaKeywordPolicy.classify(keyword)

            assertEquals(SchemaKeywordCategory.SUPPORTED, classification.category, keyword)
            assertNull(classification.severity, keyword)
        }
    }

    @Test
    fun `extensions annotations incompatible dialect keywords and typos have distinct policies`() {
        val extension = SchemaKeywordPolicy.classify("x-generator-note")
        val annotation = SchemaKeywordPolicy.classify("example")
        val openApiKeyword = SchemaKeywordPolicy.classify("nullable")
        val newerDialectKeyword = SchemaKeywordPolicy.classify("unevaluatedProperties")
        val typo = SchemaKeywordPolicy.classify("minLenght")

        assertEquals(SchemaKeywordCategory.PERMITTED_EXTENSION, extension.category)
        assertNull(extension.severity)
        assertEquals(SchemaKeywordCategory.IGNORED_ANNOTATION, annotation.category)
        assertEquals(WARNING, annotation.severity)
        assertEquals(SchemaKeywordCategory.RECOGNIZED_UNSUPPORTED, openApiKeyword.category)
        assertEquals(ERROR, openApiKeyword.severity)
        assertEquals(SchemaKeywordCategory.RECOGNIZED_UNSUPPORTED, newerDialectKeyword.category)
        assertEquals(ERROR, newerDialectKeyword.severity)
        assertEquals(SchemaKeywordCategory.UNKNOWN, typo.category)
        assertEquals(ERROR, typo.severity)
    }
}
