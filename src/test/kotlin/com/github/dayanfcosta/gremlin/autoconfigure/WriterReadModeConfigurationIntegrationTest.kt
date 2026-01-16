package com.github.dayanfcosta.gremlin.autoconfigure

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
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
class WriterReadModeConfigurationIntegrationTest {

    companion object {
        private const val GREMLIN_PORT = 8182

        @Container
        @JvmStatic
        val writerServer: GenericContainer<*> = GenericContainer(
            DockerImageName.parse("tinkerpop/gremlin-server:3.8.0")
        ).withExposedPorts(GREMLIN_PORT)
            .waitingFor(Wait.forLogMessage(".*Gremlin Server configured with worker.*\\n", 1))
            .withReuse(true)

        @Container
        @JvmStatic
        val readerServer1: GenericContainer<*> = GenericContainer(
            DockerImageName.parse("tinkerpop/gremlin-server:3.8.0")
        ).withExposedPorts(GREMLIN_PORT)
            .waitingFor(Wait.forLogMessage(".*Gremlin Server configured with worker.*\\n", 1))
            .withReuse(true)

        @Container
        @JvmStatic
        val readerServer2: GenericContainer<*> = GenericContainer(
            DockerImageName.parse("tinkerpop/gremlin-server:3.8.0")
        ).withExposedPorts(GREMLIN_PORT)
            .waitingFor(Wait.forLogMessage(".*Gremlin Server configured with worker.*\\n", 1))
            .withReuse(true)

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("gremlin.writer") { writerServer.host }
            registry.add("gremlin.readers[0]") { readerServer1.host }
            registry.add("gremlin.readers[1]") { readerServer2.host }
            registry.add("gremlin.port") { writerServer.getMappedPort(GREMLIN_PORT) }
        }
    }

    @Autowired
    @Qualifier("gremlinWriter")
    private lateinit var writerGraph: GraphTraversalSource

    @Autowired
    @Qualifier("gremlinReader")
    private lateinit var readerGraph: GraphTraversalSource

    @Test
    fun `should execute traversal on writer`() {
        val count = writerGraph.V().count().next()

        assertEquals(0, count)
    }

    @Test
    fun `should execute traversal on reader`() {
        val count = readerGraph.V().count().next()

        assertEquals(0, count)
    }
}
