# Simple Mode Example

This example demonstrates how to use the Spring Boot Starter for Gremlin in Simple Mode, connecting to a single Gremlin Server instance.

## Configuration

```yaml
# application.yaml
gremlin:
  host: localhost
  port: 8182
```

## Basic Usage

### Injecting GraphTraversalSource

```kotlin
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.springframework.stereotype.Service

@Service
class GraphService(
    private val graph: GraphTraversalSource
) {
    // Your graph operations here
}
```

## CRUD Operations

### Creating Vertices

```kotlin
@Service
class PersonService(
    private val graph: GraphTraversalSource
) {
    fun createPerson(name: String, age: Int): Map<Any, Any> {
        val vertex = graph.addV("person")
            .property("name", name)
            .property("age", age)
            .next()

        return mapOf(
            "id" to vertex.id(),
            "label" to vertex.label()
        )
    }
}
```

### Reading Vertices

```kotlin
@Service
class PersonService(
    private val graph: GraphTraversalSource
) {
    fun findAllPersons(): List<Map<String, Any>> {
        return graph.V()
            .hasLabel("person")
            .valueMap<Any>("name", "age")
            .with("~tinkerpop.valueMap.tokens")
            .toList()
            .map { it.toMap() }
    }

    fun findPersonByName(name: String): Map<String, Any>? {
        return graph.V()
            .hasLabel("person")
            .has("name", name)
            .valueMap<Any>("name", "age")
            .with("~tinkerpop.valueMap.tokens")
            .tryNext()
            .orElse(null)
            ?.toMap()
    }

    fun findPersonById(id: Any): Map<String, Any>? {
        return graph.V(id)
            .valueMap<Any>("name", "age")
            .with("~tinkerpop.valueMap.tokens")
            .tryNext()
            .orElse(null)
            ?.toMap()
    }
}
```

### Updating Vertices

```kotlin
@Service
class PersonService(
    private val graph: GraphTraversalSource
) {
    fun updatePersonAge(name: String, newAge: Int): Boolean {
        val result = graph.V()
            .hasLabel("person")
            .has("name", name)
            .property("age", newAge)
            .tryNext()

        return result.isPresent
    }
}
```

### Deleting Vertices

```kotlin
@Service
class PersonService(
    private val graph: GraphTraversalSource
) {
    fun deletePerson(name: String): Boolean {
        val count = graph.V()
            .hasLabel("person")
            .has("name", name)
            .drop()
            .iterate()

        return true
    }

    fun deletePersonById(id: Any): Boolean {
        graph.V(id).drop().iterate()
        return true
    }
}
```

## Working with Edges

### Creating Relationships

```kotlin
@Service
class RelationshipService(
    private val graph: GraphTraversalSource
) {
    fun createFriendship(person1Name: String, person2Name: String, since: Int) {
        graph.V()
            .hasLabel("person")
            .has("name", person1Name)
            .addE("friends")
            .to(
                graph.V()
                    .hasLabel("person")
                    .has("name", person2Name)
            )
            .property("since", since)
            .iterate()
    }
}
```

### Querying Relationships

```kotlin
@Service
class RelationshipService(
    private val graph: GraphTraversalSource
) {
    fun findFriends(personName: String): List<String> {
        return graph.V()
            .hasLabel("person")
            .has("name", personName)
            .out("friends")
            .values<String>("name")
            .toList()
    }

    fun findFriendsOfFriends(personName: String): List<String> {
        return graph.V()
            .hasLabel("person")
            .has("name", personName)
            .out("friends")
            .out("friends")
            .dedup()
            .values<String>("name")
            .toList()
    }
}
```

## Complete REST Controller Example

```kotlin
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class PersonRequest(val name: String, val age: Int)
data class PersonResponse(val id: Any, val name: String, val age: Int)

@RestController
@RequestMapping("/api/persons")
class PersonController(
    private val graph: GraphTraversalSource
) {
    @PostMapping
    fun create(@RequestBody request: PersonRequest): ResponseEntity<PersonResponse> {
        val vertex = graph.addV("person")
            .property("name", request.name)
            .property("age", request.age)
            .next()

        return ResponseEntity.ok(
            PersonResponse(
                id = vertex.id(),
                name = request.name,
                age = request.age
            )
        )
    }

    @GetMapping
    fun findAll(): ResponseEntity<List<PersonResponse>> {
        val persons = graph.V()
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
        val person = graph.V(id)
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

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        graph.V(id).drop().iterate()
        return ResponseEntity.noContent().build()
    }
}
```

## Testing

See [SimpleModeConfigurationIntegrationTest](../../src/test/kotlin/com/github/dayanfcosta/gremlin/autoconfigure/SimpleModeConfigurationIntegrationTest.kt) for integration testing examples using Testcontainers.
