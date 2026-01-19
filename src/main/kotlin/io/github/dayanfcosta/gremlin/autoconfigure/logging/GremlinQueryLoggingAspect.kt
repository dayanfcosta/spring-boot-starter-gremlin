package io.github.dayanfcosta.gremlin.autoconfigure.logging

import org.apache.tinkerpop.gremlin.process.traversal.Bytecode
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory

/**
 * AOP Aspect for logging Gremlin query execution.
 *
 * Intercepts query submissions to the Gremlin server and logs:
 * - The query (formatted from bytecode)
 * - Execution time in milliseconds
 * - Slow query warnings when execution exceeds the configured threshold
 *
 * This aspect intercepts all `submitAsync` methods on the Gremlin [org.apache.tinkerpop.gremlin.driver.Client]
 * that accept [Bytecode], which covers all queries executed via [org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource].
 *
 * @property properties Configuration properties for logging behavior
 */
@Aspect
class GremlinQueryLoggingAspect(
    private val properties: GremlinQueryLoggingProperties
) {

    private val logger = LoggerFactory.getLogger("io.github.dayanfcosta.gremlin.query")

    /**
     * Intercepts `submitAsync(Bytecode)` calls on Gremlin Client.
     *
     * Logs the query before execution and timing after completion.
     */
    @Around("execution(* org.apache.tinkerpop.gremlin.driver.Client.submitAsync(org.apache.tinkerpop.gremlin.process.traversal.Bytecode))")
    fun logBytecodeQuery(joinPoint: ProceedingJoinPoint): Any? {
        val bytecode = joinPoint.args[0] as Bytecode
        return executeAndLog(joinPoint, formatBytecode(bytecode), bytecode)
    }

    /**
     * Intercepts `submitAsync(Bytecode, RequestOptions)` calls on Gremlin Client.
     */
    @Around("execution(* org.apache.tinkerpop.gremlin.driver.Client.submitAsync(org.apache.tinkerpop.gremlin.process.traversal.Bytecode, ..))")
    fun logBytecodeQueryWithOptions(joinPoint: ProceedingJoinPoint): Any? {
        val bytecode = joinPoint.args[0] as Bytecode
        return executeAndLog(joinPoint, formatBytecode(bytecode), bytecode)
    }

    /**
     * Intercepts string-based `submit` calls on Gremlin Client.
     */
    @Around("execution(* org.apache.tinkerpop.gremlin.driver.Client.submit(String, ..))")
    fun logStringQuery(joinPoint: ProceedingJoinPoint): Any? {
        val query = joinPoint.args[0] as String
        return executeAndLog(joinPoint, query, null)
    }

    /**
     * Intercepts string-based `submitAsync` calls on Gremlin Client.
     */
    @Around("execution(* org.apache.tinkerpop.gremlin.driver.Client.submitAsync(String, ..))")
    fun logStringQueryAsync(joinPoint: ProceedingJoinPoint): Any? {
        val query = joinPoint.args[0] as String
        return executeAndLog(joinPoint, query, null)
    }

    private fun executeAndLog(
        joinPoint: ProceedingJoinPoint,
        formattedQuery: String,
        bytecode: Bytecode?
    ): Any? {
        val startTime = System.currentTimeMillis()

        return try {
            joinPoint.proceed()
        } finally {
            val duration = System.currentTimeMillis() - startTime
            logQuery(formattedQuery, duration, bytecode)
        }
    }

    private fun logQuery(query: String, durationMs: Long, bytecode: Bytecode?) {
        val slowThresholdMs = properties.slowQueryThreshold.toMillis()
        val isSlow = durationMs >= slowThresholdMs

        val message = buildString {
            if (isSlow) {
                append("Slow Gremlin Query: ")
            } else {
                append("Gremlin Query: ")
            }
            append(query)
            append(" | Time: ${durationMs}ms")

            if (isSlow) {
                append(" (threshold: ${slowThresholdMs}ms)")
            }

            if (properties.includeBytecode && bytecode != null) {
                append(" | Bytecode: ${bytecode.sourceInstructions}, ${bytecode.stepInstructions}")
            }
        }

        if (isSlow) {
            logger.warn(message)
        } else {
            logger.debug(message)
        }
    }

    private fun formatBytecode(bytecode: Bytecode): String {
        // Format bytecode steps into a readable query representation
        val steps = bytecode.stepInstructions.joinToString(", ") { step ->
            "${step.operator}(${step.arguments.joinToString(", ")})"
        }
        return "g.$steps"
    }
}
