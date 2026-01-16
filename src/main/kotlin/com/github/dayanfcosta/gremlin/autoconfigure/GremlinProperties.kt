package com.github.dayanfcosta.gremlin.autoconfigure

import jakarta.annotation.PostConstruct
import org.apache.tinkerpop.gremlin.driver.Channelizer
import org.apache.tinkerpop.gremlin.util.ser.Serializers
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.validation.annotation.Validated
import java.time.Duration

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

    data class ClusterConfig(
        val hosts: List<String>,
    )

    data class ConnectionPoolConfig(
        val minConnectionsPerHost: Int = 2,
        val maxConnectionsPerHost: Int = 8,
        val maxSimultaneousUsagePerConnection: Int = 16,
        val maxInProcessPerConnection: Int = 4,
        val maxWaitForConnection: Duration = Duration.ofSeconds(3),
        val maxWaitForClose: Duration = Duration.ofSeconds(5),
        val maxContentLength: Int = 65536, // 64KB
        val resultIterationBatchSize: Int = 64,
        val reconnectInterval: Duration = Duration.ofSeconds(1),
    )
}

@JvmInline
value class Host(val value: String) {
    init {
        require(value.isNotBlank()) { "The host name must not be blank" }
    }
}

enum class Mode {
    SIMPLE,
    CLUSTER,
    WRITER_READ
}
