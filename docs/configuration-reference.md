# Configuration Reference

Complete reference of all configuration properties for the Spring Boot Starter for Gremlin.

## Core Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `gremlin.host` | `String` | - | Gremlin Server host address for simple single-host mode. Mutually exclusive with `cluster` and `writer/readers` configurations. |
| `gremlin.port` | `Integer` | `8182` | Port that the Gremlin Server is listening on. |
| `gremlin.username` | `String` | - | Username for authentication to Gremlin Server. Must be used together with `password`. |
| `gremlin.password` | `String` | - | Password for authentication to Gremlin Server. Must be used together with `username`. |
| `gremlin.enable-ssl` | `Boolean` | `false` | Enable SSL/TLS connectivity. The Gremlin Server must be configured with SSL enabled. |
| `gremlin.serializer` | `Serializers` | `GRAPHBINARY_V1` | Message serializer to use for communication with Gremlin Server. |
| `gremlin.connection-timeout` | `Duration` | `5s` | Maximum time to wait when establishing a connection to the Gremlin Server. |
| `gremlin.request-timeout` | `Duration` | `30s` | Maximum time to wait for a request to complete. |
| `gremlin.keep-alive-interval` | `Duration` | - | Time to wait on an idle connection before sending a keep-alive request. If not set, keep-alive is disabled. |
| `gremlin.channelizer` | `Class` | `WebSocketChannelizer` | Channelizer implementation to use for the connection. |

---

## Cluster Mode Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `gremlin.cluster.hosts` | `List<String>` | - | List of Gremlin Server host addresses for cluster mode. Requests are load-balanced across all hosts using round-robin strategy. |

---

## Writer/Reader Mode Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `gremlin.writer` | `String` | - | Gremlin Server host address dedicated for write operations. Requires `readers` to also be configured. |
| `gremlin.readers` | `List<String>` | - | List of Gremlin Server host addresses dedicated for read operations. Requests are load-balanced across all readers. Requires `writer` to also be configured. |

---

## Connection Pool Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `gremlin.connection-pool.min-connections-per-host` | `Integer` | `2` | Minimum size of the connection pool. Connections are created to this size when the client starts. |
| `gremlin.connection-pool.max-connections-per-host` | `Integer` | `8` | Maximum size that the connection pool can grow to. |
| `gremlin.connection-pool.max-simultaneous-usage-per-connection` | `Integer` | `16` | Maximum number of times a connection can be borrowed from the pool simultaneously. |
| `gremlin.connection-pool.max-in-process-per-connection` | `Integer` | `4` | Maximum number of in-flight requests allowed on a single connection. |
| `gremlin.connection-pool.max-wait-for-connection` | `Duration` | `3s` | Maximum time to wait for a connection to be borrowed from the pool. |
| `gremlin.connection-pool.max-wait-for-close` | `Duration` | `5s` | Maximum time to wait for a connection to close gracefully before timing out. |
| `gremlin.connection-pool.max-content-length` | `Integer` | `65536` | Maximum size in bytes of any request sent to the server. |
| `gremlin.connection-pool.result-iteration-batch-size` | `Integer` | `64` | Number of results to return per batch from the server. |
| `gremlin.connection-pool.reconnect-interval` | `Duration` | `1s` | Time to wait between retries when attempting to reconnect to a dead host. |

---

## Serializer Options

The `gremlin.serializer` property accepts the following values:

| Value | Description |
|-------|-------------|
| `GRAPHBINARY_V1` | GraphBinary format version 1 (recommended, most efficient). |
| `GRAPHSON_V3` | GraphSON format version 3 (JSON-based, human-readable). |
| `GRAPHSON_V2` | GraphSON format version 2 (legacy JSON format). |
| `GRAPHSON_V1` | GraphSON format version 1 (legacy JSON format). |

---

## Channelizer Options

The `gremlin.channelizer` property accepts the following values:

| Value | Description |
|-------|-------------|
| `org.apache.tinkerpop.gremlin.driver.Channelizer$WebSocketChannelizer` | WebSocket transport protocol (default). |
| `org.apache.tinkerpop.gremlin.driver.Channelizer$HttpChannelizer` | HTTP transport protocol. |

---

## Duration Format

Properties of type `Duration` accept values in the following formats:

- `5s` - 5 seconds
- `500ms` - 500 milliseconds
- `1m` - 1 minute
- `PT30S` - ISO-8601 format (30 seconds)

---

## Example Configuration

### Minimal (Simple Mode)

```yaml
gremlin:
  host: localhost
```

### Full Configuration (Cluster Mode with SSL)

```yaml
gremlin:
  cluster:
    hosts:
      - gremlin-1.example.com
      - gremlin-2.example.com
  port: 8182
  username: admin
  password: ${GREMLIN_PASSWORD}
  enable-ssl: true
  serializer: GRAPHBINARY_V1
  connection-timeout: 10s
  request-timeout: 60s
  keep-alive-interval: 30s
  connection-pool:
    min-connections-per-host: 4
    max-connections-per-host: 16
    max-simultaneous-usage-per-connection: 32
    max-in-process-per-connection: 8
    max-wait-for-connection: 5s
    max-wait-for-close: 10s
    reconnect-interval: 2s
```
