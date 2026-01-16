# Authentication and SSL/TLS

This guide covers how to configure authentication and secure connections to Gremlin Server.

## Basic Authentication

### Configuration

```yaml
gremlin:
  host: gremlin-server.example.com
  port: 8182
  username: myuser
  password: ${GREMLIN_PASSWORD}
```

### Using Environment Variables

For security, always use environment variables or secrets management for credentials:

```yaml
gremlin:
  host: ${GREMLIN_HOST}
  port: ${GREMLIN_PORT:8182}
  username: ${GREMLIN_USERNAME}
  password: ${GREMLIN_PASSWORD}
```

### Spring Cloud Config / Vault Integration

```yaml
gremlin:
  host: gremlin-server.example.com
  username: ${vault.gremlin.username}
  password: ${vault.gremlin.password}
```

## SSL/TLS Configuration

### Enable SSL

```yaml
gremlin:
  host: gremlin-server.example.com
  port: 8182
  enable-ssl: true
```

### SSL with Authentication

```yaml
gremlin:
  host: gremlin-server.example.com
  port: 8182
  enable-ssl: true
  username: myuser
  password: ${GREMLIN_PASSWORD}
```

## Cloud Provider Configurations

### Amazon Neptune

Neptune requires SSL and uses IAM authentication or no authentication depending on configuration.

```yaml
gremlin:
  host: my-cluster.cluster-xxxxx.us-east-1.neptune.amazonaws.com
  port: 8182
  enable-ssl: true
  serializer: GRAPHBINARY_V1
```

For IAM authentication with Neptune, you'll need to implement a custom `RequestInterceptor` to sign requests. See [Request Interceptors](../advanced/request-interceptors.md).

### Azure Cosmos DB (Gremlin API)

```yaml
gremlin:
  host: my-cosmos-account.gremlin.cosmos.azure.com
  port: 443
  enable-ssl: true
  username: /dbs/my-database/colls/my-graph
  password: ${COSMOS_PRIMARY_KEY}
  serializer: GRAPHSON_V2
```

### JanusGraph with Authentication

```yaml
gremlin:
  host: janusgraph-server.example.com
  port: 8182
  enable-ssl: true
  username: janusgraph
  password: ${JANUSGRAPH_PASSWORD}
```

## Full Production Configuration

```yaml
gremlin:
  host: ${GREMLIN_HOST}
  port: ${GREMLIN_PORT:8182}

  # Authentication
  username: ${GREMLIN_USERNAME}
  password: ${GREMLIN_PASSWORD}

  # SSL/TLS
  enable-ssl: true

  # Performance tuning
  serializer: GRAPHBINARY_V1
  connection-timeout: 10s
  request-timeout: 60s
  keep-alive-interval: 30s

  # Connection pool
  connection-pool:
    min-connections-per-host: 4
    max-connections-per-host: 16
    max-wait-for-connection: 5s
```

## Cluster Mode with SSL

```yaml
gremlin:
  cluster:
    hosts:
      - gremlin-1.example.com
      - gremlin-2.example.com
      - gremlin-3.example.com
  port: 8182
  enable-ssl: true
  username: ${GREMLIN_USERNAME}
  password: ${GREMLIN_PASSWORD}
```

## Writer/Reader Mode with SSL

```yaml
gremlin:
  writer: gremlin-writer.example.com
  readers:
    - gremlin-reader-1.example.com
    - gremlin-reader-2.example.com
  port: 8182
  enable-ssl: true
  username: ${GREMLIN_USERNAME}
  password: ${GREMLIN_PASSWORD}
```

## Troubleshooting

### Connection Refused with SSL

Ensure the Gremlin Server is configured with SSL enabled. Check the server's `gremlin-server.yaml`:

```yaml
# Server-side configuration
ssl:
  enabled: true
  keyStore: /path/to/keystore.jks
  keyStorePassword: changeit
```

### Certificate Validation Errors

If you're using self-signed certificates in development, you may need to:

1. Add the certificate to Java's truststore
2. Or configure the JVM to trust the certificate:

```bash
java -Djavax.net.ssl.trustStore=/path/to/truststore.jks \
     -Djavax.net.ssl.trustStorePassword=changeit \
     -jar your-app.jar
```

### Authentication Failed

1. Verify credentials are correct
2. Check that the Gremlin Server has authentication enabled
3. Ensure the user has proper permissions on the graph

### Timeout Errors

Increase timeout values for slow networks or large responses:

```yaml
gremlin:
  connection-timeout: 30s
  request-timeout: 120s
```
