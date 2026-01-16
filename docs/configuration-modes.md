# Configuration Modes

The Spring Boot Starter for Gremlin supports three connection modes to accommodate different deployment architectures. Only one mode can be active at a time.

## Overview

| Mode | Use Case | Configuration Property |
|------|----------|----------------------|
| Simple | Single server, development, testing | `gremlin.host` |
| Cluster | Multiple servers, load balancing, high availability | `gremlin.cluster.hosts` |
| Writer/Reader | Read replicas, write/read separation | `gremlin.writer` + `gremlin.readers` |

---

## Simple Mode

The simplest configuration for connecting to a single Gremlin Server instance.

**When to use:**
- Local development
- Testing environments
- Single-node deployments

**Configuration:**

```yaml
gremlin:
  host: localhost
  port: 8182
```

**Provided Beans:**
- `Cluster` - Connection manager
- `Client` - Request executor
- `GraphTraversalSource` - Graph traversal API

**Example:**

```kotlin
@Service
class PersonService(
    private val graph: GraphTraversalSource
) {
    fun findAll(): List<Vertex> {
        return graph.V().hasLabel("person").toList()
    }
}
```

---

## Cluster Mode

Connects to multiple Gremlin Server instances with automatic load balancing using round-robin strategy.

**When to use:**
- Production environments with multiple servers
- High availability requirements
- Horizontal scaling

**Configuration:**

```yaml
gremlin:
  cluster:
    hosts:
      - gremlin-server-1.example.com
      - gremlin-server-2.example.com
      - gremlin-server-3.example.com
  port: 8182
```

**Behavior:**
- Requests are distributed across all hosts using round-robin
- Failed hosts are automatically detected and removed from rotation
- Reconnection attempts occur at the configured `reconnect-interval`

**Provided Beans:**
- `Cluster` - Connection manager with load balancing
- `Client` - Request executor
- `GraphTraversalSource` - Graph traversal API

---

## Writer/Reader Mode

Separates write and read operations across dedicated server instances. Useful for architectures with a primary writer and multiple read replicas.

**When to use:**
- Graph databases with replication (e.g., Amazon Neptune, JanusGraph with replicas)
- Read-heavy workloads requiring read scaling
- Workloads requiring write/read isolation

**Configuration:**

```yaml
gremlin:
  writer: gremlin-writer.example.com
  readers:
    - gremlin-reader-1.example.com
    - gremlin-reader-2.example.com
  port: 8182
```

**Behavior:**
- Write operations go to the single `writer` host
- Read operations are load-balanced across `readers` using round-robin
- Each endpoint has its own connection pool

**Provided Beans:**
- `Cluster` (qualified: `gremlinWriter`) - Writer connection manager
- `Cluster` (qualified: `gremlinReader`) - Reader connection manager with load balancing
- `Client` (qualified: `gremlinWriter`) - Writer request executor
- `Client` (qualified: `gremlinReader`) - Reader request executor
- `GraphTraversalSource` (qualified: `gremlinWriter`) - Writer traversal API
- `GraphTraversalSource` (qualified: `gremlinReader`) - Reader traversal API

**Example:**

```kotlin
@Service
class PersonService(
    @Qualifier("gremlinWriter")
    private val writer: GraphTraversalSource,

    @Qualifier("gremlinReader")
    private val reader: GraphTraversalSource
) {
    fun create(name: String): Vertex {
        return writer.addV("person")
            .property("name", name)
            .next()
    }

    fun findByName(name: String): List<Vertex> {
        return reader.V()
            .hasLabel("person")
            .has("name", name)
            .toList()
    }
}
```

---

## Mode Detection

The starter automatically detects which mode to use based on the properties you configure:

1. If `gremlin.host` is set → **Simple Mode**
2. If `gremlin.cluster.hosts` is set → **Cluster Mode**
3. If `gremlin.writer` and `gremlin.readers` are set → **Writer/Reader Mode**

Configuring properties from multiple modes will result in a startup error.

---

## Common Configuration

The following properties apply to all modes:

```yaml
gremlin:
  port: 8182                    # Server port (default: 8182)
  username: myuser              # Authentication username
  password: mypassword          # Authentication password
  enable-ssl: true              # Enable SSL/TLS
  serializer: GRAPHBINARY_V1    # Message serializer
  connection-timeout: 5s        # Connection timeout
  request-timeout: 30s          # Request timeout
```

See [Configuration Reference](configuration-reference.md) for the complete list of properties.
