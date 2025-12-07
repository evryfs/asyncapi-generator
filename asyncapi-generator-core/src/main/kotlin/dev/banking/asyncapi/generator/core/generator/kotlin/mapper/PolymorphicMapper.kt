package dev.banking.asyncapi.generator.core.generator.kotlin.mapper

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.schemas.Schema

class PolymorphicMapper : TypeMapper {

    override fun map(schema: Schema, propertyName: String, root: KotlinTypeMapper): String? {
        if (schema.oneOf.isNullOrEmpty() && schema.anyOf.isNullOrEmpty()) {
            return null // This mapper only handles schemas with oneOf or anyOf.
        }

        // This is a polymorphic type. We infer its sealed interface name
        // from the property name itself (e.g., property "payment" -> interface "Payment").
        return MapperUtil.toPascalCase(propertyName)
    }
}
