package dev.banking.asyncapi.generator.core.generator.java.ref

import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateReferencedPrimitiveTypeTest : AbstractJavaGeneratorClass() {
    @Test
    fun `primitive refs should map to primitive java types while enum and object refs stay named`() {
        val modelPackage = "dev.banking.asyncapi.generator.core.model.generated.reftyping"
        val generated =
            generateElement(
                yaml = File("src/test/resources/generator/asyncapi_ref_primitive_typing.yaml"),
                generated = "MyPayload.java",
                modelPackage = modelPackage,
            )

        assertTrue(generated.contains("private String myField;"), "Expected primitive ref type for myField")
        assertTrue(generated.contains("@Pattern(regexp = \"^[0-9]{4,35}$\")"), "Expected constraints from referenced primitive schema")
        assertFalse(generated.contains("import $modelPackage.MyField;"), "Primitive ref should not import model type MyField")

        assertTrue(generated.contains("private MyEnum myEnum;"), "Enum ref should stay named")
        assertTrue(generated.contains("private MyObject myObject;"), "Object ref should stay named")
    }
}
