package com.tietoevry.banking.asyncapi.generator.core.parser.schemas

import com.tietoevry.banking.asyncapi.generator.core.model.bindings.Binding
import com.tietoevry.banking.asyncapi.generator.core.model.bindings.BindingInterface
import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface

fun lightMeasuredPayload() = Schema(
    type = "object",
    required = listOf("lumens"),
    examples = listOf(
        mapOf("lumens" to 1500, "sentAt" to "'2024-09-12T12:00:00Z"),
        mapOf("lumens" to 800, "sentAt" to "'2024-09-12T18:30:00Z")
    ),
    properties = mapOf(
        "lumens" to SchemaInterface.SchemaInline(
            Schema(
                description = "Light intensity measured in lumens.",
                type = "integer",
                minimum = 0
            )
        ),
        "sentAt" to SchemaInterface.SchemaReference(
            Reference("'#/components/schemas/sentAt")
        )
    )
)

fun turnOnOffPayload() = Schema(
    type = "object",
    required = listOf("command", "myDescription"),
    properties = mapOf(
        "command" to SchemaInterface.SchemaInline(
            Schema(
                description = "Whether to turn on or off the light.",
                type = "string",
                enum = listOf("'on", "'off")
            )
        ),
        "sentAt" to SchemaInterface.SchemaReference(
            Reference("'#/components/schemas/sentAt")
        )
    )
)

fun dimLightPayload() = Schema(
    type = "object",
    deprecated = true,
    properties = mapOf(
        "percentage" to SchemaInterface.SchemaInline(
            Schema(
                description = "Percentage to which the light should be dimmed to.",
                type = "integer",
                minimum = 0,
                maximum = 100
            )
        ),
        "sentAt" to SchemaInterface.SchemaReference(
            Reference("'#/components/schemas/sentAt")
        )
    )
)

fun sentAt() = Schema(
    description = "Date and time when the message was sent.",
    type = "string",
    format = "date-time"
)

fun commandPayload() = Schema(
    oneOf = listOf(
        SchemaInterface.SchemaReference(Reference("'#/components/schemas/turnOnOffPayload")),
        SchemaInterface.SchemaReference(Reference("'#/components/schemas/dimLightPayload"))
    )
)

fun simpleString() = Schema(
    title = "Simple String Example",
    description = "A short string with constraints",
    type = "string",
    format = "uuid",
    default = "\"abc123",
    examples = listOf("\"abc123", "\"def456"),
    pattern = "\"^[a-zA-Z0-9_-]+$",
    enum = listOf("\"abc123", "\"def456"),
    const = "\"abc123",
    maxLength = 36,
    minLength = 3
)

fun simpleNumber() = Schema(
    title = "Simple Number Example",
    description = "A number with range and divisibility",
    type = "number",
    default = 2.5,
    examples = listOf(0.5, 2, 9.5),
    multipleOf = 0.5,
    exclusiveMaximum = 10,
    exclusiveMinimum = 0
)

fun numberArray() = Schema(
    title = "Array of numbers",
    type = "array",
    examples = listOf(listOf(1, 2, 3)),
    items = SchemaInterface.SchemaInline(Schema(type = "number", minimum = 0)),
    contains = SchemaInterface.SchemaInline(Schema(minimum = 3)),
    uniqueItems = true,
    maxItems = 5,
    minItems = 1
)

fun complexObject() = Schema(
    title = "Complex Object Example",
    description = "Object demonstrating properties and dependencies",
    type = "object",
    examples = listOf(mapOf("name" to "\"John", "age" to 30, "email" to "\"john@example.com")),
    required = listOf("name", "age"),
    properties = mapOf(
        "name" to SchemaInterface.SchemaInline(Schema(type = "string", minLength = 1)),
        "age" to SchemaInterface.SchemaInline(Schema(type = "integer", default = 18, minimum = 0)),
        "email" to SchemaInterface.SchemaInline(Schema(type = "string", format = "email", readOnly = true)),
        "password" to SchemaInterface.SchemaInline(Schema(type = "string", writeOnly = true)),
        "nickname" to SchemaInterface.SchemaInline(Schema(type = "string", nullable = true))
    ),
    dependencies = mapOf("password" to listOf("email")),
    propertyNames = SchemaInterface.SchemaInline(Schema(pattern = "\"^[a-z]+$")),
    additionalProperties = SchemaInterface.BooleanSchema(value = false),
    maxProperties = 5,
    minProperties = 2
)

