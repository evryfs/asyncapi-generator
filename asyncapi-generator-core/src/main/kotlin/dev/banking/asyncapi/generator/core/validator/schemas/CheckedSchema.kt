package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath

/**
 * A Schema Object together with where it sits and where its findings go.
 *
 * @property node the Schema Object being checked
 * @property path the schema as findings name it
 * @property results the collector receiving the findings
 */
internal class CheckedSchema(
    val node: Schema,
    val path: ValidationPath,
    val results: ValidationCollector,
)
