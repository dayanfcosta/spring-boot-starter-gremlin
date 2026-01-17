package io.github.dayanfcosta.gremlin.test

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for @GremlinTest with embedded TinkerGraph.
 */
@GremlinTest
class EmbeddedGremlinTest {

    @Autowired
    private lateinit var g: GraphTraversalSource

    @AfterEach
    fun cleanup() {
        GremlinTestUtils.clearGraph(g)
    }

    @Test
    fun `should inject GraphTraversalSource`() {
        assertNotNull(g)
    }

    @Test
    fun `should create and count vertices`() {
        // Given
        g.addV("person").property("name", "John").next()
        g.addV("person").property("name", "Jane").next()

        // When
        val count = GremlinTestUtils.countVertices(g, "person")

        // Then
        assertEquals(2, count)
    }

    @Test
    fun `should create vertex using test utils`() {
        // When
        val vertex = GremlinTestUtils.createVertex(g, "person", "name" to "John", "age" to 30)

        // Then
        assertNotNull(vertex)
        assertTrue(GremlinTestUtils.vertexExists(g, "person", "name" to "John"))
    }

    @Test
    fun `should create edge between vertices`() {
        // Given
        val john = GremlinTestUtils.createVertex(g, "person", "name" to "John")
        val jane = GremlinTestUtils.createVertex(g, "person", "name" to "Jane")

        // When
        GremlinTestUtils.createEdge(g, john, "knows", jane, "since" to 2020)

        // Then
        assertEquals(1, GremlinTestUtils.countEdges(g, "knows"))
        val friends = g.V(john.id()).out("knows").toList()
        assertEquals(1, friends.size)
    }

    @Test
    fun `should find vertex by properties`() {
        // Given
        GremlinTestUtils.createVertex(g, "person", "name" to "John", "age" to 30)
        GremlinTestUtils.createVertex(g, "person", "name" to "Jane", "age" to 25)

        // When
        val vertex = GremlinTestUtils.findVertex(g, "person", "name" to "John")

        // Then
        assertNotNull(vertex)
        assertEquals("John", GremlinTestUtils.getProperty<String>(vertex, "name"))
        assertEquals(30, GremlinTestUtils.getProperty<Int>(vertex, "age"))
    }

    @Test
    fun `should clear graph between tests`() {
        // Given
        g.addV("test").next()
        assertEquals(1, GremlinTestUtils.countVertices(g))

        // When
        GremlinTestUtils.clearGraph(g)

        // Then
        assertEquals(0, GremlinTestUtils.countVertices(g))
    }
}
