package dev.banking.asyncapi.generator.core.generator.schema

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * The name of the generated model for the schema this reference points to: the
 * last segment of its pointer in PascalCase, as every generated model is named.
 */
internal fun Reference.generatedSchemaName(): String = MapperUtil.toPascalCase(ref.substringAfterLast('/'))

/**
 * The schema this reference is linked to, or `null` when it is linked to
 * something else. A reference to another schema reference is not followed.
 */
internal fun SchemaInterface.SchemaReference.linkedSchema(): Schema? = reference.model as? Schema
