package io.github.dayanfcosta.gremlin.autoconfigure.health

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Configuration properties for Gremlin health indicator.
 *
 * @property timeout Timeout for health check queries. Defaults to 5 seconds.
 */
@ConfigurationProperties(prefix = "management.health.gremlin")
data class GremlinHealthProperties(
    val timeout: Duration = Duration.ofSeconds(5)
)
