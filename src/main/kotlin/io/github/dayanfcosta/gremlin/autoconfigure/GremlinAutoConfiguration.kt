package io.github.dayanfcosta.gremlin.autoconfigure

import org.apache.tinkerpop.gremlin.driver.Client
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.RequestInterceptor
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Auto-configuration for Apache TinkerPop Gremlin connections.
 *
 * This configuration automatically creates the necessary beans for connecting to
 * Gremlin-compatible graph databases based on the application properties.
 *
 * Three connection modes are supported (mutually exclusive):
 *
 * - **Simple mode** ([SimpleModeConfiguration]): Single host connection.
 *   Activated when `gremlin.host` is configured.
 *
 * - **Cluster mode** ([ClusterModeConfiguration]): Multiple hosts with load balancing.
 *   Activated when `gremlin.cluster.hosts` is configured.
 *
 * - **Writer/Reader mode** ([WriterReadModeConfiguration]): Separate write and read endpoints.
 *   Activated when `gremlin.writer` is configured. Creates qualified beans for writer and reader.
 *
 * Each mode provides the following beans:
 * - [Cluster]: The Gremlin driver cluster instance
 * - [Client]: Connected client for executing queries
 * - [GraphTraversalSource]: Traversal source for graph operations
 *
 * For Writer/Reader mode, beans are qualified with `@Qualifier("gremlinWriter")` and
 * `@Qualifier("gremlinReader")` to distinguish between write and read connections.
 *
 * @see GremlinProperties
 * @see SimpleModeConfiguration
 * @see ClusterModeConfiguration
 * @see WriterReadModeConfiguration
 */
@AutoConfiguration
@ConditionalOnClass(Cluster::class)
@EnableConfigurationProperties(GremlinProperties::class)
class GremlinAutoConfiguration {

