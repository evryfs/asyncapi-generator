package dev.banking.asyncapi.generator.core.generator.context

import dev.banking.asyncapi.generator.core.model.schemas.Schema

class GeneratorContext(

    val schemas: Map<String, Schema>
) {

    fun findSchemaByName(name: String): Schema? = schemas[name]
}
