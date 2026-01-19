package io.github.dayanfcosta.gremlin.autoconfigure.logging

import org.apache.tinkerpop.gremlin.driver.Cluster
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy

/**
 * Auto-configuration for Gremlin query logging.
 *
 * When enabled, this configuration provides AOP-based query logging that intercepts
 * all Gremlin queries executed via the [org.apache.tinkerpop.gremlin.driver.Client]
 * and logs them with execution timing.
 *
 * To enable query logging, add to application.yaml:
 * ```yaml
 * gremlin:
 *   logging:
 *     enabled: true
 *     slow-query-threshold: 1000ms  # optional, default 1s
 *     include-bytecode: false       # optional, default false
 *
 * # Enable DEBUG logging to see queries
 * logging:
 *   level:
 *     io.github.dayanfcosta.gremlin.query: DEBUG
 * ```
 *
 * Example log output:
 * ```
 * DEBUG - Gremlin Query: g.V(), hasLabel(person), count() | Time: 45ms
 * WARN  - Slow Gremlin Query: g.V(), has(name, John) | Time: 1523ms (threshold: 1000ms)
 * ```
 *
 * @see GremlinQueryLoggingProperties
 * @see GremlinQueryLoggingAspect
 */
@AutoConfiguration
@ConditionalOnClass(Cluster::class)
@ConditionalOnProperty(prefix = "gremlin.logging", name = ["enabled"], havingValue = "true")
@EnableConfigurationProperties(GremlinQueryLoggingProperties::class)
@EnableAspectJAutoProxy
class GremlinQueryLoggingAutoConfiguration {

    /**
     * Creates the query logging aspect that intercepts Gremlin Client method calls.
     *
     * @param properties The logging configuration properties
     * @return The configured logging aspect
     */
    @Bean
    fun gremlinQueryLoggingAspect(properties: GremlinQueryLoggingProperties): GremlinQueryLoggingAspect {
        return GremlinQueryLoggingAspect(properties)
    }
}