    /**
     * Configuration for simple mode with a single Gremlin server host.
     *
     * Activated when `gremlin.host` property is configured.
     * Creates a single [Cluster], [Client], and [GraphTraversalSource] bean.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "gremlin", name = ["host"])
    class SimpleModeConfiguration(
        private val properties: GremlinProperties,
        private val requestInterceptor: ObjectProvider<RequestInterceptor>
    ) {

        @Bean
        @ConditionalOnMissingBean
        fun gremlinCluster(): Cluster {
            return Cluster.build()
                .addContactPoint(properties.host!!)
                .port(properties.port)
                .applyCommonConfiguration(properties, requestInterceptor)
                .create()
        }

        @Bean
        @ConditionalOnMissingBean
        fun gremlinClient(cluster: Cluster): Client = cluster.connect()

        @Bean
        @ConditionalOnMissingBean
        fun gremlinTraversalSource(cluster: Cluster): GraphTraversalSource =
            AnonymousTraversalSource.traversal().with(
                DriverRemoteConnection.using(cluster)
            )

        @Bean
        fun gremlinClusterLifecycle(cluster: Cluster): ClusterLifecycle = ClusterLifecycle(cluster)
    }

    /**
     * Configuration for cluster mode with multiple Gremlin server hosts.
     *
     * Activated when `gremlin.cluster.hosts` property is configured.
     * Creates a single [Cluster] with multiple contact points for load balancing,
     * along with [Client] and [GraphTraversalSource] beans.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "gremlin.cluster", name = ["hosts[0]"])
    class ClusterModeConfiguration(
        private val properties: GremlinProperties,
        private val requestInterceptor: ObjectProvider<RequestInterceptor>
    ) {
        @Bean
        @ConditionalOnMissingBean
        fun gremlinCluster(): Cluster {
            return Cluster.build()
                .addContactPoints(*properties.cluster!!.hosts.toTypedArray())
                .port(properties.port)
                .applyCommonConfiguration(properties, requestInterceptor)
                .create()
        }

        @Bean
        @ConditionalOnMissingBean
        fun gremlinClient(cluster: Cluster): Client = cluster.connect()

        @Bean
        @ConditionalOnMissingBean
        fun gremlinTraversalSource(cluster: Cluster): GraphTraversalSource =
            AnonymousTraversalSource.traversal().with(
                DriverRemoteConnection.using(cluster)
            )

        @Bean
        fun gremlinClusterLifecycle(cluster: Cluster): ClusterLifecycle = ClusterLifecycle(cluster)
    }

    /**
     * Configuration for writer/reader mode with separate write and read endpoints.
     *
     * Activated when `gremlin.writer` property is configured.
     * Creates separate [Cluster], [Client], and [GraphTraversalSource] beans for
     * writer and reader connections, qualified with `@Qualifier("gremlinWriter")`
     * and `@Qualifier("gremlinReader")` respectively.
     *
     * This mode is useful for graph databases that support read replicas,
     * such as Amazon Neptune or Azure Cosmos DB.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "gremlin", name = ["writer"])
    class WriterReadModeConfiguration(
        private val properties: GremlinProperties,
        private val requestInterceptor: ObjectProvider<RequestInterceptor>
    ) {
        @Bean
        @ConditionalOnMissingBean(name = ["gremlinWriterCluster"])
        @Qualifier("gremlinWriter")
        fun gremlinWriterCluster(): Cluster {
            return Cluster.build()
                .addContactPoint(properties.writer!!)
                .port(properties.port)
                .applyCommonConfiguration(properties, requestInterceptor)
                .create()
        }

        @Bean
        @ConditionalOnMissingBean(name = ["gremlinReaderCluster"])
        @Qualifier("gremlinReader")
        fun gremlinReaderCluster(): Cluster {
            return Cluster.build()
                .addContactPoints(*properties.readers!!.toTypedArray())
                .port(properties.port)
                .applyCommonConfiguration(properties, requestInterceptor)

                .create()
        }

        @Bean
        @ConditionalOnMissingBean(name = ["gremlinWriterClient"])
        @Qualifier("gremlinWriter")
        fun gremlinWriterClient(@Qualifier("gremlinWriter") cluster: Cluster): Client =
            cluster.connect()

        @Bean
        @ConditionalOnMissingBean(name = ["gremlinReaderClient"])
        @Qualifier("gremlinReader")
        fun gremlinReaderClient(@Qualifier("gremlinReader") cluster: Cluster): Client =
            cluster.connect()

        @Bean
        @ConditionalOnMissingBean(name = ["gremlinWriterTraversalSource"])
        @Qualifier("gremlinWriter")
        fun gremlinWriterTraversalSource(@Qualifier("gremlinWriter") cluster: Cluster): GraphTraversalSource =
            AnonymousTraversalSource.traversal().with(
                DriverRemoteConnection.using(cluster)
            )

        @Bean
        @ConditionalOnMissingBean(name = ["gremlinReaderTraversalSource"])
        @Qualifier("gremlinReader")
        fun gremlinReaderTraversalSource(@Qualifier("gremlinReader") cluster: Cluster): GraphTraversalSource =
            AnonymousTraversalSource.traversal().with(
                DriverRemoteConnection.using(cluster)
            )

        @Bean
        fun gremlinWriterClusterLifecycle(@Qualifier("gremlinWriter") cluster: Cluster): ClusterLifecycle =
            ClusterLifecycle(cluster)

        @Bean
        fun gremlinReaderClusterLifecycle(@Qualifier("gremlinReader") cluster: Cluster): ClusterLifecycle =
            ClusterLifecycle(cluster)
    }

    /**
     * Lifecycle manager for Gremlin [Cluster] instances.
     *
     * Implements [DisposableBean] to ensure proper cleanup of cluster connections
     * when the Spring application context is closed.
     *
     * @property cluster The Gremlin cluster instance to manage
     */
    class ClusterLifecycle(
        private val cluster: Cluster
    ) : DisposableBean {
        override fun destroy() {
            cluster.close()
        }
    }
}