fun composedSchema() = Schema(
    title = "Schema Composition Example",
    description = "Example demonstrating allOf, anyOf, oneOf, and not",
    examples = listOf("\"some value"),
    allOf = listOf(
        SchemaInterface.SchemaReference(Reference("'#/components/schemas/simpleString")),
        SchemaInterface.SchemaReference(Reference("'#/components/schemas/simpleNumber"))
    ),
    anyOf = listOf(
        SchemaInterface.SchemaReference(Reference("'#/components/schemas/simpleString")),
        SchemaInterface.SchemaInline(Schema(type = "boolean"))
    ),
    oneOf = listOf(
        SchemaInterface.SchemaReference(Reference("'#/components/schemas/simpleNumber")),
        SchemaInterface.SchemaInline(Schema(type = "boolean"))
    ),
    not = SchemaInterface.SchemaInline(Schema(type = "null"))
)


fun conditionalExample() = Schema(
    title = "Conditional Schema",
    type = "object",
    examples = listOf(mapOf("type" to "\"car", "wheels" to 4)),
    properties = mapOf(
        "type" to SchemaInterface.SchemaInline(Schema(type = "string")),
        "wheels" to SchemaInterface.SchemaInline(Schema(type = "integer")),
        "legs" to SchemaInterface.SchemaInline(Schema(type = "integer"))
    ),
    ifSchema = SchemaInterface.SchemaInline(
        Schema(
            properties = mapOf(
                "type" to SchemaInterface.SchemaInline(Schema(const = "\"car"))
            )
        )
    ),
    thenSchema = SchemaInterface.SchemaInline(Schema(required = listOf("wheels"))),
    elseSchema = SchemaInterface.SchemaInline(Schema(required = listOf("legs")))
)

fun asyncApiSpecific() = Schema(
    title = "AsyncAPI Custom Schema",
    description = "Demonstrates AsyncAPI-only fields",
    type = "object",
    required = listOf("type"),
    examples = listOf(mapOf("type" to "\"event", "data" to mapOf("id" to "\"123"))),
    properties = mapOf(
        "type" to SchemaInterface.SchemaInline(Schema(type = "string")),
        "data" to SchemaInterface.SchemaInline(Schema(type = "object"))
    ),
    discriminator = "\"type",
    deprecated = true,
    externalDocs = ExternalDocInterface.ExternalDocInline(
        ExternalDoc(
            url = "\"https://example.com/docs/schema",
            description = "\"External reference documentation"
        )
    ),
    bindings = mapOf(
        "kafka" to BindingInterface.BindingInline(
            Binding(
                content = mapOf("topic" to "\"my-topic")
            )
        )

    )

)

fun referencedSchema() = Reference("'#/components/schemas/simpleString")

fun allowAnything() = SchemaInterface.BooleanSchema(value = true)

fun allowNothing() = SchemaInterface.BooleanSchema(value = false)

fun simpleObject() = Schema(
    comment = "\"This schema defines the minimal user object used across APIs.",
    type = "object",
    required = listOf("id"),
    properties = mapOf(
        "id" to SchemaInterface.SchemaInline(
            Schema(
                type = "string",
                description = "Unique identifier"
            )
        ),
        "name" to SchemaInterface.SchemaInline(
            Schema(
                type = "string"
            )
        ),
        "active" to SchemaInterface.SchemaInline(
            Schema(
                type = "boolean",
                default = true
            )
        )
    )
)

