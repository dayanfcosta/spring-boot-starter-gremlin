# Request Interceptors

Request interceptors allow you to modify HTTP requests before they are sent to the Gremlin Server. This is useful for custom authentication, adding headers, logging, or request transformation.

## Overview

The `RequestInterceptor` interface from the Gremlin driver allows you to intercept and modify requests before they are sent.

## Enabling Request Interceptors

Define a `RequestInterceptor` bean in your Spring configuration:

```kotlin
import org.apache.tinkerpop.gremlin.driver.RequestInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GremlinConfig {

    @Bean
    fun requestInterceptor(): RequestInterceptor {
        return RequestInterceptor { request ->
            // Modify request here
            request
        }
    }
}
```

The starter will automatically pick up the bean and apply it to the Gremlin client.

## Use Cases

### Adding Custom Headers

```kotlin
import org.apache.tinkerpop.gremlin.driver.RequestInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GremlinConfig {

    @Bean
    fun requestInterceptor(): RequestInterceptor {
        return RequestInterceptor { request ->
            request.addHeader("X-Correlation-ID", generateCorrelationId())
            request.addHeader("X-Client-Version", "1.0.0")
            request
        }
    }

    private fun generateCorrelationId(): String {
        return java.util.UUID.randomUUID().toString()
    }
}
```

### Request Logging

```kotlin
import org.apache.tinkerpop.gremlin.driver.RequestInterceptor
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GremlinConfig {

    private val logger = LoggerFactory.getLogger(GremlinConfig::class.java)

    @Bean
    fun requestInterceptor(): RequestInterceptor {
        return RequestInterceptor { request ->
            logger.debug(
                "Gremlin request: requestId={}, op={}, processor={}",
                request.requestId,
                request.op,
                request.processor
            )
            request
        }
    }
}
```

### AWS IAM Authentication (Amazon Neptune)

For Amazon Neptune with IAM authentication, you need to sign requests with AWS Signature V4:

```kotlin
import org.apache.tinkerpop.gremlin.driver.RequestInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.signer.Aws4Signer
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams
import software.amazon.awssdk.regions.Region

@Configuration
class NeptuneConfig {

    @Bean
    fun requestInterceptor(): RequestInterceptor {
        val credentialsProvider = DefaultCredentialsProvider.create()
        val region = Region.of(System.getenv("AWS_REGION") ?: "us-east-1")

        return RequestInterceptor { request ->
            // Sign the request with AWS Signature V4
            val credentials = credentialsProvider.resolveCredentials()

            // Add IAM authentication headers
            // Note: Full implementation requires AWS SDK integration
            // This is a simplified example

            request
        }
    }
}
```

For a complete Neptune IAM authentication implementation, consider using the `amazon-neptune-sigv4-signer` library:

```kotlin
// build.gradle.kts
dependencies {
    implementation("software.amazon.neptune:amazon-neptune-sigv4-signer:2.4.0")
}
```

```kotlin
import com.amazon.neptune.gremlin.driver.sigv4.AwsSigV4ClientHandlerInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NeptuneConfig {

    @Bean
    fun requestInterceptor(): AwsSigV4ClientHandlerInterceptor {
        return AwsSigV4ClientHandlerInterceptor.builder()
            .regionName(System.getenv("AWS_REGION") ?: "us-east-1")
            .build()
    }
}
```

### Token-Based Authentication

For servers using token-based authentication:

```kotlin
import org.apache.tinkerpop.gremlin.driver.RequestInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GremlinConfig(
    private val tokenProvider: TokenProvider
) {

    @Bean
    fun requestInterceptor(): RequestInterceptor {
        return RequestInterceptor { request ->
            val token = tokenProvider.getToken()
            request.addHeader("Authorization", "Bearer $token")
            request
        }
    }
}

interface TokenProvider {
    fun getToken(): String
}
```

### Request Modification Based on Context

Using MDC (Mapped Diagnostic Context) for request tracing:

```kotlin
import org.apache.tinkerpop.gremlin.driver.RequestInterceptor
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GremlinConfig {

    @Bean
    fun requestInterceptor(): RequestInterceptor {
        return RequestInterceptor { request ->
            // Add trace context from MDC
            MDC.get("traceId")?.let { traceId ->
                request.addHeader("X-Trace-ID", traceId)
            }

            MDC.get("spanId")?.let { spanId ->
                request.addHeader("X-Span-ID", spanId)
            }

            request
        }
    }
}
```

## Multiple Interceptors

If you need multiple interceptors, create a composite interceptor:

```kotlin
import org.apache.tinkerpop.gremlin.driver.RequestInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GremlinConfig {

    @Bean
    fun requestInterceptor(
        loggingInterceptor: LoggingInterceptor,
        authInterceptor: AuthInterceptor,
        tracingInterceptor: TracingInterceptor
    ): RequestInterceptor {
        return RequestInterceptor { request ->
            var modifiedRequest = request
            modifiedRequest = loggingInterceptor.intercept(modifiedRequest)
            modifiedRequest = authInterceptor.intercept(modifiedRequest)
            modifiedRequest = tracingInterceptor.intercept(modifiedRequest)
            modifiedRequest
        }
    }
}
```

## Conditional Interceptors

Apply interceptors based on configuration:

```kotlin
import org.apache.tinkerpop.gremlin.driver.RequestInterceptor
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GremlinConfig {

    @Bean
    @ConditionalOnProperty(name = ["gremlin.interceptor.logging.enabled"], havingValue = "true")
    fun loggingInterceptor(): RequestInterceptor {
        return RequestInterceptor { request ->
            // Log request
            request
        }
    }
}
```

## Testing Interceptors

```kotlin
import org.apache.tinkerpop.gremlin.driver.RequestInterceptor
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RequestInterceptorTest {

    @Test
    fun `should add correlation id header`() {
        val interceptor = RequestInterceptor { request ->
            request.addHeader("X-Correlation-ID", "test-id")
            request
        }

        // Test the interceptor logic
        // Note: Actual testing requires mocking the RequestMessage
    }
}
```

## Considerations

1. **Thread Safety**: Interceptors may be called from multiple threads. Ensure your implementation is thread-safe.

2. **Performance**: Interceptors are called for every request. Keep them lightweight.

3. **Error Handling**: Handle exceptions gracefully to avoid breaking the request pipeline.

4. **Credentials**: Never log sensitive information like passwords or tokens.

5. **Order**: When using multiple interceptors, consider the order of execution.
