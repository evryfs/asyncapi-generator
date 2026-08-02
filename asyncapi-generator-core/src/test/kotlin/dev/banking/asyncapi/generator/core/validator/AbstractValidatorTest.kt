package dev.banking.asyncapi.generator.core.validator

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.ValidatorFixtures
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationReport
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity
import dev.banking.asyncapi.generator.core.validator.util.ValidationReporter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.fail

abstract class AbstractValidatorTest {

    protected val asyncApiContext = AsyncApiContext()
    private val validatorFixtures = ValidatorFixtures(asyncApiContext)

    /**
     * Reads and parses any YAML fixture path.
     *
     * Paths can be relative to test resources, or use the legacy
     * `src/test/resources/...` prefix.
     */
    protected fun parse(path: String): AsyncApiDocument {
        return validatorFixtures.document(path)
    }

    protected fun validate(path: String): ValidationReport {
        return validatorFixtures.validate(path)
    }

    protected fun throwErrors(report: ValidationReport) {
        ValidationReporter(asyncApiContext).throwErrors(report)
    }

    protected fun logWarnings(report: ValidationReport) {
        ValidationReporter(asyncApiContext).logWarnings(report)
    }

    protected fun assertNoFindings(results: ValidationReport) {
        assertFalse(results.hasErrors(), "Found validation errors: ${results.errors}")
        assertFalse(results.hasWarnings(), "Found validation warnings: ${results.warnings}")
        assertEquals(emptyList(), results.findings, "Found validation findings: ${results.findings}")
    }

    protected fun assertRule(
        results: ValidationReport,
        rule: ValidationRule,
        sourceFile: String? = null,
        path: String? = null,
        line: Int? = null,
        messageContains: String? = null,
    ): ValidationFinding {
        val finding = results.findings.singleOrNull { finding ->
            finding.code == rule.code &&
                finding.concern == rule.concern &&
                finding.severity == rule.severity &&
                (messageContains == null || finding.message.contains(messageContains)) &&
                (sourceFile == null || finding.sourceLocation?.file?.name == sourceFile) &&
                (path == null || finding.path == path) &&
                (line == null || finding.line == line)
        } ?: fail(
            "Expected one ${rule.code} finding, " +
                "but found: ${results.findings}"
        )

        if (sourceFile != null) {
            assertEquals(sourceFile, finding.sourceLocation?.file?.name)
        }
        if (path != null) {
            assertEquals(path, finding.path)
        }
        if (line != null) {
            assertEquals(line, finding.line)
        }
        return finding
    }

    protected fun assertFinding(
        results: ValidationReport,
        severity: ValidationSeverity,
        messageContains: String,
        sourceFile: String? = null,
        path: String? = null,
        line: Int? = null,
    ): ValidationFinding {
        val finding = results.findings.singleOrNull { finding ->
            finding.severity == severity &&
                finding.message.contains(messageContains) &&
                (sourceFile == null || finding.sourceLocation?.file?.name == sourceFile) &&
                (path == null || finding.path == path) &&
                (line == null || finding.line == line)
        } ?: fail(
            "Expected one $severity finding containing '$messageContains', " +
                "but found: ${results.findings}"
        )

        return finding
    }
}
