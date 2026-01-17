package io.github.dayanfcosta.gremlin.test

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource
import org.testcontainers.containers.GenericContainer

/**
 * [ContainerConnectionDetailsFactory] for [GremlinServerContainer].
 *
 * Enables `@ServiceConnection` support for Gremlin containers, allowing
 * automatic configuration of connection details from the container.
 *
 * Example usage:
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
 */
class GremlinContainerConnectionDetailsFactory :
    ContainerConnectionDetailsFactory<GremlinServerContainer, GremlinConnectionDetails>() {

    override fun getContainerConnectionDetails(
        source: ContainerConnectionSource<GremlinServerContainer>
    ): GremlinConnectionDetails {
        return GremlinServerContainerConnectionDetails(source)
    }

    /**
     * Implementation of [GremlinConnectionDetails] that extracts
     * connection information from a [GremlinServerContainer].
     */
    private class GremlinServerContainerConnectionDetails(
        source: ContainerConnectionSource<GremlinServerContainer>
    ) : ContainerConnectionDetails<GremlinServerContainer>(source), GremlinConnectionDetails {

        override fun getHost(): String = container.host

        override fun getPort(): Int = container.getGremlinPort()

        override fun getUsername(): String? = null

        override fun getPassword(): String? = null

        override fun isSslEnabled(): Boolean = false
    }
}

/**
 * Factory for [GenericContainer] with Gremlin server image.
 *
 * Matches containers using the "tinkerpop/gremlin-server" image name,
 * allowing `@ServiceConnection` to work with generic containers.
 *
 * Example usage:
 * ```kotlin
 * @Container
 * @ServiceConnection
 * val gremlin = GenericContainer(DockerImageName.parse("tinkerpop/gremlin-server:3.8.0"))
 *     .withExposedPorts(8182)
 * ```
 */
class GenericGremlinContainerConnectionDetailsFactory :
    ContainerConnectionDetailsFactory<GenericContainer<*>, GremlinConnectionDetails>(
        "tinkerpop/gremlin-server"
    ) {

    override fun getContainerConnectionDetails(
        source: ContainerConnectionSource<GenericContainer<*>>
    ): GremlinConnectionDetails {
        return GenericGremlinContainerConnectionDetails(source)
    }

    /**
     * Implementation of [GremlinConnectionDetails] for generic containers
     * with the Gremlin server image.
     */
    private class GenericGremlinContainerConnectionDetails(
        source: ContainerConnectionSource<GenericContainer<*>>
    ) : ContainerConnectionDetails<GenericContainer<*>>(source), GremlinConnectionDetails {

        override fun getHost(): String = container.host

        override fun getPort(): Int = container.getMappedPort(GremlinServerContainer.GREMLIN_PORT)
    }
}
