package dev.banking.asyncapi.generator.core.model.schemas

import com.fasterxml.jackson.annotation.JsonProperty
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface

data class Schema(
    val id: String? = null, // is $id in JSON/Yaml Schema
    val schema: String? = null, // is $schema in JSON/Yaml Schema
    val comment: String? = null, // is $comment in JSON/Yaml Schema
    val title: String? = null,
    val description: String? = null,
    val type: Any? = null, // so sad - https://www.learnjsonschema.com/draft7/validation/type/ - can be string or array of strings
    val format: String? = null,
    val default: Any? = null,
    val examples: List<Any?>? = null,

    val multipleOf: Number? = null,
    val maximum: Number? = null,
    val exclusiveMaximum: Number? = null,
    val minimum: Number? = null,
    val exclusiveMinimum: Number? = null,

    val maxLength: Number? = null,
    val minLength: Number? = null,
    val pattern: String? = null,
    val contentEncoding: String? = null,
    val contentMediaType: String? = null,

    val items: SchemaInterface? = null,
    val additionalItems: SchemaInterface? = null,
    val maxItems: Number? = null,
    val minItems: Number? = null,
    val uniqueItems: Boolean? = null,
    val contains: SchemaInterface? = null,

    val maxProperties: Number? = null,
    val minProperties: Number? = null,
    val required: List<String>? = null,
    val properties: Map<String, SchemaInterface>? = null,
    val patternProperties: Map<String, SchemaInterface>? = null,
    val additionalProperties: SchemaInterface? = null,
    val propertyNames: SchemaInterface? = null,
    val dependencies: Map<String, Any>? = null,
    val definitions: Map<String, SchemaInterface>? = null,

    val allOf: List<SchemaInterface>? = null,
    val anyOf: List<SchemaInterface>? = null,
    val oneOf: List<SchemaInterface>? = null,
    val not: SchemaInterface? = null,
    @get:JsonProperty("if") val ifSchema: SchemaInterface? = null,
    @get:JsonProperty("then") val thenSchema: SchemaInterface? = null,
    @get:JsonProperty("else") val elseSchema: SchemaInterface? = null,

    val enum: List<Any?>? = null,
    val const: Any? = null,
    val nullable: Boolean? = null,
    val readOnly: Boolean? = null,
    val writeOnly: Boolean? = null,

    val discriminator: String? = null,
    val deprecated: Boolean? = null,
    val externalDocs: ExternalDocInterface? = null,
    val bindings: Map<String, BindingInterface>? = null,
)
