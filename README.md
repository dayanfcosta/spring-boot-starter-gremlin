# Spring Boot Starter for Apache TinkerPop Gremlin

A Spring Boot starter that provides auto-configuration for Apache TinkerPop Gremlin, enabling seamless integration with graph databases.

## Features

- Auto-configuration for Gremlin driver with Spring Boot 3.x
- Three connection modes: Simple, Cluster, and Writer/Reader
- Built-in load balancing with round-robin strategy
- Connection pool management
- SSL/TLS support
- Authentication support
- Spring Boot Actuator health indicator
- Query logging with slow query detection
- Test utilities with embedded TinkerGraph and Testcontainers support
- Compatible with Amazon Neptune, Azure Cosmos DB, JanusGraph, and other Gremlin-compatible databases

## Requirements

- Java 17 or higher
- Spring Boot 3.x
- Apache TinkerPop Gremlin 3.8.x

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.dayanfcosta:spring-boot-starter-gremlin:1.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'io.github.dayanfcosta:spring-boot-starter-gremlin:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.dayanfcosta</groupId>
    <artifactId>spring-boot-starter-gremlin</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### 1. Add Configuration

```yaml
gremlin:
  host: localhost
  port: 8182
```

### 2. Inject and Use

```kotlin
@Service
class PersonService(
    private val graph: GraphTraversalSource
) {
    fun findAll(): List<Vertex> {
        return graph.V().hasLabel("person").toList()
    }

    fun create(name: String): Vertex {
        return graph.addV("person")
            .property("name", name)
            .next()
    }
}
```

## Configuration Modes

| Mode | Description | Configuration |
|------|-------------|---------------|
| **Simple** | Single host connection | `gremlin.host` |
| **Cluster** | Multiple hosts with load balancing | `gremlin.cluster.hosts` |
| **Writer/Reader** | Separate write and read endpoints | `gremlin.writer` + `gremlin.readers` |

### Simple Mode

```yaml
gremlin:
  host: localhost
  port: 8182
```

### Cluster Mode

```yaml
gremlin:
  cluster:
    hosts:
      - gremlin-1.example.com
      - gremlin-2.example.com
  port: 8182
```

### Writer/Reader Mode

```yaml
gremlin:
  writer: gremlin-writer.example.com
  readers:
    - gremlin-reader-1.example.com
    - gremlin-reader-2.example.com
  port: 8182
```

```kotlin
@Service
class PersonService(
    @Qualifier("gremlinWriter") private val writer: GraphTraversalSource,
    @Qualifier("gremlinReader") private val reader: GraphTraversalSource
) {
    fun create(name: String) = writer.addV("person").property("name", name).next()
    fun findAll() = reader.V().hasLabel("person").toList()
}
```

## Authentication & SSL

```yaml
gremlin:
  host: gremlin-server.example.com
  username: myuser
  password: ${GREMLIN_PASSWORD}
  enable-ssl: true
```

## Documentation

- [Configuration Modes](docs/configuration-modes.md) - Detailed explanation of connection modes
- [Configuration Reference](docs/configuration-reference.md) - Complete property reference

### Examples

- [Simple Mode](docs/examples/simple-mode.md)
- [Cluster Mode](docs/examples/cluster-mode.md)
- [Writer/Reader Mode](docs/examples/writer-reader-mode.md)
- [Authentication & SSL](docs/examples/authentication-ssl.md)

### Advanced Topics

- [Connection Pool Tuning](docs/advanced/connection-pool-tuning.md)
- [Custom Serializers](docs/advanced/custom-serializers.md)
- [Request Interceptors](docs/advanced/request-interceptors.md)

## Cloud Provider Support

### Amazon Neptune

```yaml
gremlin:
  host: my-cluster.cluster-xxxxx.us-east-1.neptune.amazonaws.com
  port: 8182
  enable-ssl: true
  serializer: GRAPHBINARY_V1
```

### Azure Cosmos DB

```yaml
gremlin:
  host: my-cosmos-account.gremlin.cosmos.azure.com
  port: 443
  enable-ssl: true
  username: /dbs/my-database/colls/my-graph
  password: ${COSMOS_PRIMARY_KEY}
  serializer: GRAPHSON_V2
```

### JanusGraph

```yaml
gremlin:
  host: janusgraph-server.example.com
  port: 8182
  serializer: GRAPHBINARY_V1
```

## Health Indicator

When Spring Boot Actuator is on the classpath, a health indicator is automatically registered.

```yaml
# Optional configuration
gremlin:
  health:
    enabled: true        # default: true
    timeout: 5000        # milliseconds, default: 5000
```

Health endpoint response:

