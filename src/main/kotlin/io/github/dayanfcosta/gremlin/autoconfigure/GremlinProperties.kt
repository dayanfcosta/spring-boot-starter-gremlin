package io.github.dayanfcosta.gremlin.autoconfigure

import jakarta.annotation.PostConstruct
import org.apache.tinkerpop.gremlin.driver.Channelizer
import org.apache.tinkerpop.gremlin.util.ser.Serializers
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.validation.annotation.Validated
import java.time.Duration

/**
 * Configuration properties for Apache TinkerPop Gremlin connections.
 *
 * Supports three mutually exclusive connection modes:
 * - **Simple mode**: Single host connection via [host] property
 * - **Cluster mode**: Multiple hosts with load balancing via [cluster] property
 * - **Writer/Reader mode**: Separate write and read endpoints via [writer] and [readers] properties
 *
 * Only one mode can be configured at a time. Attempting to configure multiple modes
 * will result in a validation error at startup.
 *
 * Example configuration for simple mode:
 * ```yaml
 * gremlin:
 *   host: localhost
 *   port: 8182
 * ```
 *
 * Example configuration for cluster mode:
 * ```yaml
 * gremlin:
 *   cluster:
 *     hosts:
 *       - node1.example.com
 *       - node2.example.com
 *   port: 8182
 * ```
 *
 * Example configuration for writer/reader mode:
 * ```yaml
 * gremlin:
 *   writer: writer.example.com
 *   readers:
 *     - reader1.example.com
 *     - reader2.example.com
 *   port: 8182
 * ```
 *
 * @property host Single host address for simple mode connection
 * @property port Gremlin server port. Defaults to 8182
 * @property cluster Configuration for cluster mode with multiple hosts
 * @property writer Writer endpoint for writer/reader mode
 * @property readers List of reader endpoints for writer/reader mode
 * @property username Authentication username (optional)
 * @property password Authentication password (optional)
 * @property enableSsl Whether to enable SSL/TLS. Defaults to false
 * @property serializer Message serializer format. Defaults to GRAPHBINARY_V1
 * @property connectionPool Connection pool configuration
 * @property connectionTimeout Timeout for establishing connections. Defaults to 5 seconds
 * @property requestTimeout Timeout for request execution. Defaults to 30 seconds
 * @property keepAliveInterval Interval for keep-alive messages (optional)
 * @property channelizer Network channelizer class. Defaults to WebSocketChannelizer
 */
@ConfigurationProperties(prefix = "gremlin")
@Validated
data class GremlinProperties(
    // Single-host mode
    val host: String? = null,
    val port: Int = 8182,

    // Cluster mode
    @NestedConfigurationProperty val cluster: ClusterConfig? = null,

    val writer: String? = null,
    val readers: List<String>? = null,

    val username: String? = null,
    val password: String? = null,

    val enableSsl: Boolean = false,
    val serializer: Serializers = Serializers.GRAPHBINARY_V1,

    @NestedConfigurationProperty val connectionPool: ConnectionPoolConfig = ConnectionPoolConfig(),

    val connectionTimeout: Duration = Duration.ofSeconds(5),
    val requestTimeout: Duration = Duration.ofSeconds(30),

    val keepAliveInterval: Duration? = null,
    val channelizer: Class<out Channelizer> = Channelizer.WebSocketChannelizer::class.java
) {

    @PostConstruct
    fun validate() {
        val modes = listOfNotNull(
            if (host != null) Mode.SIMPLE else null,
            if (cluster != null) Mode.CLUSTER else null,
            if (writer != null || !readers.isNullOrEmpty()) Mode.WRITER_READ else null
        )

        require(modes.size == 1) {
            "Only one configuration mode allowed. Found ${modes.joinToString(", ")}"
        }

        if (writer != null || readers != null) {
            require(writer != null && readers != null && !readers.isNullOrEmpty()) {
                "Writer/reader mode requires both writer and at least one reader configured"
            }
        }
    }

    internal fun getMode(): Mode = when {
        writer != null -> Mode.WRITER_READ
        cluster != null -> Mode.CLUSTER
        host != null -> Mode.SIMPLE
        else -> throw IllegalArgumentException("No Gremlin configuration provided")
    }

    /**
     * Configuration for cluster mode with multiple Gremlin server hosts.
     *
     * @property hosts List of host addresses for the cluster nodes
     */
    data class ClusterConfig(
        val hosts: List<String>,
    )

    /**
     * Connection pool configuration for tuning Gremlin driver behavior.
     *
     * These settings control how connections are managed, pooled, and recycled.
     * Adjust these values based on your workload characteristics and server capacity.
     *
     * @property minConnectionsPerHost Minimum connections maintained per host. Defaults to 2
     * @property maxConnectionsPerHost Maximum connections allowed per host. Defaults to 8
     * @property maxSimultaneousUsagePerConnection Maximum concurrent requests per connection. Defaults to 16
     * @property maxInProcessPerConnection Maximum in-flight requests per connection. Defaults to 4
     * @property maxWaitForConnection Maximum wait time for an available connection. Defaults to 3 seconds
     * @property maxWaitForClose Maximum wait time when closing connections. Defaults to 5 seconds
     * @property maxContentLength Maximum message content length in bytes. Defaults to 65536 (64KB)
     * @property resultIterationBatchSize Batch size for result iteration. Defaults to 64
     * @property reconnectInterval Interval between reconnection attempts. Defaults to 1 second
     */
    data class ConnectionPoolConfig(
        val minConnectionsPerHost: Int = 2,
        val maxConnectionsPerHost: Int = 8,
        val maxSimultaneousUsagePerConnection: Int = 16,
        val maxInProcessPerConnection: Int = 4,
        val maxWaitForConnection: Duration = Duration.ofSeconds(3),
        val maxWaitForClose: Duration = Duration.ofSeconds(5),
        val maxContentLength: Int = 65536,
        val resultIterationBatchSize: Int = 64,
        val reconnectInterval: Duration = Duration.ofSeconds(1),
    )
}

/**
 * Enumeration of supported Gremlin connection modes.
 *
 * - [SIMPLE]: Single host connection mode
 * - [CLUSTER]: Multi-host cluster mode with load balancing
 * - [WRITER_READ]: Separate writer and reader endpoints mode
 */
enum class Mode {
    SIMPLE,
    CLUSTER,
    WRITER_READ
}
