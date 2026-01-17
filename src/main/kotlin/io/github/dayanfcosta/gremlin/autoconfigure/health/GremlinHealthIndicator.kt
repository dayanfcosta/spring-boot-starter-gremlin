package io.github.dayanfcosta.gremlin.autoconfigure.health

import org.apache.tinkerpop.gremlin.driver.Client
import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Health indicator for Apache TinkerPop Gremlin connections.
 *
 * Performs a lightweight connectivity check using g.inject(1) traversal
 * which executes in constant time regardless of graph size.
 *
 * @property client The Gremlin client to check
 * @property connectionName A name identifying this connection (e.g., "default", "writer", "reader")
 * @property hosts The list of hosts this client connects to
 * @property port The port used for connection
 * @property timeout Maximum time to wait for health check query
 */
class GremlinHealthIndicator(
    private val client: Client,
    private val connectionName: String,
    private val hosts: List<String>,
    private val port: Int,
    private val timeout: Duration
) : AbstractHealthIndicator("Gremlin health check failed") {

    companion object {
        private const val HEALTH_CHECK_QUERY = "g.inject(1)"
    }

    override fun doHealthCheck(builder: Health.Builder) {
        val startTime = System.nanoTime()

        try {
            val resultSet = client.submit(HEALTH_CHECK_QUERY)
            resultSet.all().get(timeout.toMillis(), TimeUnit.MILLISECONDS)

            val latencyMs = (System.nanoTime() - startTime) / 1_000_000

            builder.up()
                .withDetail("connection", connectionName)
                .withDetail("hosts", hosts)
                .withDetail("port", port)
                .withDetail("latency", "${latencyMs}ms")

        } catch (e: TimeoutException) {
            val latencyMs = (System.nanoTime() - startTime) / 1_000_000
            builder.down()
                .withDetail("connection", connectionName)
                .withDetail("hosts", hosts)
                .withDetail("port", port)
                .withDetail("latency", "${latencyMs}ms")
                .withDetail("error", "Health check timed out after ${timeout.toMillis()}ms")
                .withException(e)

        } catch (e: Exception) {
            val latencyMs = (System.nanoTime() - startTime) / 1_000_000
            builder.down()
                .withDetail("connection", connectionName)
                .withDetail("hosts", hosts)
                .withDetail("port", port)
                .withDetail("latency", "${latencyMs}ms")
                .withDetail("error", e.message ?: "Unknown error")
                .withException(e)
        }
    }
}
