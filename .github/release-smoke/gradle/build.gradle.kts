plugins {
    id("dev.banking.asyncapi.generator")
}

asyncApiGenerator {
    executions {
        register("models") {
            generatorName.set("kotlin")
            inputSpec.set(file("api.yaml"))
            modelPackage.set("com.example.smoke.model")
        }
    }
}
