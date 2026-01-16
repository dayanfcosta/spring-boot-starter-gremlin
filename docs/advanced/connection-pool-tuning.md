# Connection Pool Tuning

This guide explains how to tune the connection pool for optimal performance in different scenarios.

## Connection Pool Architecture

The Gremlin driver maintains a pool of connections to each host. Understanding the pool parameters helps optimize for your workload.

```
┌─────────────────────────────────────────────────────────┐
│                    Connection Pool                       │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐      ┌─────────┐ │
│  │ Conn 1  │  │ Conn 2  │  │ Conn 3  │ ...  │ Conn N  │ │
│  │         │  │         │  │         │      │         │ │
│  │ req req │  │ req req │  │ req     │      │         │ │
│  │ req req │  │ req     │  │         │      │         │ │
│  └─────────┘  └─────────┘  └─────────┘      └─────────┘ │
│      ▲                                                   │
│      │ maxInProcessPerConnection (requests per conn)     │
│                                                          │
│  ◄────────────────────────────────────────────────────► │
│   minConnectionsPerHost ◄──────► maxConnectionsPerHost   │
└─────────────────────────────────────────────────────────┘
```

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `min-connections-per-host` | 2 | Initial pool size per host |
| `max-connections-per-host` | 8 | Maximum pool size per host |
| `max-simultaneous-usage-per-connection` | 16 | Max times a connection can be borrowed at once |
| `max-in-process-per-connection` | 4 | Max concurrent requests per connection |
| `max-wait-for-connection` | 3s | Max wait time to borrow a connection |
| `max-wait-for-close` | 5s | Max wait time for graceful connection close |
| `reconnect-interval` | 1s | Retry interval for dead hosts |
| `max-content-length` | 65536 | Max request size in bytes |
| `result-iteration-batch-size` | 64 | Results per batch from server |

## Tuning for Different Workloads

### Low-Traffic Application

For applications with few concurrent requests:

```yaml
gremlin:
  connection-pool:
    min-connections-per-host: 1
    max-connections-per-host: 4
    max-simultaneous-usage-per-connection: 8
    max-in-process-per-connection: 2
```

### High-Traffic Application

For applications with many concurrent requests:

```yaml
gremlin:
  connection-pool:
    min-connections-per-host: 8
    max-connections-per-host: 32
    max-simultaneous-usage-per-connection: 32
    max-in-process-per-connection: 8
    max-wait-for-connection: 10s
```

### Batch Processing

For applications that run large batch operations:

```yaml
gremlin:
  connection-pool:
    min-connections-per-host: 4
    max-connections-per-host: 16
    max-in-process-per-connection: 16
    max-content-length: 1048576  # 1MB for large requests
    result-iteration-batch-size: 256  # Larger batches for bulk reads

  # Longer timeouts for batch operations
  request-timeout: 300s
```

### Latency-Sensitive Application

For applications requiring fast response times:

```yaml
gremlin:
  connection-pool:
    min-connections-per-host: 8    # Pre-warm more connections
    max-connections-per-host: 16
    max-wait-for-connection: 1s    # Fail fast
    max-in-process-per-connection: 2  # Less contention per connection

  connection-timeout: 2s
  request-timeout: 10s
```

## Understanding the Parameters

### min-connections-per-host

Connections created at startup. Higher values reduce latency for initial requests but consume more resources.

**Trade-off:**
- Higher: Faster initial requests, more idle connections
- Lower: Slower initial requests, fewer resources used

### max-connections-per-host

Maximum connections that can be created. Limits resource usage under high load.

**Trade-off:**
- Higher: Better throughput under load, more server connections
- Lower: Limited throughput, fewer server connections

### max-simultaneous-usage-per-connection

How many times a connection can be borrowed simultaneously from the pool. Controls connection reuse.

**Trade-off:**
- Higher: More reuse, potential queuing
- Lower: Less reuse, more connections needed

### max-in-process-per-connection

Maximum concurrent requests on a single connection. The Gremlin driver multiplexes requests over WebSocket connections.

**Trade-off:**
- Higher: Better connection utilization, potential head-of-line blocking
- Lower: Less efficient, more predictable latency

### max-wait-for-connection

How long to wait for a connection from the pool. Affects behavior under high load.

**Trade-off:**
- Higher: Requests queue longer, less likely to fail
- Lower: Faster failures, better for latency-sensitive apps

### reconnect-interval

How often to retry connecting to a dead host.

**Trade-off:**
- Higher: Less reconnection overhead, slower recovery
- Lower: Faster recovery, more reconnection attempts

### result-iteration-batch-size

Number of results returned per round-trip from the server.

**Trade-off:**
- Higher: Fewer round-trips, more memory per batch
- Lower: More round-trips, less memory per batch

## Calculating Pool Size

A rough formula for `max-connections-per-host`:

```
maxConnections = (expectedConcurrentRequests / maxInProcessPerConnection) + buffer
```

For example:
- Expected concurrent requests: 100
- maxInProcessPerConnection: 4
- Buffer: 25%

```
maxConnections = (100 / 4) * 1.25 = 31.25 ≈ 32
```

## Monitoring Pool Health

Access pool metrics programmatically:

```kotlin
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.springframework.stereotype.Component

@Component
class PoolMonitor(
    private val cluster: Cluster
) {
    fun getPoolStats(): Map<String, Any> {
        val hosts = cluster.allHosts()
        return mapOf(
            "totalHosts" to hosts.size,
            "availableHosts" to cluster.availableHosts().size,
            "hosts" to hosts.map { host ->
                mapOf(
                    "address" to host.address.toString(),
                    "isAvailable" to host.isAvailable
                )
            }
        )
    }
}
```

## Common Issues

### Connection Exhaustion

**Symptom:** `TimeoutException` when borrowing connections

**Solutions:**
1. Increase `max-connections-per-host`
2. Increase `max-wait-for-connection`
3. Reduce request duration (optimize queries)
4. Ensure traversals are properly terminated (call `.iterate()`, `.next()`, `.toList()`)

### Slow Connection Creation

**Symptom:** High latency on first requests after startup

**Solutions:**
1. Increase `min-connections-per-host` to pre-warm pool
2. Implement application warmup that executes initial queries

### Memory Issues

**Symptom:** `OutOfMemoryError` during large result sets

**Solutions:**
1. Reduce `result-iteration-batch-size`
2. Use pagination in queries (`.range()` step)
3. Reduce `max-content-length`

### Connection Timeouts

**Symptom:** Frequent connection timeouts

**Solutions:**
1. Increase `connection-timeout`
2. Check network latency to Gremlin Server
3. Verify server is not overloaded

## Example: Calculating Settings

Given:
- 50 concurrent users
- Average query time: 100ms
- Peak query time: 2s
- 3 Gremlin Server hosts

Calculate settings:

```yaml
gremlin:
  cluster:
    hosts:
      - gremlin-1.example.com
      - gremlin-2.example.com
      - gremlin-3.example.com

  connection-pool:
    # Per-host settings
    # 50 users / 3 hosts = ~17 users per host
    # With maxInProcess=4: 17/4 = 4.25, round up with buffer
    min-connections-per-host: 4
    max-connections-per-host: 8

    # Allow some multiplexing but not too much
    max-in-process-per-connection: 4
    max-simultaneous-usage-per-connection: 16

    # Wait up to peak time before failing
    max-wait-for-connection: 3s

  # Request timeout should exceed peak query time
  request-timeout: 5s
```
