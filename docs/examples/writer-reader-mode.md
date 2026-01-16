# Writer/Reader Mode Example

This example demonstrates how to use the Spring Boot Starter for Gremlin in Writer/Reader Mode, separating write and read operations across dedicated server instances.

## Configuration

```yaml
# application.yaml
gremlin:
  writer: gremlin-writer.example.com
  readers:
    - gremlin-reader-1.example.com
    - gremlin-reader-2.example.com
  port: 8182
```

## When to Use Writer/Reader Mode

This mode is ideal for:

- **Graph databases with replication**: Amazon Neptune, JanusGraph with read replicas
- **Read-heavy workloads**: Scale reads independently by adding more reader nodes
- **Write/read isolation**: Prevent read queries from impacting write performance
- **Geographic distribution**: Route reads to nearby replicas for lower latency

## Understanding the Beans

In Writer/Reader mode, the starter provides **qualified beans** for each endpoint:

| Bean Type | Writer Qualifier | Reader Qualifier |
|-----------|-----------------|------------------|
| `Cluster` | `gremlinWriter` | `gremlinReader` |
| `Client` | `gremlinWriter` | `gremlinReader` |
| `GraphTraversalSource` | `gremlinWriter` | `gremlinReader` |

## Basic Usage

### Injecting Both Sources

```kotlin
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class PersonService(
    @Qualifier("gremlinWriter")
    private val writer: GraphTraversalSource,

    @Qualifier("gremlinReader")
    private val reader: GraphTraversalSource
) {
    // Use 'writer' for create, update, delete operations
    // Use 'reader' for read operations
}
```

### Write Operations (Writer)

```kotlin
@Service
class PersonService(
    @Qualifier("gremlinWriter")
    private val writer: GraphTraversalSource,

    @Qualifier("gremlinReader")
    private val reader: GraphTraversalSource
) {
    fun createPerson(name: String, age: Int): Any {
        val vertex = writer.addV("person")
            .property("name", name)
            .property("age", age)
            .next()

        return vertex.id()
    }

    fun updatePersonAge(name: String, newAge: Int) {
        writer.V()
            .hasLabel("person")
            .has("name", name)
            .property("age", newAge)
            .iterate()
    }

    fun deletePerson(name: String) {
        writer.V()
            .hasLabel("person")
            .has("name", name)
            .drop()
            .iterate()
    }
}
```

### Read Operations (Reader)

```kotlin
@Service
class PersonService(
    @Qualifier("gremlinWriter")
    private val writer: GraphTraversalSource,

    @Qualifier("gremlinReader")
    private val reader: GraphTraversalSource
) {
    fun findAllPersons(): List<Map<String, Any>> {
        return reader.V()
            .hasLabel("person")
            .valueMap<Any>("name", "age")
            .toList()
            .map { it.toMap() }
    }

    fun findPersonByName(name: String): Map<String, Any>? {
        return reader.V()
            .hasLabel("person")
            .has("name", name)
            .valueMap<Any>("name", "age")
            .tryNext()
            .orElse(null)
            ?.toMap()
    }

    fun countPersons(): Long {
        return reader.V()
            .hasLabel("person")
            .count()
            .next()
    }
}
```

## Handling Replication Lag

When using Writer/Reader mode with replicated databases, there may be a delay before writes are visible on readers. Consider these patterns:

### Read-Your-Writes Pattern

For operations where the user needs to see their changes immediately:

```kotlin
@Service
class PersonService(
    @Qualifier("gremlinWriter")
    private val writer: GraphTraversalSource,

    @Qualifier("gremlinReader")
    private val reader: GraphTraversalSource
) {
    fun createAndReturn(name: String, age: Int): Map<String, Any> {
        // Write to writer
        val vertex = writer.addV("person")
            .property("name", name)
            .property("age", age)
            .next()

        // Read back from writer (not reader) to avoid replication lag
        return writer.V(vertex.id())
            .valueMap<Any>("name", "age")
            .next()
            .toMap()
    }
}
```

### Eventual Consistency with Retry

For non-critical reads that can tolerate eventual consistency:

