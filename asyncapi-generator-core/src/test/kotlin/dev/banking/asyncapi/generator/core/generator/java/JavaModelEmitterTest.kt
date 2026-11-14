package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JavaModelEmitterTest {
    @Test
    fun `emits one Java source per model except open objects`() {
        val result =
            JavaModelEmitter().emit(
                listOf(
                    user(),
                    EnumModel("Status", PACKAGE, null, listOf(EnumConstant("ACTIVE", jsonValue = null))),
                    UnionModel("Command", PACKAGE, null, discriminator = null, subtypes = emptyList()),
                    OpenObjectModel("Payload", PACKAGE, null),
                ),
            )

        assertEquals(
            listOf("User.java", "Status.java", "Command.java").map { "com/example/model/$it" },
            result.artifacts.map { it.relativePath },
        )
        assertTrue(result.artifacts.all { it.kind == GeneratedArtifactKind.SOURCE })
        val (user, status, command) = result.artifacts.map { it.content }
        assertTrue(user.contains("public class User implements Command, Serializable"))
        assertTrue(user.contains("public UUID getId()"))
        assertTrue(status.contains("public enum Status implements Serializable"))
        assertTrue(command.contains("public interface Command extends Serializable"))
    }

    @Test
    fun `renders required properties and references to open objects`() {
        val content = JavaModelEmitter().emit(listOf(user())).artifacts.single().content

        assertTrue(content.contains("import java.util.UUID;"))
        assertTrue(content.contains("import jakarta.validation.constraints.NotNull;"))
        assertTrue(content.contains("@NotNull\n    private UUID id;"))
        assertTrue(content.contains("private Object payload;"))
        assertFalse(content.contains("import com.example.model.Payload;"))
    }

    @Test
    fun `emits records when configured`() {
        val emitter = JavaModelEmitter(JavaModelType.RECORD)

        val user = emitter.emit(listOf(user())).artifacts.single().content
        val empty =
            emitter.emit(listOf(ObjectModel("Empty", PACKAGE, null, emptyList(), emptyList()))).artifacts.single().content

        assertTrue(user.contains("public record User("))
        assertFalse(user.contains("java.util.Objects"))
        assertTrue(empty.contains("public record Empty() implements Serializable"))
    }

    private fun user() =
        ObjectModel(
            name = "User",
            packageName = PACKAGE,
            description = null,
            properties =
                listOf(
                    PropertyDefinition("id", "id", TypeRef.Scalar.UUID, required = true, schema = Schema()),
                    PropertyDefinition(
                        "payload",
                        "payload",
                        TypeRef.Named("Payload", isModel = true, isOpenObject = true),
                        required = false,
                        schema = Schema(type = "object"),
                    ),
                ),
            unions = listOf("Command"),
        )

    private companion object {
        const val PACKAGE = "com.example.model"
    }
}
