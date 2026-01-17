import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.9" apply false
    id("io.spring.dependency-management") version "1.1.7"
    `maven-publish`
    signing
}

group = "io.github.dayanfcosta"
version = "1.0.0-SNAPSHOT"
description = "Test utilities for Spring Boot Gremlin Starter"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
    withJavadocJar()
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Main starter dependency
    api(rootProject)

    // Apache TinkerPop Gremlin (needed for compilation)
    api("org.apache.tinkerpop:gremlin-driver:3.8.0")

    // Spring Boot Test
    api("org.springframework.boot:spring-boot-starter-test")
    api("org.springframework.boot:spring-boot-testcontainers")

    // TinkerPop - TinkerGraph for embedded testing
    api("org.apache.tinkerpop:tinkergraph-gremlin:3.8.0")

    // Testcontainers
    api("org.testcontainers:testcontainers")
    api("org.testcontainers:junit-jupiter")

    // Kotlin
    api("org.jetbrains.kotlin:kotlin-reflect")
    api("org.jetbrains.kotlin:kotlin-test-junit5")

    // Test dependencies for this module
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_17
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    enabled = true
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name = "Spring Boot Starter Gremlin Test"
                description = "Test utilities for Spring Boot Gremlin Starter - includes TinkerGraph embedded support and Testcontainers integration"
                url = "https://github.com/dayanfcosta/spring-boot-starter-gremlin"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }

                developers {
                    developer {
                        id = "dayanfcosta"
                        name = "Dayan Costa"
                        url = "https://github.com/dayanfcosta"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/dayanfcosta/spring-boot-starter-gremlin.git"
                    developerConnection = "scm:git:ssh://github.com/dayanfcosta/spring-boot-starter-gremlin.git"
                    url = "https://github.com/dayanfcosta/spring-boot-starter-gremlin"
                }
            }
        }
    }
}

signing {
    val signingKey = System.getenv("GPG_PRIVATE_KEY")
    val signingPassword = System.getenv("GPG_PASSPHRASE")

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }

    sign(publishing.publications["mavenJava"])
}

tasks.withType<Sign>().configureEach {
    onlyIf { !version.toString().endsWith("SNAPSHOT") }
}
