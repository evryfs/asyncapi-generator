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
def projectedSchema = new File(
    kotlinOutput,
    "com/example/complete/kotlin/schema/AccountUpdatedV1Payload.avsc",
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
assert projectedSchema.isFile() : "Expected the configured Avro projection package"

def javaRecord = new File(
    basedir,
    "target/generated-sources/java/com/example/complete/java/model/AccountUpdatedV1Payload.java",
)
assert javaRecord.isFile() : "Expected the configured Java model package"
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
