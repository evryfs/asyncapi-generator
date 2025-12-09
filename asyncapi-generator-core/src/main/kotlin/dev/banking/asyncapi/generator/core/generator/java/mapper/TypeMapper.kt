package dev.banking.asyncapi.generator.core.generator.java.mapper

import dev.banking.asyncapi.generator.core.model.schemas.Schema

interface TypeMapper {

    fun map(schema: Schema, propertyName: String, root: JavaTypeMapper): String?
}
