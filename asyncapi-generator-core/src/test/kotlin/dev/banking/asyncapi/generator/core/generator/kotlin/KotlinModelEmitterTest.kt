package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.ir.DefaultValue
import dev.banking.asyncapi.generator.core.generator.ir.EnumModel
import dev.banking.asyncapi.generator.core.generator.ir.ObjectModel
import dev.banking.asyncapi.generator.core.generator.ir.OpenObjectModel
import dev.banking.asyncapi.generator.core.generator.ir.PropertyDefinition
import dev.banking.asyncapi.generator.core.generator.ir.TypeRef
import dev.banking.asyncapi.generator.core.generator.ir.UnionModel
import dev.banking.asyncapi.generator.core.generator.model.EnumConstant
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinModelEmitterTest {
    private val emitter = KotlinModelEmitter()

    @Test
    fun `emits one Kotlin source per model`() {
        val result =
            emitter.emit(
                listOf(
                    ObjectModel("User", PACKAGE, null, properties = emptyList(), unions = listOf("Command")),
                    EnumModel("Status", PACKAGE, null, listOf(EnumConstant("ACTIVE", jsonValue = null))),
                    UnionModel("Command", PACKAGE, null, discriminator = null, subtypes = emptyList()),
                    OpenObjectModel("Payload", PACKAGE, null),
                ),
            )

        assertEquals(
            listOf("User.kt", "Status.kt", "Command.kt", "Payload.kt").map { "com/example/model/$it" },
            result.artifacts.map { it.relativePath },
        )
        assertTrue(result.artifacts.all { it.kind == GeneratedArtifactKind.SOURCE })
        val (user, status, command, payload) = result.artifacts.map { it.content }
        assertTrue(user.contains("data class User(") && user.contains(") : Command {"))
        assertTrue(status.contains("enum class Status"))
        assertTrue(command.contains("sealed interface Command"))
        assertTrue(payload.contains("typealias Payload = Any"))
    }

    @Test
    fun `renders property types, nullability, defaults, annotations, and imports`() {
        val order =
            ObjectModel(
                name = "Order",
                packageName = PACKAGE,
                description = null,
                properties =
                    listOf(
                        property("id", TypeRef.Scalar.UUID, required = true),
                        property("items", TypeRef.ListOf(TypeRef.Named("Item", isModel = true)), required = true),
                        property("ratio", TypeRef.Scalar.DOUBLE, default = DefaultValue.NumberLiteral("1")),
                        property("order-note", TypeRef.Scalar.STRING, identifier = "orderNote"),
                    ),
                unions = emptyList(),
            )

        val content = emitter.emit(listOf(order)).artifacts.single().content

        assertTrue(content.contains("import java.util.UUID"))
        assertTrue(content.contains("import com.example.model.Item"))
        assertTrue(content.contains("import jakarta.validation.Valid"))
        assertTrue(content.contains("val id: UUID,"))
        assertTrue(content.contains("@field:Valid\n    val items: List<Item>,"))
        assertTrue(content.contains("val ratio: Double? = 1.0,"))
        assertTrue(content.contains("@field:JsonProperty(\"order-note\")\n    val orderNote: String? = null"))
    }

    private fun property(
        wireName: String,
        type: TypeRef,
        required: Boolean = false,
        default: DefaultValue? = null,
        identifier: String = wireName,
    ) = PropertyDefinition(
        wireName = wireName,
        identifier = identifier,
        type = type,
        required = required,
        schema = Schema(),
        default = default,
    )

    private companion object {
        const val PACKAGE = "com.example.model"
    }
}
