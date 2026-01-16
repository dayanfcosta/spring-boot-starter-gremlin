# Custom Serializers

This guide explains the available serializers and when to use each one.

## Available Serializers

| Serializer | Format | Use Case |
|------------|--------|----------|
| `GRAPHBINARY_V1` | Binary | Production (recommended) |
| `GRAPHSON_V3` | JSON | Debugging, interoperability |
| `GRAPHSON_V2` | JSON | Legacy systems |
| `GRAPHSON_V1` | JSON | Legacy systems |

## Configuration

```yaml
gremlin:
  host: localhost
  serializer: GRAPHBINARY_V1
```

## GraphBinary (Recommended)

GraphBinary is the most efficient serializer for production use.

**Advantages:**
- Smallest payload size
- Fastest serialization/deserialization
- Type-safe
- Full support for all Gremlin types

**Configuration:**

```yaml
gremlin:
  serializer: GRAPHBINARY_V1
```

**When to use:**
- Production environments
- High-throughput applications
- When bandwidth is a concern

## GraphSON V3

GraphSON V3 is JSON-based and human-readable.

**Advantages:**
- Human-readable for debugging
- Easy to inspect with standard JSON tools
- Good interoperability with other languages

**Disadvantages:**
- Larger payload size
- Slower than GraphBinary
- Type information embedded in JSON

**Configuration:**

```yaml
gremlin:
  serializer: GRAPHSON_V3
```

**Example payload:**

```json
{
  "@type": "g:Vertex",
  "@value": {
    "id": {
      "@type": "g:Int64",
      "@value": 1
    },
    "label": "person",
    "properties": {
      "name": [
        {
          "@type": "g:VertexProperty",
          "@value": {
            "id": {"@type": "g:Int64", "@value": 0},
            "value": "Alice",
            "label": "name"
          }
        }
      ]
    }
  }
}
```

**When to use:**
- Development and debugging
- When you need to inspect traffic
- Interoperability with systems expecting JSON

## GraphSON V2 and V1

Legacy JSON formats maintained for backward compatibility.

**Configuration:**

```yaml
gremlin:
  serializer: GRAPHSON_V2  # or GRAPHSON_V1
```

**When to use:**
- Connecting to older Gremlin Servers
- Legacy system integration
- Generally avoid for new projects

## Serializer Compatibility

| Server Version | Recommended Serializer |
|----------------|----------------------|
| TinkerPop 3.5+ | GRAPHBINARY_V1 |
| TinkerPop 3.4 | GRAPHSON_V3 |
| TinkerPop 3.3 | GRAPHSON_V2 |
| TinkerPop 3.2 | GRAPHSON_V1 |

## Cloud Provider Serializers

### Amazon Neptune

Neptune supports GraphBinary and GraphSON:

```yaml
gremlin:
  host: my-cluster.cluster-xxxxx.us-east-1.neptune.amazonaws.com
  serializer: GRAPHBINARY_V1  # Recommended
  enable-ssl: true
```

### Azure Cosmos DB

Cosmos DB Gremlin API requires GraphSON V2:

```yaml
gremlin:
  host: my-cosmos-account.gremlin.cosmos.azure.com
  serializer: GRAPHSON_V2  # Required for Cosmos DB
  enable-ssl: true
```

### JanusGraph

JanusGraph supports all serializers:

```yaml
gremlin:
  host: janusgraph-server.example.com
  serializer: GRAPHBINARY_V1  # Recommended
```

## Performance Comparison

Approximate comparison (varies by data):

| Serializer | Relative Size | Relative Speed |
|------------|--------------|----------------|
| GRAPHBINARY_V1 | 1x (baseline) | 1x (fastest) |
| GRAPHSON_V3 | 3-5x larger | 2-3x slower |
| GRAPHSON_V2 | 3-5x larger | 2-3x slower |
| GRAPHSON_V1 | 2-4x larger | 2-3x slower |

## Debugging with GraphSON

During development, you might want to see the actual data being transferred:

```kotlin
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.springframework.stereotype.Service

@Service
class DebugService(
    private val graph: GraphTraversalSource
) {
    fun debugQuery(): String {
        // Get result as a map for inspection
        val result = graph.V()
            .hasLabel("person")
            .limit(1)
            .valueMap<Any>()
            .next()

        // Convert to JSON for debugging
        return result.toString()
    }
}
```

## Switching Serializers

To switch serializers (e.g., for debugging), you can use Spring profiles:

```yaml
# application.yaml (production)
gremlin:
  host: localhost
  serializer: GRAPHBINARY_V1

---
# application-debug.yaml
spring:
  config:
    activate:
      on-profile: debug

gremlin:
  serializer: GRAPHSON_V3
```

Then run with:

```bash
java -jar your-app.jar --spring.profiles.active=debug
```

## Troubleshooting

### Serialization Errors

**Symptom:** `ResponseException` with serialization errors

**Solutions:**
1. Verify server and client use compatible serializers
2. Check server configuration for supported serializers
3. Try a different serializer

### Type Conversion Issues

**Symptom:** Unexpected types in query results

**Solutions:**
1. Use GraphSON for debugging to see exact types
2. Explicitly cast results in your code
3. Use `.project()` step to control output structure

### Large Payload Errors

**Symptom:** Request too large errors

**Solutions:**
1. Switch to GraphBinary for smaller payloads
2. Increase `max-content-length` in connection pool
3. Paginate large result sets
