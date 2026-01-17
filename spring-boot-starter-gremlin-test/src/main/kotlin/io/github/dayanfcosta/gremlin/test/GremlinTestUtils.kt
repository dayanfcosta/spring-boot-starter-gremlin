package io.github.dayanfcosta.gremlin.test

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Vertex

/**
 * Utility methods for Gremlin testing.
 *
 * Provides common operations like clearing graphs, creating test data,
 * and querying for assertions.
 *
 * Example usage:
 * ```kotlin
 * @GremlinTest
 * class MyTest {
 *     @Autowired
 *     lateinit var g: GraphTraversalSource
 *
 *     @AfterEach
 *     fun cleanup() {
 *         GremlinTestUtils.clearGraph(g)
 *     }
 *
 *     @Test
 *     fun test() {
 *         val vertex = GremlinTestUtils.createVertex(g, "person", "name" to "John")
 *         assertTrue(GremlinTestUtils.vertexExists(g, "person", "name" to "John"))
 *     }
 * }
 * ```
 */
object GremlinTestUtils {

    /**
     * Clears all vertices and edges from the graph.
     *
     * Useful for test cleanup between tests to ensure isolation.
     *
     * @param g The graph traversal source
     */
    fun clearGraph(g: GraphTraversalSource) {
        g.V().drop().iterate()
    }

    /**
     * Counts all vertices in the graph.
     *
     * @param g The graph traversal source
     * @return The total number of vertices
     */
    fun countVertices(g: GraphTraversalSource): Long {
        return g.V().count().next()
    }

    /**
     * Counts vertices with a specific label.
     *
     * @param g The graph traversal source
     * @param label The vertex label to count
     * @return The number of vertices with the given label
     */
    fun countVertices(g: GraphTraversalSource, label: String): Long {
        return g.V().hasLabel(label).count().next()
    }

    /**
     * Counts all edges in the graph.
     *
     * @param g The graph traversal source
     * @return The total number of edges
     */
    fun countEdges(g: GraphTraversalSource): Long {
        return g.E().count().next()
    }

    /**
     * Counts edges with a specific label.
     *
     * @param g The graph traversal source
     * @param label The edge label to count
     * @return The number of edges with the given label
     */
    fun countEdges(g: GraphTraversalSource, label: String): Long {
        return g.E().hasLabel(label).count().next()
    }

    /**
     * Checks if a vertex with the given label and properties exists.
     *
     * @param g The graph traversal source
     * @param label The vertex label
     * @param properties Key-value pairs of properties to match
     * @return true if a matching vertex exists, false otherwise
     */
    fun vertexExists(
        g: GraphTraversalSource,
        label: String,
        vararg properties: Pair<String, Any>
    ): Boolean {
        var traversal = g.V().hasLabel(label)
        properties.forEach { (key, value) ->
            traversal = traversal.has(key, value)
        }
        return traversal.hasNext()
    }

    /**
     * Finds a vertex by label and properties.
     *
     * @param g The graph traversal source
     * @param label The vertex label
     * @param properties Key-value pairs of properties to match
     * @return The matching vertex, or null if not found
     */
    fun findVertex(
        g: GraphTraversalSource,
        label: String,
        vararg properties: Pair<String, Any>
    ): Vertex? {
        var traversal = g.V().hasLabel(label)
        properties.forEach { (key, value) ->
            traversal = traversal.has(key, value)
        }
        return traversal.tryNext().orElse(null)
    }

    /**
     * Creates a test vertex with the given label and properties.
     *
     * @param g The graph traversal source
     * @param label The vertex label
     * @param properties Key-value pairs of properties to set
     * @return The created vertex
     */
    fun createVertex(
        g: GraphTraversalSource,
        label: String,
        vararg properties: Pair<String, Any>
    ): Vertex {
        var traversal = g.addV(label)
        properties.forEach { (key, value) ->
            traversal = traversal.property(key, value)
        }
        return traversal.next()
    }

    /**
     * Creates an edge between two vertices.
     *
     * @param g The graph traversal source
     * @param from The source vertex
     * @param label The edge label
     * @param to The target vertex
     * @param properties Key-value pairs of properties to set on the edge
     * @return The created edge
     */
    fun createEdge(
        g: GraphTraversalSource,
        from: Vertex,
        label: String,
        to: Vertex,
        vararg properties: Pair<String, Any>
    ): Edge {
        var traversal = g.V(from.id()).addE(label).to(to)
        properties.forEach { (key, value) ->
            traversal = traversal.property(key, value)
        }
        return traversal.next()
    }

    /**
     * Gets a property value from a vertex.
     *
     * @param vertex The vertex
     * @param key The property key
     * @return The property value, or null if not present
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getProperty(vertex: Vertex, key: String): T? {
        val property = vertex.property<T>(key)
        return if (property.isPresent) property.value() else null
    }
}
