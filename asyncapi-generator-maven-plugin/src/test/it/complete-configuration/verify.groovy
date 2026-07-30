def kotlinOutput = new File(basedir, "target/generated-sources/kotlin")
def bundledSpec = new File(basedir, "target/bundled/asyncapi.yaml")
def kotlinModel = new File(
    kotlinOutput,
    "com/example/complete/kotlin/model/AccountUpdatedV1Payload.kt",
)
def producer = new File(
    kotlinOutput,
    "com/example/complete/kotlin/client/producer/AccountEventsProducer.kt",
)
def consumer = new File(
    kotlinOutput,
    "com/example/complete/kotlin/client/consumer/AccountEventsConsumer.kt",
)

assert bundledSpec.isFile() : "Expected outputFile to contain the bundled AsyncAPI document"
assert kotlinModel.isFile() : "Expected the configured Kotlin model package"
assert kotlinModel.text.contains("import com.example.codegen.GeneratedPayload")
assert kotlinModel.text.contains("@GeneratedPayload")
assert producer.isFile() : "Expected producer generation to be enabled"
assert consumer.isFile() : "Expected consumer generation to be enabled"
assert producer.text.contains("@Validated")
assert producer.text.contains("@Valid")
assert producer.text.contains("fun sendAccountUpdatedV1(")
assert producer.text.contains("X_EXAMPLE_CORRELATION_ID")
assert producer.text.contains('account.\\${kafka.environment}.events.v1')
assert consumer.text.contains("fun listenAccountUpdatedV1(")

def javaRecord = new File(
    basedir,
    "target/generated-sources/java/com/example/complete/java/model/AccountUpdatedV1Payload.java",
)
assert javaRecord.isFile() : "Expected the configured Java model package"
assert javaRecord.text.contains("import com.example.codegen.GeneratedPayload;")
assert javaRecord.text.contains("@GeneratedPayload")
assert javaRecord.text.contains("public record AccountUpdatedV1Payload(")

def nativeAvroSchema = new File(
    basedir,
    "target/generated-sources/avro/com/example/complete/avro/AccountSnapshot.avsc",
)
def specificRecord = new File(
    basedir,
    "target/generated-sources/avro/com/example/complete/avro/AccountSnapshot.java",
)
assert nativeAvroSchema.isFile() : "Expected native Avro schema generation"
assert specificRecord.isFile() : "Expected Avro SpecificRecord generation"
assert specificRecord.text.contains("extends org.apache.avro.specific.SpecificRecordBase")

def protobufOutput = new File(
    basedir,
    "target/generated-sources/protobuf/com/example/complete/protobuf",
)
def nativeProtobufSchema = new File(protobufOutput, "AccountSnapshot.proto")
def protobufJavaMessage = new File(protobufOutput, "AccountSnapshot.java")
def protobufKotlinDsl = new File(protobufOutput, "AccountSnapshotKt.kt")
assert nativeProtobufSchema.isFile() : "Expected native Protobuf schema generation"
assert protobufJavaMessage.isFile() : "Expected the Protobuf Java runtime message"
assert protobufKotlinDsl.isFile() : "Expected the configured Protobuf Kotlin DSL"

def avroSchemaOnlyOutput = new File(
    basedir,
    "target/generated-schemas/avro/com/example/complete/schema/avro",
)
def avroSchemaOnlyArtifact = new File(
    avroSchemaOnlyOutput,
    "AccountUpdatedV1Payload.avsc",
)
assert avroSchemaOnlyArtifact.isFile() : "Expected Avro schema-only generation"
assert !avroSchemaOnlyOutput
    .listFiles()
    .any { file -> file.name.endsWith(".java") || file.name.endsWith(".kt") } :
    "Avro schema-only generation must not create source models"

def protobufSchemaOnlyOutput = new File(
    basedir,
    "target/generated-schemas/protobuf/com/example/complete/schema/protobuf",
)
def protobufSchemaOnlyArtifact = new File(
    protobufSchemaOnlyOutput,
    "AccountSnapshot.proto",
)
assert protobufSchemaOnlyArtifact.isFile() : "Expected Protobuf schema-only generation"
assert !protobufSchemaOnlyOutput
    .listFiles()
    .any { file -> file.name.endsWith(".java") || file.name.endsWith(".kt") } :
    "Protobuf schema-only generation must not create runtime message sources"

def jsonSchemaOnlyOutput = new File(
    basedir,
    "target/generated-schemas/json/com/example/complete/schema/json",
)
def jsonSchemaOnlyArtifact = new File(
    jsonSchemaOnlyOutput,
    "AccountUpdatedV1Payload.schema.json",
)
assert jsonSchemaOnlyArtifact.isFile() : "Expected JSON Schema-only generation"
assert jsonSchemaOnlyArtifact.text.contains('"$schema" : "http://json-schema.org/draft-07/schema#"')
assert !jsonSchemaOnlyOutput
    .listFiles()
    .any { file -> file.name.endsWith(".java") || file.name.endsWith(".kt") } :
    "JSON Schema-only generation must not create source models"

def asyncApiYaml = new File(basedir, "target/documents/asyncapi.yaml")
assert asyncApiYaml.isFile() : "Expected AsyncAPI YAML document generation"
assert asyncApiYaml.text.startsWith("asyncapi:")

def asyncApiJson = new File(basedir, "target/documents/asyncapi.json")
assert asyncApiJson.isFile() : "Expected AsyncAPI JSON document generation"
assert asyncApiJson.text.startsWith("{")
assert asyncApiJson.text.contains('"asyncapi"')

def compiledClasses = new File(basedir, "target/classes")
assert new File(
    compiledClasses,
    "com/example/complete/kotlin/model/AccountUpdatedV1Payload.class",
).isFile() : "Expected the generated Kotlin model to compile"
assert new File(
    compiledClasses,
    "com/example/complete/kotlin/client/producer/AccountEventsProducer.class",
).isFile() : "Expected the generated Kotlin producer contract to compile"
assert new File(
    compiledClasses,
    "com/example/complete/kotlin/client/consumer/AccountEventsConsumer.class",
).isFile() : "Expected the generated Kotlin consumer contract to compile"
assert new File(
    compiledClasses,
    "com/example/complete/java/model/AccountUpdatedV1Payload.class",
).isFile() : "Expected the generated Java record to compile"
assert new File(
    compiledClasses,
    "com/example/complete/avro/AccountSnapshot.class",
).isFile() : "Expected the generated Avro SpecificRecord to compile"
assert new File(
    compiledClasses,
    "com/example/complete/protobuf/AccountSnapshot.class",
).isFile() : "Expected the generated Protobuf message to compile"
