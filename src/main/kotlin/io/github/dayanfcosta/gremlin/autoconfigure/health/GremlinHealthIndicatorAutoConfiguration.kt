package io.github.dayanfcosta.gremlin.autoconfigure.health

import io.github.dayanfcosta.gremlin.autoconfigure.GremlinAutoConfiguration
import io.github.dayanfcosta.gremlin.autoconfigure.GremlinProperties
import org.apache.tinkerpop.gremlin.driver.Client
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Autoconfiguration for Gremlin health indicator.
 *
 * Creates health indicators for each connection mode:
 * - Simple mode: single "gremlin" health indicator
 * - Cluster mode: single "gremlin" health indicator
 * - Writer/Reader mode: two health indicators ("gremlinWriter" and "gremlinReader")
 *
 * Health indicators can be disabled via `management.health.gremlin.enabled=false`.
 */
@AutoConfiguration(after = [GremlinAutoConfiguration::class])
@ConditionalOnClass(HealthIndicator::class, Client::class)
@ConditionalOnEnabledHealthIndicator("gremlin")
@EnableConfigurationProperties(GremlinHealthProperties::class)
class GremlinHealthIndicatorAutoConfiguration {

    /**
     * Health indicator configuration for Simple mode (single host).
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "gremlin", name = ["host"])
    class SimpleModeHealthConfiguration(
        private val properties: GremlinProperties,
        private val healthProperties: GremlinHealthProperties
    ) {
        @Bean
        @ConditionalOnMissingBean(name = ["gremlinHealthIndicator"])
        fun gremlinHealthIndicator(client: Client): GremlinHealthIndicator {
            return GremlinHealthIndicator(
                client = client,
                connectionName = "default",
                hosts = listOf(properties.host!!),
                port = properties.port,
                timeout = healthProperties.timeout
            )
        }
    }

    /**
     * Health indicator configuration for Cluster mode (multiple hosts).
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "gremlin.cluster", name = ["hosts[0]"])
    class ClusterModeHealthConfiguration(
        private val properties: GremlinProperties,
        private val healthProperties: GremlinHealthProperties
    ) {
        @Bean
        @ConditionalOnMissingBean(name = ["gremlinHealthIndicator"])
        fun gremlinHealthIndicator(client: Client): GremlinHealthIndicator {
            return GremlinHealthIndicator(
                client = client,
                connectionName = "cluster",
                hosts = properties.cluster!!.hosts,
                port = properties.port,
                timeout = healthProperties.timeout
            )
        }
    }

    /**
     * Health indicator configuration for Writer/Reader mode.
     * Creates two separate health indicators for writer and reader connections.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "gremlin", name = ["writer"])
    class WriterReaderModeHealthConfiguration(
        private val properties: GremlinProperties,
        private val healthProperties: GremlinHealthProperties
    ) {
        @Bean
        @ConditionalOnMissingBean(name = ["gremlinWriterHealthIndicator"])
        fun gremlinWriterHealthIndicator(
            @Qualifier("gremlinWriter") client: Client
        ): GremlinHealthIndicator {
            return GremlinHealthIndicator(
                client = client,
                connectionName = "writer",
                hosts = listOf(properties.writer!!),
                port = properties.port,
                timeout = healthProperties.timeout
            )
        }

        @Bean
        @ConditionalOnMissingBean(name = ["gremlinReaderHealthIndicator"])
        fun gremlinReaderHealthIndicator(
            @Qualifier("gremlinReader") client: Client
        ): GremlinHealthIndicator {
            return GremlinHealthIndicator(
                client = client,
                connectionName = "reader",
                hosts = properties.readers!!,
                port = properties.port,
                timeout = healthProperties.timeout
            )
        }
    }
}
