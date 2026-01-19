package io.github.dayanfcosta.gremlin.autoconfigure.logging

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GremlinQueryLoggingAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(GremlinQueryLoggingAutoConfiguration::class.java))

    @Test
    fun `should not create auto-configuration when logging is disabled`() {
        contextRunner
            .withPropertyValues("gremlin.logging.enabled=false")
            .run { context ->
                assertFalse(context.containsBean("gremlinQueryLoggingAspect"))
            }
    }

    @Test
    fun `should not create auto-configuration when logging property is not set`() {
        contextRunner
            .run { context ->
                assertFalse(context.containsBean("gremlinQueryLoggingAspect"))
            }
    }

    @Test
    fun `should create aspect bean when logging is enabled`() {
        contextRunner
            .withPropertyValues("gremlin.logging.enabled=true")
            .run { context ->
                assertNotNull(context.getBean(GremlinQueryLoggingAspect::class.java))
            }
    }

    @Test
    fun `should create properties bean with defaults when logging is enabled`() {
        contextRunner
            .withPropertyValues("gremlin.logging.enabled=true")
            .run { context ->
                val properties = context.getBean(GremlinQueryLoggingProperties::class.java)
                assertNotNull(properties)
                assertTrue(properties.enabled)
                assertEquals(Duration.ofSeconds(1), properties.slowQueryThreshold)
                assertFalse(properties.includeBytecode)
            }
    }

    @Test
    fun `should configure properties with custom values`() {
        contextRunner
            .withPropertyValues(
                "gremlin.logging.enabled=true",
                "gremlin.logging.slow-query-threshold=2s",
                "gremlin.logging.include-bytecode=true"
            )
            .run { context ->
                val properties = context.getBean(GremlinQueryLoggingProperties::class.java)
                assertNotNull(properties)
                assertEquals(Duration.ofSeconds(2), properties.slowQueryThreshold)
                assertTrue(properties.includeBytecode)
            }
    }
}
