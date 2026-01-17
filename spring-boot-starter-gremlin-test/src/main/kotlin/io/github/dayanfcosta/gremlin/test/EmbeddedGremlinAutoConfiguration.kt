package io.github.dayanfcosta.gremlin.test

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean

/**
 * Auto-configuration for embedded TinkerGraph.
 *
 * Provides an in-memory graph implementation for fast unit testing
 * without requiring Docker or external services.
 *
 * This configuration is enabled by default when using `@GremlinTest`.
 * To use a real Gremlin server via Testcontainers instead, set
 * `gremlin.test.embedded=false`.
 *
 * Note: TinkerGraph operates locally and does not provide Cluster or Client beans.
 * Only [GraphTraversalSource] is available for graph operations.
 *
 * Example usage:
 * ```kotlin
 * @GremlinTest
 * class MyRepositoryTest {
 *     @Autowired
 *     lateinit var g: GraphTraversalSource
 *
 *     @Test
 *     fun `should create vertex`() {
 *         g.addV("person").property("name", "John").next()
 *         assertEquals(1, g.V().count().next())
 *     }
 * }
 * ```
 */
@AutoConfiguration
@ConditionalOnClass(TinkerGraph::class, GraphTraversalSource::class)
@ConditionalOnProperty(
    prefix = "gremlin.test",
    name = ["embedded"],
    havingValue = "true",
    matchIfMissing = true
)
class EmbeddedGremlinAutoConfiguration {

    /**
     * Creates an in-memory TinkerGraph instance.
     *
     * @return A new TinkerGraph instance
     */
    @Bean
    @ConditionalOnMissingBean
    fun tinkerGraph(): TinkerGraph {
        return TinkerGraph.open()
    }

    /**
     * Creates a [GraphTraversalSource] from the TinkerGraph.
     *
     * This is the primary bean that tests will interact with for
     * graph traversal operations.
     *
     * @param graph The TinkerGraph instance
     * @return A GraphTraversalSource for the graph
     */
    @Bean
    @ConditionalOnMissingBean
    fun graphTraversalSource(graph: TinkerGraph): GraphTraversalSource {
        return graph.traversal()
    }
}
