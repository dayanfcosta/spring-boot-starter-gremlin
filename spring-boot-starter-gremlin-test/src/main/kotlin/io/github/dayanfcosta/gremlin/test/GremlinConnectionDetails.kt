package io.github.dayanfcosta.gremlin.test

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails

/**
 * Connection details for a Gremlin server.
 *
 * Provides the necessary information to establish a connection to a
 * Gremlin-compatible graph database. Used by Spring Boot's
 * `@ServiceConnection` mechanism for automatic configuration.
 *
 * @see GremlinContainerConnectionDetailsFactory
 */
interface GremlinConnectionDetails : ConnectionDetails {

    /**
     * Returns the host address of the Gremlin server.
     *
     * @return The host address (e.g., "localhost")
     */
    fun getHost(): String

    /**
     * Returns the port of the Gremlin server.
     *
     * @return The port number (e.g., 8182)
     */
    fun getPort(): Int

    /**
     * Returns the username for authentication, or null if not required.
     *
     * @return The username, or null for anonymous access
     */
    fun getUsername(): String? = null

    /**
     * Returns the password for authentication, or null if not required.
     *
     * @return The password, or null for anonymous access
     */
    fun getPassword(): String? = null

    /**
     * Returns whether SSL is enabled for the connection.
     *
     * @return true if SSL is enabled, false otherwise
     */
    fun isSslEnabled(): Boolean = false
}
