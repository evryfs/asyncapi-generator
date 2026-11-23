package dev.banking.asyncapi.generator.core.generator.schema

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.references.Reference

/**
 * The name of the generated model for the schema this reference points to: the
 * last segment of its pointer in PascalCase, as every generated model is named.
 */
internal fun Reference.generatedSchemaName(): String = MapperUtil.toPascalCase(ref.substringAfterLast('/'))
