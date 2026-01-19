package io.github.dayanfcosta.gremlin.autoconfigure.logging

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Configuration properties for Gremlin query logging.
 *
 * Enables developers to debug and monitor Gremlin queries by intercepting
 * query execution and logging details including the query, execution time,
 * and optionally slow query warnings.
 *
 * Example configuration:
 * ```yaml
 * gremlin:
 *   logging:
 *     enabled: true
 *     slow-query-threshold: 1000ms
 *     include-bytecode: false
 *
 * # Set the log level
 * logging:
 *   level:
 *     io.github.dayanfcosta.gremlin.query: DEBUG
 * ```
 *
 * Example log output:
 * ```
 * DEBUG - Gremlin Query: [GraphStep(vertex,[]), HasStep([~label.eq(person)]), CountGlobalStep] | Time: 45ms
 * WARN  - Slow Gremlin Query: [GraphStep(vertex,[]), HasStep([~label.eq(person)])] | Time: 1523ms (threshold: 1000ms)
 * ```
 *
 * @property enabled Whether query logging is enabled. Defaults to false
 * @property slowQueryThreshold Threshold for slow query warnings. Queries exceeding this will be logged at WARN level
 * @property includeBytecode Whether to include raw bytecode details in logs. Defaults to false
 */
@ConfigurationProperties(prefix = "gremlin.logging")
data class GremlinQueryLoggingProperties(
    val enabled: Boolean = false,
    val slowQueryThreshold: Duration = Duration.ofSeconds(1),
    val includeBytecode: Boolean = false
)
