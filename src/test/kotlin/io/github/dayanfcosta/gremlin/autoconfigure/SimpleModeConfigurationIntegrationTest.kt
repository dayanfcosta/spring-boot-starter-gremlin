package io.github.dayanfcosta.gremlin.autoconfigure

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import kotlin.test.assertEquals

@Testcontainers
@SpringBootTest(classes = [GremlinAutoConfiguration::class])
class SimpleModeConfigurationIntegrationTest {

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
    private lateinit var graph: GraphTraversalSource

    @Test
    fun `should use user-defined Cluster bean instead of auto-configured`() {
        val count = graph.V().count().next()

        assertEquals(0, count)
    }
}
