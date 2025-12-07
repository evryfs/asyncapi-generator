package com.tietoevry.banking.asyncapi.generator.core.model.schemas

/**
 * Defaults:
 * application/vnd.aai.asyncapi;version=3.0.0,
 * application/vnd.aai.asyncapi+json;version=3.0.0,
 * application/vnd.aai.asyncapi+yaml;version=3.0.0
 *
 * JSON Schema draft-07:
 * application/schema+json;version=draft-07,
 * application/schema+yaml;version=draft-07
 *
 * Avro:
 * application/vnd.apache.avro;version=1.9.0,
 * application/vnd.apache.avro+json;version=1.9.0,
 * application/vnd.apache.avro+yaml;version=1.9.0
 *
 * OpenAPI 3.0 Schema Object:
 * application/vnd.oai.openapi;version=3.0.0,
 * application/vnd.oai.openapi+json;version=3.0.0,
 * application/vnd.oai.openapi+yaml;version=3.0.0
 *
 * RAML 1.0 Data Type:
 * application/raml+yaml;version=1.0
 *
 * Protobuf:
 * application/vnd.google.protobuf;version=2,
 * application/vnd.google.protobuf;version=3
 *
 * */
data class MultiFormatSchema(
    val schemaFormat: String,
    val schema: Any
)
