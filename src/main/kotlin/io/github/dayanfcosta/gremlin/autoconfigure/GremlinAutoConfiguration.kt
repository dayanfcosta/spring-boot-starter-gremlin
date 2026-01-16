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

@AutoConfiguration
@ConditionalOnClass(Cluster::class)
@EnableConfigurationProperties(GremlinProperties::class)
class GremlinAutoConfiguration {

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

    class ClusterLifecycle(
        private val cluster: Cluster
    ) : DisposableBean {
        override fun destroy() {
            cluster.close()
        }
    }
}
