package dev.banking.asyncapi.generator.core.validator

import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.validator.ValidationReport

/**
 * Contract for validator-stage implementations.
 *
 * A validation stage receives an already parsed AsyncAPI model and returns
 * validation results. It must not read input files, parse documents, bundle
 * references, or generate output.
 *
 * Expected behavior is covered by:
 * - `AsyncApiValidatorTest`
 */
interface ValidationStage {
    fun validate(asyncApiDocument: AsyncApiDocument): ValidationReport
}