```kotlin
@Service
class PersonService(
    @Qualifier("gremlinWriter")
    private val writer: GraphTraversalSource,

    @Qualifier("gremlinReader")
    private val reader: GraphTraversalSource
) {
    fun findPersonWithRetry(id: Any, maxRetries: Int = 3): Map<String, Any>? {
        repeat(maxRetries) { attempt ->
            val result = reader.V(id)
                .valueMap<Any>("name", "age")
                .tryNext()
                .orElse(null)

            if (result != null) {
                return result.toMap()
            }

            if (attempt < maxRetries - 1) {
                Thread.sleep(100L * (attempt + 1)) // Exponential backoff
            }
        }
        return null
    }
}
```

## Complete REST Controller Example

```kotlin
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class PersonRequest(val name: String, val age: Int)
data class PersonResponse(val id: Any, val name: String, val age: Int)

@RestController
@RequestMapping("/api/persons")
class PersonController(
    @Qualifier("gremlinWriter")
    private val writer: GraphTraversalSource,

    @Qualifier("gremlinReader")
    private val reader: GraphTraversalSource
) {
    @PostMapping
    fun create(@RequestBody request: PersonRequest): ResponseEntity<PersonResponse> {
        // Write to writer endpoint
        val vertex = writer.addV("person")
            .property("name", request.name)
            .property("age", request.age)
            .next()

        // Read back from writer to avoid replication lag
        val created = writer.V(vertex.id())
            .project<Any>("id", "name", "age")
            .by(org.apache.tinkerpop.gremlin.structure.T.id)
            .by("name")
            .by("age")
            .next()

        return ResponseEntity.ok(
            PersonResponse(
                id = created["id"]!!,
                name = created["name"] as String,
                age = created["age"] as Int
            )
        )
    }

    @GetMapping
    fun findAll(): ResponseEntity<List<PersonResponse>> {
        // Read from reader endpoint (load-balanced across readers)
        val persons = reader.V()
            .hasLabel("person")
            .project<Any>("id", "name", "age")
            .by(org.apache.tinkerpop.gremlin.structure.T.id)
            .by("name")
            .by("age")
            .toList()
            .map { map ->
                PersonResponse(
                    id = map["id"]!!,
                    name = map["name"] as String,
                    age = map["age"] as Int
                )
            }

        return ResponseEntity.ok(persons)
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ResponseEntity<PersonResponse> {
        // Read from reader endpoint
        val person = reader.V(id)
            .project<Any>("id", "name", "age")
            .by(org.apache.tinkerpop.gremlin.structure.T.id)
            .by("name")
            .by("age")
            .tryNext()
            .orElse(null)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            PersonResponse(
                id = person["id"]!!,
                name = person["name"] as String,
                age = person["age"] as Int
            )
        )
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: PersonRequest
    ): ResponseEntity<PersonResponse> {
        // Write to writer endpoint
        val updated = writer.V(id)
            .property("name", request.name)
            .property("age", request.age)
            .project<Any>("id", "name", "age")
            .by(org.apache.tinkerpop.gremlin.structure.T.id)
            .by("name")
            .by("age")
            .tryNext()
            .orElse(null)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            PersonResponse(
                id = updated["id"]!!,
                name = updated["name"] as String,
                age = updated["age"] as Int
            )
        )
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        // Write (delete) to writer endpoint
        writer.V(id).drop().iterate()
        return ResponseEntity.noContent().build()
    }
}
```

## Amazon Neptune Configuration

For Amazon Neptune with read replicas:

```yaml
gremlin:
  writer: my-cluster.cluster-xxxxx.us-east-1.neptune.amazonaws.com
  readers:
    - my-cluster.cluster-ro-xxxxx.us-east-1.neptune.amazonaws.com
  port: 8182
  enable-ssl: true
  serializer: GRAPHBINARY_V1
```

## Testing

See [WriterReadModeConfigurationIntegrationTest](../../src/test/kotlin/com/github/dayanfcosta/gremlin/autoconfigure/WriterReadModeConfigurationIntegrationTest.kt) for integration testing examples using Testcontainers with separate writer and reader containers.