```json
{
  "status": "UP",
  "components": {
    "gremlin": {
      "status": "UP",
      "details": {
        "host": "localhost",
        "port": 8182
      }
    }
  }
}
```

## Query Logging

Enable query logging to debug and monitor Gremlin queries with execution timing.

**Requirements:** Add `spring-boot-starter-aop` to your project.

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-aop")
}
```

### Configuration

```yaml
gremlin:
  logging:
    enabled: true
    slow-query-threshold: 1000ms  # default: 1s
    include-bytecode: false       # default: false

# Enable DEBUG logging for query output
logging:
  level:
    io.github.dayanfcosta.gremlin.query: DEBUG
```

### Log Output Examples

Normal queries (DEBUG level):
```
DEBUG - Gremlin Query: g.V(), hasLabel(person), count() | Time: 45ms
```

Slow queries (WARN level, exceeds threshold):
```
WARN - Slow Gremlin Query: g.V(), has(name, John), out(knows) | Time: 1523ms (threshold: 1000ms)
```

### Configuration Options

| Property | Description | Default |
|----------|-------------|---------|
| `gremlin.logging.enabled` | Enable query logging | `false` |
| `gremlin.logging.slow-query-threshold` | Threshold for slow query warnings | `1s` |
| `gremlin.logging.include-bytecode` | Include raw bytecode in logs | `false` |

## Testing

Add the test utilities module to your project:

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    testImplementation("io.github.dayanfcosta:spring-boot-starter-gremlin-test:1.0.0")
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.dayanfcosta</groupId>
    <artifactId>spring-boot-starter-gremlin-test</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

### Embedded TinkerGraph (No Docker Required)

Use `@GremlinTest` for fast, in-memory testing with TinkerGraph:

```kotlin
@GremlinTest
class PersonRepositoryTest {

    @Autowired
    lateinit var g: GraphTraversalSource

    @AfterEach
    fun cleanup() {
        GremlinTestUtils.clearGraph(g)
    }

    @Test
    fun `should create and find person`() {
        // Given
        val vertex = GremlinTestUtils.createVertex(g, "person",
            "name" to "John",
            "age" to 30
        )

        // When
        val found = GremlinTestUtils.findVertex(g, "person", "name" to "John")

        // Then
        assertNotNull(found)
        assertEquals("John", GremlinTestUtils.getProperty<String>(found, "name"))
    }
}
```

### Testcontainers with @ServiceConnection

For integration tests with a real Gremlin Server:

```kotlin
@SpringBootTest
@Testcontainers
class GremlinIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        val gremlin = GremlinServerContainer()
    }

    @Autowired
    lateinit var g: GraphTraversalSource

    @Test
    fun `should connect to containerized gremlin server`() {
        g.addV("person").property("name", "John").next()

        val count = g.V().hasLabel("person").count().next()
        assertEquals(1, count)
    }
}
```

### Test Utilities

| Method | Description |
|--------|-------------|
| `GremlinTestUtils.clearGraph(g)` | Remove all vertices and edges |
| `GremlinTestUtils.createVertex(g, label, props...)` | Create a vertex with properties |
| `GremlinTestUtils.createEdge(g, from, label, to, props...)` | Create an edge between vertices |
| `GremlinTestUtils.findVertex(g, label, props...)` | Find vertex by label and properties |
| `GremlinTestUtils.vertexExists(g, label, props...)` | Check if vertex exists |
| `GremlinTestUtils.countVertices(g)` / `countVertices(g, label)` | Count vertices |
| `GremlinTestUtils.countEdges(g)` / `countEdges(g, label)` | Count edges |
| `GremlinTestUtils.getProperty<T>(vertex, key)` | Get property value from vertex |

## Provided Beans

### Simple and Cluster Modes

| Bean | Type | Description |
|------|------|-------------|
| `Cluster` | `org.apache.tinkerpop.gremlin.driver.Cluster` | Connection manager |
| `Client` | `org.apache.tinkerpop.gremlin.driver.Client` | Request executor |
| `GraphTraversalSource` | `org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource` | Graph traversal API |

### Writer/Reader Mode

| Bean | Qualifier | Description |
|------|-----------|-------------|
| `Cluster` | `gremlinWriter` | Writer connection manager |
| `Cluster` | `gremlinReader` | Reader connection manager |
| `Client` | `gremlinWriter` | Writer request executor |
| `Client` | `gremlinReader` | Reader request executor |
| `GraphTraversalSource` | `gremlinWriter` | Writer traversal API |
| `GraphTraversalSource` | `gremlinReader` | Reader traversal API |

## Building from Source

```bash
./gradlew build
```

## Running Tests

```bash
./gradlew test
```

Tests use Testcontainers with `tinkerpop/gremlin-server:3.8.0` Docker image.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
