package com.tietoevry.banking.asyncapi.generator.core.parser.bindings

import com.tietoevry.banking.asyncapi.generator.core.model.bindings.Binding

fun userSignedUpChannelBinding() = Binding(
    content = mapOf(
        "kafka" to mapOf(
            "topic" to "'my-specific-topic-name",
            "partitions" to 20,
            "replicas" to 3,
            "topicConfiguration" to mapOf(
                "cleanup.policy" to listOf("\"delete", "\"compact"),
                "retention.ms" to 604800000,
                "retention.bytes" to 1000000000,
                "delete.retention.ms" to 86400000,
                "max.message.bytes" to 1048588
            ),
            "bindingVersion" to "'0.5.0"
        )
    )
)

fun userSignedUpMessageBinding() = Binding(
    content = mapOf(
        "amqp" to mapOf(
            "contentEncoding" to "gzip",
            "messageType" to "user.signup"
        )
    )
)

fun myServerBinding() = Binding(
    content = mapOf(
        "mqtt" to mapOf(
            "clientId" to "guest",
            "cleanSession" to true
        )
    )
)

fun myOperationBinding() = Binding(
    content = mapOf(
        "http" to mapOf(
            "method" to "POST",
            "query" to mapOf("type" to "object")
        )
    )
)
