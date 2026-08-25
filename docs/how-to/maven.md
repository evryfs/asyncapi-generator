# Maven

Add the plugin to your `pom.xml`:

```xml
<plugin>
    <groupId>dev.banking.asyncapi.generator</groupId>
    <artifactId>asyncapi-generator-maven-plugin</artifactId>
    <version>0.0.1</version>
    <executions>
        <execution>
            <id>generate</id>
            <phase>generate-sources</phase>
            <goals><goal>generate</goal></goals>
            <configuration>
                <generatorName>kotlin</generatorName>
                <inputSpec>src/main/resources/asyncapi.yaml</inputSpec>
                <models>
                    <packageName>com.example.model</packageName>
                </models>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Run with:

```sh
mvn generate-sources
```

## Configuration options

| Option          | Description                                                                                                             | Default  |
|-----------------|-------------------------------------------------------------------------------------------------------------------------|----------|
| `generatorName` | Generator profile (`kotlin`, `java`, `avro-schema`, `protobuf-schema`, `json-schema`, `asyncapi-yaml`, `asyncapi-json`) | required |
| `inputSpec`     | Path to the AsyncAPI YAML or JSON file                                                                                  | required |
| `outputFile`    | File for bundled document output                                                                                        | —        |
| `modelPackage`  | Package for generated payload models                                                                                    | —        |
| `clientPackage` | Package for generated client contracts                                                                                  | —        |
| `schemaPackage` | Package for generated schema artifacts                                                                                  | —        |

## Model configuration

```xml
<models>
    <packageName>com.example.model</packageName>
    <javaModelType>record</javaModelType> <!-- class or record -->
    <annotation>com.example.Nullable</annotation>
</models>
```

## Spring Kafka client configuration

```xml
<clients>
    <kafka>
        <packageName>com.example.client</packageName>
        <modelPackageName>com.example.model</modelPackageName>
        <springKafka>
            <enabled>true</enabled>
            <clientContract>interface</clientContract>
            <producer>
                <enabled>true</enabled>
                <additionalPayloadTypes>
                    <additionalPayloadType>byte-array</additionalPayloadType>
                </additionalPayloadTypes>
            </producer>
            <consumer>
                <enabled>true</enabled>
            </consumer>
        </springKafka>
    </kafka>
</clients>
```

## Schema configuration

```xml
<schemas>
    <avroProjection>
        <packageName>com.example.schema</packageName>
    </avroProjection>
    <nativeAvro>
        <generateSpecificRecords>true</generateSpecificRecords>
    </nativeAvro>
    <nativeProtobuf>
        <enabled>true</enabled>
    </nativeProtobuf>
</schemas>
```
