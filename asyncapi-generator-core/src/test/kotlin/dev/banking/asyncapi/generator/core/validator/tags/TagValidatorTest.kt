package dev.banking.asyncapi.generator.core.validator.tags

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.TAG_NAME_REQUIRED
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TagValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid tags trigger errors only`() {
        val document = parse("validator/tags/asyncapi_validator_tag_invalid.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(1, results.errors.size)
        val finding = results.findings.single()
        assertEquals(TAG_NAME_REQUIRED.code, finding.code)
        assertEquals(TAG_NAME_REQUIRED.severity, finding.severity)
        assertEquals(TAG_NAME_REQUIRED.concern, finding.concern)
        assertEquals("asyncapi_validator_tag_invalid.yaml", finding.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_tag_invalid.root.components.tags.InvalidTag.name",
            finding.path,
        )
        assertEquals(9, finding.line)
    }
}
