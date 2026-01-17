package io.github.dayanfcosta.gremlin.test

import io.github.dayanfcosta.gremlin.autoconfigure.GremlinAutoConfiguration
import org.apache.tinkerpop.gremlin.driver.Client
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.springframework.beans.factory.DisposableBean
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

/**
 * Autoconfiguration that creates Gremlin beans from [GremlinConnectionDetails].
 *
 * This configuration is activated when a [GremlinConnectionDetails] bean
 * is present (typically created by `@ServiceConnection` with Testcontainers).
 *
 * Takes precedence over both the main starter's autoconfiguration and
 * the embedded TinkerGraph configuration, ensuring that when a container
 * is used, connections go to the container instead of an embedded graph.
 *
 * @see GremlinConnectionDetails
 * @see GremlinContainerConnectionDetailsFactory
 */
@AutoConfiguration(
    before = [
        GremlinAutoConfiguration::class,
        EmbeddedGremlinAutoConfiguration::class
    ]
)
@ConditionalOnClass(Cluster::class, GremlinConnectionDetails::class)
@ConditionalOnBean(GremlinConnectionDetails::class)
class GremlinConnectionDetailsAutoConfiguration {

    /**
     * Creates a [Cluster] from the connection details.
     *
     * @param connectionDetails The connection details from the container
     * @return A configured Cluster instance
     */
    @Bean
    @ConditionalOnMissingBean
    fun gremlinCluster(connectionDetails: GremlinConnectionDetails): Cluster {
        val builder = Cluster.build()
            .addContactPoint(connectionDetails.getHost())
            .port(connectionDetails.getPort())
            .enableSsl(connectionDetails.isSslEnabled())

        val username = connectionDetails.getUsername()
        val password = connectionDetails.getPassword()
        if (username != null && password != null) {
            builder.credentials(username, password)
        }

        return builder.create()
    }

    /**
     * Creates a [Client] from the cluster.
     *
     * @param cluster The Gremlin cluster
     * @return A connected Client instance
     */
    @Bean
    @ConditionalOnMissingBean
    fun gremlinClient(cluster: Cluster): Client {
        return cluster.connect()
    }

    /**
     * Creates a [GraphTraversalSource] from the cluster.
     *
     * @param cluster The Gremlin cluster
     * @return A GraphTraversalSource for graph operations
     */
    @Bean
    @ConditionalOnMissingBean
    fun graphTraversalSource(cluster: Cluster): GraphTraversalSource {
        return AnonymousTraversalSource.traversal()
            .with(DriverRemoteConnection.using(cluster))
    }

    /**
     * Manages the lifecycle of the Gremlin cluster.
     *
     * @param cluster The Gremlin cluster to manage
     * @return A lifecycle bean that closes the cluster on shutdown
     */
    @Bean
    fun gremlinTestClusterLifecycle(cluster: Cluster): ClusterLifecycle {
        return ClusterLifecycle(cluster)
    }

    /**
     * Lifecycle manager for Gremlin [Cluster] instances.
     */
    class ClusterLifecycle(
        private val cluster: Cluster
    ) : DisposableBean {
        override fun destroy() {
            cluster.close()
        }
    }
}
