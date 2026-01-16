package io.github.dayanfcosta.gremlin.autoconfigure

import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.RequestInterceptor
import org.springframework.beans.factory.ObjectProvider

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