fun nestedObject() = Schema(
    type = "object",
    properties = mapOf(
        "user" to SchemaInterface.SchemaInline(
            Schema(
                type = "object",
                properties = mapOf(
                    "id" to SchemaInterface.SchemaInline(
                        Schema(
                            type = "string"
                        )
                    ),
                    "profile" to SchemaInterface.SchemaInline(
                        Schema(
                            type = "object",
                            properties = mapOf(
                                "age" to SchemaInterface.SchemaInline(
                                    Schema(
                                        type = "integer"
                                    )
                                ),
                                "city" to SchemaInterface.SchemaInline(
                                    Schema(
                                        type = "string"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )
)

fun arrayOfObjects() = Schema(
    type = "array",
    description = "\"A list of user score objects",
    items = SchemaInterface.SchemaInline(
        Schema(
            type = "object",
            required = listOf("id", "score"),
            properties = mapOf(
                "id" to SchemaInterface.SchemaInline(
                    Schema(
                        type = "string",
                        description = "\"Unique user identifier"
                    )
                ),
                "score" to SchemaInterface.SchemaInline(
                    Schema(
                        type = "number",
                        description = "\"Score achieved by the user",
                        minimum = 0,
                        maximum = 100
                    )
                ),
                "metadata" to SchemaInterface.SchemaInline(
                    Schema(
                        type = "object",
                        properties = mapOf(
                            "timestamp" to SchemaInterface.SchemaInline(
                                Schema(
                                    type = "string",
                                    format = "date-time",
                                    description = "\"When the score was recorded"
                                )
                            ),
                            "tags" to SchemaInterface.SchemaInline(
                                Schema(
                                    type = "array",
                                    items = SchemaInterface.SchemaInline(
                                        Schema(
                                            type = "string",
                                            description = "\"Optional tags for score context"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )
)

fun enumAndConst() = Schema(
    type = "string",
    description = "\"Primary color selection",
    enum = listOf("red", "green", "blue"),
    const = "red",
    default = "red",
    pattern = "\"^(red|green|blue)$",
    examples = listOf("red", "green"),
    deprecated = false,
    readOnly = false,
    writeOnly = false
)

fun combined() = Schema(
    allOf = listOf(
        SchemaInterface.SchemaInline(
            Schema(
                type = "object",
                properties = mapOf(
                    "id" to SchemaInterface.SchemaInline(
                        Schema(type = "string")
                    )
                )
            )
        ),
        SchemaInterface.SchemaInline(
            Schema(
                type = "object",
                properties = mapOf(
                    "name" to SchemaInterface.SchemaInline(
                        Schema(type = "string")
                    )
                )
            )
        )
    ),
    anyOf = listOf(
        SchemaInterface.SchemaInline(Schema(type = "string")),
        SchemaInterface.SchemaInline(Schema(type = "number"))
    ),
    oneOf = listOf(
        SchemaInterface.SchemaInline(Schema(const = true)),
        SchemaInterface.SchemaInline(Schema(const = false))
    )
)

fun conditional() = Schema(
    type = "object",
    properties = mapOf(
        "role" to SchemaInterface.SchemaInline(Schema(type = "string")),
        "accessLevel" to SchemaInterface.SchemaInline(Schema(type = "integer"))
    ),
    ifSchema = SchemaInterface.SchemaInline(
        Schema(
            properties = mapOf(
                "role" to SchemaInterface.SchemaInline(Schema(const = "admin"))
            )
        )
    ),
    thenSchema = SchemaInterface.SchemaInline(
        Schema(required = listOf("accessLevel"))
    ),
    elseSchema = SchemaInterface.SchemaInline(
        Schema(
            properties = mapOf(
                "accessLevel" to SchemaInterface.SchemaInline(Schema(const = 1))
            )
        )
    )
)

fun objectWithDeps() = Schema(
    type = "object",
    properties = mapOf(
        "credit_card" to SchemaInterface.SchemaInline(Schema(type = "number")),
        "billing_address" to SchemaInterface.SchemaInline(Schema(type = "string"))
    ),
    required = listOf("credit_card"),
    dependencies = mapOf(
        "credit_card" to listOf("billing_address")
    ),
    patternProperties = mapOf(
        "^S_" to SchemaInterface.SchemaInline(Schema(type = "string"))
    )
)

fun arrayWithContains() = Schema(
    type = "array",
    items = SchemaInterface.SchemaInline(
        Schema(type = "integer")
    ),
    contains = SchemaInterface.SchemaInline(
        Schema(
            type = "integer",
            minimum = 0
        )
    )
)

fun flexibleObject() = Schema(
    type = "object",
    properties = mapOf(
        "known" to SchemaInterface.SchemaInline(Schema(type = "string"))
    ),
    additionalProperties = SchemaInterface.SchemaInline(
        Schema(type = "string")
    ),
    propertyNames = SchemaInterface.SchemaInline(
        Schema(pattern = "\"^[a-zA-Z_][a-zA-Z0-9_]*$")
    )
)
