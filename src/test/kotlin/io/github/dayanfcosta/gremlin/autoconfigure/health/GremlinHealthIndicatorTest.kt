package io.github.dayanfcosta.gremlin.autoconfigure.health

import io.mockk.every
import io.mockk.mockk
import org.apache.tinkerpop.gremlin.driver.Client
import org.apache.tinkerpop.gremlin.driver.Result
import org.apache.tinkerpop.gremlin.driver.ResultSet
import org.junit.jupiter.api.Test
import org.springframework.boot.actuate.health.Status
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GremlinHealthIndicatorTest {

    private val defaultHosts = listOf("localhost")
    private val defaultPort = 8182
    private val defaultTimeout = Duration.ofSeconds(5)

    @Test
    fun `should report UP when query succeeds`() {
        val client = mockk<Client>()
        val resultSet = mockk<ResultSet>()

        every { client.submit(any<String>()) } returns resultSet
        every { resultSet.all() } returns CompletableFuture.completedFuture(listOf(Result(1)))

        val indicator = GremlinHealthIndicator(
            client = client,
            connectionName = "default",
            hosts = defaultHosts,
            port = defaultPort,
            timeout = defaultTimeout
        )

        val health = indicator.health()

        assertEquals(Status.UP, health.status)
        assertEquals("default", health.details["connection"])
        assertEquals(defaultHosts, health.details["hosts"])
        assertEquals(defaultPort, health.details["port"])
        assertNotNull(health.details["latency"])
        assertTrue(health.details["latency"].toString().endsWith("ms"))
    }

    @Test
    fun `should report DOWN when query times out`() {
        val client = mockk<Client>()
        val resultSet = mockk<ResultSet>()
        val future = CompletableFuture<List<Result>>()

        every { client.submit(any<String>()) } returns resultSet
        every { resultSet.all() } returns future

        // Simulate timeout by completing exceptionally
        future.completeExceptionally(TimeoutException("Timed out"))

        val indicator = GremlinHealthIndicator(
            client = client,
            connectionName = "default",
            hosts = defaultHosts,
            port = defaultPort,
            timeout = Duration.ofMillis(100)
        )

        val health = indicator.health()

        assertEquals(Status.DOWN, health.status)
        assertEquals("default", health.details["connection"])
        assertNotNull(health.details["error"])
    }

    @Test
    fun `should report DOWN when connection fails`() {
        val client = mockk<Client>()

        every { client.submit(any<String>()) } throws RuntimeException("Connection refused")

        val indicator = GremlinHealthIndicator(
            client = client,
            connectionName = "default",
            hosts = defaultHosts,
            port = defaultPort,
            timeout = defaultTimeout
        )

        val health = indicator.health()

        assertEquals(Status.DOWN, health.status)
        assertTrue(health.details["error"].toString().contains("Connection refused"))
    }

    @Test
    fun `should include connection metadata in health details`() {
        val client = mockk<Client>()
        val resultSet = mockk<ResultSet>()
        val hosts = listOf("node1.example.com", "node2.example.com")

        every { client.submit(any<String>()) } returns resultSet
        every { resultSet.all() } returns CompletableFuture.completedFuture(listOf(Result(1)))

        val indicator = GremlinHealthIndicator(
            client = client,
            connectionName = "cluster",
            hosts = hosts,
            port = 9182,
            timeout = defaultTimeout
        )

        val health = indicator.health()

        assertEquals("cluster", health.details["connection"])
        assertEquals(hosts, health.details["hosts"])
        assertEquals(9182, health.details["port"])
    }

    @Test
    fun `should report latency in milliseconds`() {
        val client = mockk<Client>()
        val resultSet = mockk<ResultSet>()

        every { client.submit(any<String>()) } returns resultSet
        every { resultSet.all() } returns CompletableFuture.completedFuture(listOf(Result(1)))

        val indicator = GremlinHealthIndicator(
            client = client,
            connectionName = "default",
            hosts = defaultHosts,
            port = defaultPort,
            timeout = defaultTimeout
        )

        val health = indicator.health()

        val latency = health.details["latency"] as String
        assertTrue(latency.matches(Regex("\\d+ms")))
    }
}
