package dev.banking.asyncapi.generator.core.generator.kotlin.ref

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateReferencedPrimitiveTypeTest : AbstractKotlinGeneratorClass() {
    @Test
    fun `primitive refs should map to primitive kotlin types while enum and object refs stay named`() {
        val modelPackage = "dev.banking.asyncapi.generator.core.model.generated.reftyping"
        val generated =
            generateElement(
                yaml = File("src/test/resources/generator/asyncapi_ref_primitive_typing.yaml"),
                generated = "MyPayload.kt",
                modelPackage = modelPackage,
            )

        assertTrue(generated.contains("val myField: String"), "Expected primitive ref type for myField")
        assertTrue(
            generated.contains("@field:Pattern(regexp = \"^[0-9]{4,35}$\")"),
            "Expected constraints from referenced primitive schema",
        )
        assertFalse(generated.contains("import $modelPackage.MyField"), "Primitive ref should not import model type MyField")

        assertTrue(generated.contains("val myEnum: MyEnum"), "Enum ref should stay named")
        assertTrue(generated.contains("val myObject: MyObject"), "Object ref should stay named")
    }
}
