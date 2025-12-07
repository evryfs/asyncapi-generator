package dev.banking.asyncapi.generator.core.parser.servers

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.tags.Tag
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface

fun scramConnections() = Server(
    host = "'test.mykafkacluster.org:{port}/{environment}",
    protocol = "kafka-secure",
    description = "Test broker secured with scramSha256",
    tags = listOf(
        TagInterface.TagInline(
            Tag(
                name = "\"env:test-scram",
                description = ">This environment is meant for running internal tests through scramSha256\n"
            )
        ),
        TagInterface.TagInline(
            Tag(
                name = "'kind:remote",
                description = "|This server is a remote server. \nNot exposed by the application\n"
            )
        ),
        TagInterface.TagInline(
            Tag(
                name = "'visibility:private",
                description = "|- This is one point\n- This in another point\n"
            )
        )
    ),
    variables = mapOf(
        "port" to ServerVariableInterface.ServerVariableInline(
            ServerVariable(
                enum = listOf("\"18092", "\"28092"),
                default = "\"18092",
                description = "The port used for Kafka connections"
            )
        ),
        "environment" to ServerVariableInterface.ServerVariableInline(
            ServerVariable(
                enum = listOf("\"test", "\"staging", "\"prod"),
                default = "\"test",
                description = "Deployment environment"
            )
        )
    )
)

fun mtlsConnections() = Server(
    host = "'test.mykafkacluster.org:28092",
    protocol = "kafka-secure",
    description = "Test broker secured with X509",
    tags = listOf(
        TagInterface.TagInline(
            Tag(
                name = "'env:test-mtls",
                description = "This environment is meant for running internal tests through mtls"
            )
        ),
        TagInterface.TagInline(
            Tag(
                name = "'kind:remote",
                description = "This server is a remote server. Not exposed by the application"
            )
        ),
        TagInterface.TagInline(
            Tag(
                name = "'visibility:private",
                description = "This resource is private and only available to certain users"
            )
        )
    )
)

fun stagingReference() = Reference("'#/components/servers/stagingServer")
