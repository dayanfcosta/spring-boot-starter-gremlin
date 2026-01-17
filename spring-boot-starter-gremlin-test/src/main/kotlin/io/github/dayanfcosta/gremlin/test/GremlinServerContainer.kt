package io.github.dayanfcosta.gremlin.test

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * Testcontainers container for Apache TinkerPop Gremlin Server.
 *
 * Provides a pre-configured container with proper wait strategy and
 * convenient accessor methods for connecting to the Gremlin server.
 *
 * Usage with @ServiceConnection (Spring Boot 3.1+):
 * ```kotlin
 * @SpringBootTest
 * @Testcontainers
 * class MyIntegrationTest {
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
 * Usage with custom image version:
 * ```kotlin
 * val gremlin = GremlinServerContainer("3.7.3")
 * ```
 *
 * @param image The Docker image to use for the container
 */
class GremlinServerContainer(
    image: DockerImageName = DEFAULT_IMAGE
) : GenericContainer<GremlinServerContainer>(image) {

    companion object {
        /** Default Gremlin server port */
        const val GREMLIN_PORT = 8182

        /** Default Docker image name */
        const val DEFAULT_IMAGE_NAME = "tinkerpop/gremlin-server"

        /** Default Docker image tag */
        const val DEFAULT_IMAGE_TAG = "3.8.0"

        /** Default Docker image */
        val DEFAULT_IMAGE: DockerImageName = DockerImageName.parse(
            "$DEFAULT_IMAGE_NAME:$DEFAULT_IMAGE_TAG"
        )

        private val WAIT_STRATEGY = Wait
            .forLogMessage(".*Gremlin Server configured with worker.*\\n", 1)
            .withStartupTimeout(Duration.ofMinutes(2))
    }

    /**
     * Creates a container with the specified image tag.
     *
     * @param tag The Docker image tag (e.g., "3.7.3", "3.8.0")
     */
    constructor(tag: String) : this(
        DockerImageName.parse("$DEFAULT_IMAGE_NAME:$tag")
    )

    init {
        withExposedPorts(GREMLIN_PORT)
        waitingFor(WAIT_STRATEGY)
        withReuse(true)
    }

    /**
     * Returns the mapped port for the Gremlin server.
     *
     * @return The host port mapped to the container's Gremlin port
     */
    fun getGremlinPort(): Int = getMappedPort(GREMLIN_PORT)

    /**
     * Returns the WebSocket URL for connecting to the Gremlin server.
     *
     * @return The WebSocket URL (e.g., "ws://localhost:32768/gremlin")
     */
    fun getGremlinUrl(): String = "ws://$host:${getGremlinPort()}/gremlin"

    /**
     * Returns the host address for the Gremlin server.
     *
     * @return The host address
     */
    fun getGremlinHost(): String = host
}
