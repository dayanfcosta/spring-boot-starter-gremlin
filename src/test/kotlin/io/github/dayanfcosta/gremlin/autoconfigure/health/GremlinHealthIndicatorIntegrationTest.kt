package io.github.dayanfcosta.gremlin.autoconfigure.health

import io.github.dayanfcosta.gremlin.autoconfigure.GremlinAutoConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.health.Status
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Testcontainers
@SpringBootTest(
    classes = [
        GremlinAutoConfiguration::class,
        GremlinHealthIndicatorAutoConfiguration::class
    ]
)
class GremlinHealthIndicatorIntegrationTest {

    companion object {
        private const val GREMLIN_PORT = 8182

        @Container
        @JvmStatic
        val gremlinServer: GenericContainer<*> = GenericContainer(
            DockerImageName.parse("tinkerpop/gremlin-server:3.8.0")
        ).withExposedPorts(GREMLIN_PORT)
            .waitingFor(Wait.forLogMessage(".*Gremlin Server configured with worker.*\\n", 1))
            .withReuse(true)

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("gremlin.host") { gremlinServer.host }
            registry.add("gremlin.port") { gremlinServer.getMappedPort(GREMLIN_PORT) }
        }
    }

    @Autowired
    private lateinit var healthIndicator: HealthIndicator

    @Test
    fun `should report UP when Gremlin server is healthy`() {
        val health: Health = healthIndicator.health()

        assertEquals(Status.UP, health.status)
        assertNotNull(health.details["latency"])
        assertEquals(listOf(gremlinServer.host), health.details["hosts"])
        assertEquals("default", health.details["connection"])
    }

    @Test
    fun `should include port in health details`() {
        val health: Health = healthIndicator.health()

        assertEquals(gremlinServer.getMappedPort(GREMLIN_PORT), health.details["port"])
    }

    @Test
    fun `should report latency in milliseconds format`() {
        val health: Health = healthIndicator.health()

        val latency = health.details["latency"] as String
        assertTrue(latency.matches(Regex("\\d+ms")), "Latency should be in format '123ms', got: $latency")
    }
}
