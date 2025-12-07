package com.tietoevry.banking.asyncapi.generator.core.generator.context

import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema

/**
 * An immutable context object that holds all shared information for a single
 * code generation run.
 *
 * It acts as a single, reliable source of truth for all processors and factories,
 * removing the need to pass large maps of schemas around and making dependencies explicit.
 */
class GeneratorContext(
    /**
     * The final, fully processed map of all schemas, where the key is the
     * schema name and the value is the complete Schema object.
     */
    val schemas: Map<String, Schema>
) {
    /**
     * Safely finds a schema by its name.
     */
    fun findSchemaByName(name: String): Schema? = schemas[name]
}
