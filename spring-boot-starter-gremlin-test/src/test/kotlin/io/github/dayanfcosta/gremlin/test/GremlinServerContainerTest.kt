package io.github.dayanfcosta.gremlin.test

import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for GremlinServerContainer.
 */
@Testcontainers
class GremlinServerContainerTest {

    companion object {
        @Container
        @JvmStatic
        val gremlinServer = GremlinServerContainer()
    }

    @Test
    fun `should start container and expose port`() {
        assertTrue(gremlinServer.isRunning)
        assertNotNull(gremlinServer.getGremlinPort())
        assertTrue(gremlinServer.getGremlinPort() > 0)
    }

    @Test
    fun `should provide valid Gremlin URL`() {
        val url = gremlinServer.getGremlinUrl()

        assertTrue(url.startsWith("ws://"))
        assertTrue(url.endsWith("/gremlin"))
    }

    @Test
    fun `should connect and execute traversal`() {
        // Given
        val cluster = Cluster.build()
            .addContactPoint(gremlinServer.getGremlinHost())
            .port(gremlinServer.getGremlinPort())
            .create()

        val g = AnonymousTraversalSource.traversal()
            .with(DriverRemoteConnection.using(cluster))

        try {
            // When
            g.addV("test").property("name", "container-test").next()

            // Then
            val count = g.V().hasLabel("test").count().next()
            assertEquals(1, count)

        } finally {
            // Cleanup
            g.V().drop().iterate()
            cluster.close()
        }
    }

    @Test
    fun `should create container with custom tag`() {
        val container = GremlinServerContainer("3.7.3")

        assertEquals("tinkerpop/gremlin-server:3.7.3", container.dockerImageName)
    }
}
