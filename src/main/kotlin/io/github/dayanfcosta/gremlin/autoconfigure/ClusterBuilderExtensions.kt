package io.github.dayanfcosta.gremlin.autoconfigure

import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.RequestInterceptor
import org.springframework.beans.factory.ObjectProvider

/**
 * Extension function that applies common configuration to a [Cluster.Builder].
 *
 * This function configures the following settings from [GremlinProperties]:
 * - Authentication credentials (username/password)
 * - SSL/TLS enablement
 * - Serializer configuration
 * - Connection pool settings (min/max connections, timeouts, etc.)
 * - Keep-alive interval
 * - Channelizer (WebSocket or HTTP)
 * - Optional request interceptor for custom request modification
 *
 * @param properties The Gremlin configuration properties
 * @param requestInterceptor Optional request interceptor provider for custom request handling
 * @return The configured [Cluster.Builder] instance for method chaining
 */
internal fun Cluster.Builder.applyCommonConfiguration(
    properties: GremlinProperties,
    requestInterceptor: ObjectProvider<RequestInterceptor>
): Cluster.Builder {
    return apply {
        if(properties.username != null && properties.password != null) {
            credentials(properties.username, properties.password)
        }

        enableSsl(properties.enableSsl)

        serializer(properties.serializer)

        maxConnectionPoolSize(properties.connectionPool.maxConnectionsPerHost)
        minConnectionPoolSize(properties.connectionPool.minConnectionsPerHost)

        maxSimultaneousUsagePerConnection(properties.connectionPool.maxSimultaneousUsagePerConnection)
        maxInProcessPerConnection(properties.connectionPool.maxInProcessPerConnection)
        maxWaitForConnection(properties.connectionPool.maxWaitForConnection.toMillis().toInt())
        maxWaitForClose(properties.connectionPool.maxWaitForClose.toMillis().toInt())
        maxContentLength(properties.connectionPool.maxContentLength)
        resultIterationBatchSize(properties.connectionPool.resultIterationBatchSize)
        reconnectInterval(properties.connectionPool.reconnectInterval.toMillis().toInt())

        properties.keepAliveInterval?.let { keepAliveInterval(it.toMillis()) }
        channelizer(properties.channelizer)

        requestInterceptor.ifAvailable?.let {
            requestInterceptor(it)
        }
    }
}
