package io.github.dayanfcosta.gremlin.test

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Annotation for a Gremlin test that focuses only on Gremlin components.
 *
 * Using this annotation will configure an embedded in-memory TinkerGraph
 * for fast unit testing without requiring Docker or external services.
 *
 * The test will have access to a [org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource]
 * bean that can be injected for graph operations.
 *
 * Example usage:
 * ```kotlin
 * @GremlinTest
 * class PersonRepositoryTest {
 *     @Autowired
 *     lateinit var g: GraphTraversalSource
 *
 *     @AfterEach
 *     fun cleanup() {
 *         GremlinTestUtils.clearGraph(g)
 *     }
 *
 *     @Test
 *     fun `should create and find person vertex`() {
 *         // Given
 *         g.addV("person")
 *             .property("name", "John")
 *             .property("age", 30)
 *             .next()
 *
 *         // When
 *         val count = GremlinTestUtils.countVertices(g, "person")
 *
 *         // Then
 *         assertEquals(1, count)
 *     }
 * }
 * ```
 *
 * For integration testing with a real Gremlin server, use [SpringBootTest]
 * with [GremlinServerContainer] and `@ServiceConnection` instead:
 *
 * ```kotlin
 * @SpringBootTest
 * @Testcontainers
 * class PersonServiceIntegrationTest {
 *     companion object {
 *         @Container
 *         @ServiceConnection
 *         val gremlin = GremlinServerContainer()
 *     }
 *
 *     @Autowired
 *     lateinit var g: GraphTraversalSource
 * }
 * ```
 *
 * @see GremlinTestUtils
 * @see GremlinServerContainer
 * @see EmbeddedGremlinAutoConfiguration
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ExtendWith(SpringExtension::class)
@OverrideAutoConfiguration(enabled = false)
@ImportAutoConfiguration(EmbeddedGremlinAutoConfiguration::class)
annotation class GremlinTest(
    /**
     * Properties in form `key=value` that should be added to the Spring
     * Environment before the test runs.
     */
    val properties: Array<String> = []
)
