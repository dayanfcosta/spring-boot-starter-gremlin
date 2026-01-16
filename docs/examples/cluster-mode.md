# Cluster Mode Example

This example demonstrates how to use the Spring Boot Starter for Gremlin in Cluster Mode, connecting to multiple Gremlin Server instances with automatic load balancing.

## Configuration

```yaml
# application.yaml
gremlin:
  cluster:
    hosts:
      - gremlin-server-1.example.com
      - gremlin-server-2.example.com
      - gremlin-server-3.example.com
  port: 8182
```

## How Load Balancing Works

The Gremlin driver uses a **round-robin** strategy to distribute requests across all configured hosts:

1. Each request is sent to the next host in the list
2. If a host becomes unavailable, it's automatically removed from rotation
3. The driver periodically attempts to reconnect to failed hosts
4. When a host recovers, it's added back to the rotation

## Basic Usage

The usage is identical to Simple Mode. The load balancing is handled transparently by the driver.

```kotlin
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.springframework.stereotype.Service

@Service
class PersonService(
    private val graph: GraphTraversalSource
) {
    // Requests are automatically distributed across cluster nodes
    fun findAllPersons(): List<Map<String, Any>> {
        return graph.V()
            .hasLabel("person")
            .valueMap<Any>("name", "age")
            .toList()
            .map { it.toMap() }
    }

    fun createPerson(name: String, age: Int) {
        graph.addV("person")
            .property("name", name)
            .property("age", age)
            .iterate()
    }
}
```

## Production Configuration

For production environments, consider these additional settings:

```yaml
gremlin:
  cluster:
    hosts:
      - gremlin-server-1.example.com
      - gremlin-server-2.example.com
      - gremlin-server-3.example.com
  port: 8182

  # Authentication
  username: ${GREMLIN_USERNAME}
  password: ${GREMLIN_PASSWORD}

  # SSL/TLS
  enable-ssl: true

  # Timeouts
  connection-timeout: 10s
  request-timeout: 60s
  keep-alive-interval: 30s

  # Connection pool tuning for high load
  connection-pool:
    min-connections-per-host: 4
    max-connections-per-host: 16
    max-simultaneous-usage-per-connection: 32
    max-in-process-per-connection: 8
    max-wait-for-connection: 5s
    reconnect-interval: 2s
```

## High Availability Patterns

### Retry Logic

While the driver handles host failover automatically, you may want application-level retry logic for transient failures:

```kotlin
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service

@Service
class ResilientPersonService(
    private val graph: GraphTraversalSource
) {
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    fun findPersonByName(name: String): Map<String, Any>? {
        return graph.V()
            .hasLabel("person")
            .has("name", name)
            .valueMap<Any>()
            .tryNext()
            .orElse(null)
            ?.toMap()
    }
}
```

### Circuit Breaker

For more sophisticated resilience, use a circuit breaker pattern:

```kotlin
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.springframework.stereotype.Service

@Service
class ResilientPersonService(
    private val graph: GraphTraversalSource
) {
    @CircuitBreaker(name = "gremlin", fallbackMethod = "findPersonFallback")
    fun findPerson(name: String): Map<String, Any>? {
        return graph.V()
            .hasLabel("person")
            .has("name", name)
            .valueMap<Any>()
            .tryNext()
            .orElse(null)
            ?.toMap()
    }

    fun findPersonFallback(name: String, ex: Exception): Map<String, Any>? {
        // Return cached data or handle gracefully
        return null
    }
}
```

## Monitoring Cluster Health

You can monitor which hosts are available by accessing the Cluster bean directly:

```kotlin
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.springframework.stereotype.Component

@Component
class ClusterHealthMonitor(
    private val cluster: Cluster
) {
    fun getAvailableHosts(): List<String> {
        return cluster.availableHosts()
            .map { "${it.address.hostName}:${it.address.port}" }
    }

    fun getTotalHosts(): Int {
        return cluster.allHosts().size
    }

    fun isHealthy(): Boolean {
        return cluster.availableHosts().isNotEmpty()
    }
}
```

## Docker Compose Example

For local development with multiple Gremlin Server instances:

```yaml
# docker-compose.yaml
version: '3.8'

services:
  gremlin-server-1:
    image: tinkerpop/gremlin-server:3.8.0
    ports:
      - "8182:8182"

  gremlin-server-2:
    image: tinkerpop/gremlin-server:3.8.0
    ports:
      - "8183:8182"

  gremlin-server-3:
    image: tinkerpop/gremlin-server:3.8.0
    ports:
      - "8184:8182"
```

```yaml
# application-local.yaml
gremlin:
  cluster:
    hosts:
      - localhost
  # Note: When using different ports, you'll need separate clusters
  # This example assumes all servers use the same port
  port: 8182
```

## Testing

See [ClusterModeConfigurationIntegrationTest](../../src/test/kotlin/com/github/dayanfcosta/gremlin/autoconfigure/ClusterModeConfigurationIntegrationTest.kt) for integration testing examples using Testcontainers with multiple containers.
